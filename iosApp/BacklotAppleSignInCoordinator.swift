import AuthenticationServices
import CryptoKit
import Foundation
import Security
import UIKit

/// Native Apple credential returned before any Supabase exchange.
///
/// Identity tokens, authorization codes and the raw nonce stay inside the iOS authentication layer.
/// Shared Backlot product models must receive only the stable identity fields after server exchange.
struct BacklotAppleAuthorization: Sendable {
    let providerSubject: String
    let email: String
    let displayName: String
    let identityToken: String
    let authorizationCode: String
    let rawNonce: String
}

enum BacklotAppleSignInError: Error, LocalizedError {
    case requestAlreadyRunning
    case randomNonceFailure(OSStatus)
    case invalidCredential
    case missingIdentityToken
    case missingAuthorizationCode
    case invalidTokenEncoding
    case noPresentationAnchor

    var errorDescription: String? {
        switch self {
        case .requestAlreadyRunning:
            return "An Apple sign-in request is already running."
        case .randomNonceFailure(let status):
            return "Could not create a secure Sign in with Apple nonce (OSStatus \(status))."
        case .invalidCredential:
            return "Apple returned an unsupported authorization credential."
        case .missingIdentityToken:
            return "Apple did not return an identity token."
        case .missingAuthorizationCode:
            return "Apple did not return an authorization code."
        case .invalidTokenEncoding:
            return "Apple returned authentication data that Backlot could not decode."
        case .noPresentationAnchor:
            return "Backlot could not find a window to present Sign in with Apple."
        }
    }
}

/// Native Sign in with Apple coordinator.
///
/// The nonce is generated with `SecRandomCopyBytes`, SHA-256 hashed for Apple's request, and the
/// un-hashed value is returned only to the native caller that will exchange the ID token with
/// Supabase. Nothing in this coordinator is persisted or sent into BacklotShared.
@MainActor
final class BacklotAppleSignInCoordinator: NSObject {
    private var continuation: CheckedContinuation<BacklotAppleAuthorization, Error>?
    private var rawNonce: String?

    func signIn() async throws -> BacklotAppleAuthorization {
        guard continuation == nil else {
            throw BacklotAppleSignInError.requestAlreadyRunning
        }

        let rawNonce = try Self.makeNonce()
        self.rawNonce = rawNonce

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(rawNonce)

        return try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation

            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            controller.performRequests()
        }
    }

    private func finish(_ result: Result<BacklotAppleAuthorization, Error>) {
        guard let continuation else {
            return
        }
        self.continuation = nil
        rawNonce = nil

        switch result {
        case .success(let authorization):
            continuation.resume(returning: authorization)
        case .failure(let error):
            continuation.resume(throwing: error)
        }
    }

    private static func makeNonce(length: Int = 32) throws -> String {
        precondition(length > 0)
        let alphabet = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        result.reserveCapacity(length)

        while result.count < length {
            var random: UInt8 = 0
            let status = SecRandomCopyBytes(kSecRandomDefault, 1, &random)
            guard status == errSecSuccess else {
                throw BacklotAppleSignInError.randomNonceFailure(status)
            }
            if random < alphabet.count {
                result.append(alphabet[Int(random)])
            }
        }
        return result
    }

    private static func sha256(_ input: String) -> String {
        let digest = SHA256.hash(data: Data(input.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    private static func displayName(from components: PersonNameComponents?) -> String {
        guard let components else {
            return ""
        }
        return PersonNameComponentsFormatter().string(from: components)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

extension BacklotAppleSignInCoordinator: ASAuthorizationControllerDelegate {
    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        Task { @MainActor in
            guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
                finish(.failure(BacklotAppleSignInError.invalidCredential))
                return
            }
            guard let rawNonce else {
                finish(.failure(BacklotAppleSignInError.invalidCredential))
                return
            }
            guard let identityTokenData = credential.identityToken else {
                finish(.failure(BacklotAppleSignInError.missingIdentityToken))
                return
            }
            guard let authorizationCodeData = credential.authorizationCode else {
                finish(.failure(BacklotAppleSignInError.missingAuthorizationCode))
                return
            }
            guard
                let identityToken = String(data: identityTokenData, encoding: .utf8),
                let authorizationCode = String(data: authorizationCodeData, encoding: .utf8)
            else {
                finish(.failure(BacklotAppleSignInError.invalidTokenEncoding))
                return
            }

            finish(.success(BacklotAppleAuthorization(
                providerSubject: credential.user,
                email: credential.email ?? "",
                displayName: Self.displayName(from: credential.fullName),
                identityToken: identityToken,
                authorizationCode: authorizationCode,
                rawNonce: rawNonce
            )))
        }
    }

    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        Task { @MainActor in
            finish(.failure(error))
        }
    }
}

extension BacklotAppleSignInCoordinator: ASAuthorizationControllerPresentationContextProviding {
    nonisolated func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        MainActor.assumeIsolated {
            let windows = UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap(\.windows)

            if let window = windows.first(where: { $0.isKeyWindow }) ?? windows.first {
                return window
            }
            return ASPresentationAnchor()
        }
    }
}
