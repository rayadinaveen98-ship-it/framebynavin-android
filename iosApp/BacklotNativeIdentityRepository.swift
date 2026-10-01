import Foundation
import Security

/// Provider identity owned by the native iOS authentication layer.
///
/// This type deliberately lives outside BacklotShared: credentials, Supabase session tokens and
/// provider-specific authentication material must never cross into shared product models.
enum BacklotNativeIdentityProvider: String, Codable, Sendable {
    case google
    case apple
}

/// Secure native session envelope persisted only in the iOS Keychain.
///
/// `providerSubject` identifies the provider account. `cloudAccountId` is the provider-neutral
/// Supabase user id used for cloud ownership. It remains optional so Keychain payloads created by
/// earlier V151 builds can still be decoded during the migration.
struct BacklotNativeAuthSession: Codable, Equatable, Sendable {
    let provider: BacklotNativeIdentityProvider
    let providerSubject: String
    let cloudAccountId: String?
    let email: String
    let displayName: String
    let avatarURL: String
    let supabaseAccessToken: String
    let supabaseRefreshToken: String
    let expiresAtEpochSeconds: Int64

    var isExpired: Bool {
        Int64(Date().timeIntervalSince1970) >= expiresAtEpochSeconds
    }
}

enum BacklotSecureSessionError: Error, LocalizedError {
    case invalidSession
    case encodingFailed(Error)
    case decodingFailed(Error)
    case keychain(OSStatus)

    var errorDescription: String? {
        switch self {
        case .invalidSession:
            return "Backlot received an incomplete native authentication session."
        case .encodingFailed(let error):
            return "Backlot could not encode the secure session: \(error.localizedDescription)"
        case .decodingFailed(let error):
            return "Backlot could not decode the secure session: \(error.localizedDescription)"
        case .keychain(let status):
            let detail = SecCopyErrorMessageString(status, nil) as String? ?? "OSStatus \(status)"
            return "Backlot Keychain operation failed: \(detail)"
        }
    }
}

protocol BacklotSecureSessionStoring {
    func load() throws -> BacklotNativeAuthSession?
    func save(_ session: BacklotNativeAuthSession) throws
    func clear() throws
}

/// Keychain-backed credential vault for the native iOS identity adapter.
///
/// `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` keeps credentials on this device while still
/// allowing future background refresh after the device has been unlocked once since boot.
final class BacklotKeychainSessionStore: BacklotSecureSessionStoring {
    private let service: String
    private let account: String
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(
        service: String = "\(Bundle.main.bundleIdentifier ?? "com.backlot.app.dev").identity",
        account: String = "creator-auth-session"
    ) {
        self.service = service
        self.account = account
    }

    func load() throws -> BacklotNativeAuthSession? {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound {
            return nil
        }
        guard status == errSecSuccess else {
            throw BacklotSecureSessionError.keychain(status)
        }
        guard let data = result as? Data else {
            throw BacklotSecureSessionError.keychain(errSecDecode)
        }

        do {
            return try decoder.decode(BacklotNativeAuthSession.self, from: data)
        } catch {
            throw BacklotSecureSessionError.decodingFailed(error)
        }
    }

    func save(_ session: BacklotNativeAuthSession) throws {
        try validate(session)

        let data: Data
        do {
            data = try encoder.encode(session)
        } catch {
            throw BacklotSecureSessionError.encodingFailed(error)
        }

        let attributes: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]

        var status = SecItemUpdate(baseQuery as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            var addQuery = baseQuery
            attributes.forEach { addQuery[$0.key] = $0.value }
            status = SecItemAdd(addQuery as CFDictionary, nil)
        }

        guard status == errSecSuccess else {
            throw BacklotSecureSessionError.keychain(status)
        }
    }

    func clear() throws {
        let status = SecItemDelete(baseQuery as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw BacklotSecureSessionError.keychain(status)
        }
    }

    private var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }

    private func validate(_ session: BacklotNativeAuthSession) throws {
        let subject = session.providerSubject.trimmingCharacters(in: .whitespacesAndNewlines)
        let accessToken = session.supabaseAccessToken.trimmingCharacters(in: .whitespacesAndNewlines)
        let refreshToken = session.supabaseRefreshToken.trimmingCharacters(in: .whitespacesAndNewlines)
        let cloudAccountIsValid = session.cloudAccountId.map {
            !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        } ?? true
        guard !subject.isEmpty, cloudAccountIsValid, !accessToken.isEmpty, !refreshToken.isEmpty else {
            throw BacklotSecureSessionError.invalidSession
        }
    }
}

/// Native identity boundary used by future Apple/Google + Supabase adapters.
///
/// The shared Backlot session receives only provider-neutral identity fields after this repository
/// has loaded/refreshed a valid native auth session. Tokens never leave this boundary.
protocol BacklotNativeIdentityRepositoryProtocol {
    func persistedSession() throws -> BacklotNativeAuthSession?
    func persist(_ session: BacklotNativeAuthSession) throws
    func signOut() throws
}

final class BacklotNativeIdentityRepository: BacklotNativeIdentityRepositoryProtocol {
    private let secureStore: BacklotSecureSessionStoring

    init(secureStore: BacklotSecureSessionStoring = BacklotKeychainSessionStore()) {
        self.secureStore = secureStore
    }

    func persistedSession() throws -> BacklotNativeAuthSession? {
        guard let session = try secureStore.load() else {
            return nil
        }

        // An expired token is not promoted into shared product state. The Supabase adapter can later
        // replace this with a refresh flow; until then fail closed and remove the stale credential.
        guard !session.isExpired else {
            try secureStore.clear()
            return nil
        }
        return session
    }

    func persist(_ session: BacklotNativeAuthSession) throws {
        try secureStore.save(session)
    }

    func signOut() throws {
        try secureStore.clear()
    }
}
