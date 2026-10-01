import Foundation

#if canImport(Supabase)
import Supabase
#endif

/// Native refresh boundary for a persisted Supabase-backed Backlot session.
protocol BacklotNativeSessionRefreshing {
    func refresh(_ session: BacklotNativeAuthSession) async throws -> BacklotNativeAuthSession
}

enum BacklotSupabaseSessionRefreshError: Error, LocalizedError {
    case invalidConfiguration
    case incompleteSession
    case cloudAccountMismatch

    var errorDescription: String? {
        switch self {
        case .invalidConfiguration:
            return "Backlot Supabase Auth is not configured with a valid project URL and publishable key."
        case .incompleteSession:
            return "Supabase returned an incomplete refreshed authentication session."
        case .cloudAccountMismatch:
            return "The refreshed Supabase account does not match the persisted Backlot cloud account."
        }
    }
}

#if canImport(Supabase)
/// Refreshes expired native sessions using the Keychain-owned Supabase refresh token.
///
/// Supabase refresh tokens rotate. The caller must persist the returned session immediately before
/// promoting it into shared product state.
final class BacklotSupabaseSessionRefresher: BacklotNativeSessionRefreshing {
    private let client: SupabaseClient

    init(supabaseURL: URL, publishableKey: String) throws {
        let key = publishableKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard
            let scheme = supabaseURL.scheme?.lowercased(),
            scheme == "https",
            supabaseURL.host != nil,
            !key.isEmpty
        else {
            throw BacklotSupabaseSessionRefreshError.invalidConfiguration
        }

        client = SupabaseClient(
            supabaseURL: supabaseURL,
            supabaseKey: key
        )
    }

    func refresh(_ storedSession: BacklotNativeAuthSession) async throws -> BacklotNativeAuthSession {
        let refreshToken = storedSession.supabaseRefreshToken
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !refreshToken.isEmpty else {
            throw BacklotSupabaseSessionRefreshError.incompleteSession
        }

        let refreshed = try await client.auth.refreshSession(refreshToken: refreshToken)
        let refreshedCloudAccountId = refreshed.user.id.uuidString.lowercased()

        if let existingCloudAccountId = storedSession.cloudAccountId {
            let normalizedExistingId = existingCloudAccountId
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .lowercased()
            if !normalizedExistingId.isEmpty, normalizedExistingId != refreshedCloudAccountId {
                throw BacklotSupabaseSessionRefreshError.cloudAccountMismatch
            }
        }

        let refreshedEmail = refreshed.user.email?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let email = (refreshedEmail?.isEmpty == false ? refreshedEmail : nil) ?? storedSession.email

        let metadataName = refreshed.user.userMetadata["full_name"]?.stringValue?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let displayName = (metadataName?.isEmpty == false ? metadataName : nil) ?? storedSession.displayName

        let metadataAvatar = refreshed.user.userMetadata["avatar_url"]?.stringValue
            ?? refreshed.user.userMetadata["picture"]?.stringValue
        let avatarURL = metadataAvatar ?? storedSession.avatarURL

        guard
            !refreshedCloudAccountId.isEmpty,
            !refreshed.accessToken.isEmpty,
            !refreshed.refreshToken.isEmpty,
            refreshed.expiresAt.isFinite,
            refreshed.expiresAt > 0
        else {
            throw BacklotSupabaseSessionRefreshError.incompleteSession
        }

        return BacklotNativeAuthSession(
            provider: storedSession.provider,
            providerSubject: storedSession.providerSubject,
            cloudAccountId: refreshedCloudAccountId,
            email: email,
            displayName: displayName,
            avatarURL: avatarURL,
            supabaseAccessToken: refreshed.accessToken,
            supabaseRefreshToken: refreshed.refreshToken,
            expiresAtEpochSeconds: Int64(refreshed.expiresAt)
        )
    }
}
#endif
