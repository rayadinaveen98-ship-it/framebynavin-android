import Foundation

#if canImport(GoogleSignIn)
import GoogleSignIn
import UIKit
#endif

struct BacklotGoogleAuthorization: Equatable, Sendable {
    let providerSubject: String
    let idToken: String
    let accessToken: String
    let email: String
    let displayName: String
    let avatarURL: String
}

enum BacklotGoogleSignInError: Error, LocalizedError {
    case invalidConfiguration
    case presentationUnavailable
    case incompleteCredential

    var errorDescription: String? {
        switch self {
        case .invalidConfiguration:
            return "Google Sign-In is missing a valid iOS or Web client ID."
        case .presentationUnavailable:
            return "Backlot could not find an active window to present Google Sign-In."
        case .incompleteCredential:
            return "Google Sign-In returned an incomplete identity credential."
        }
    }
}

@MainActor
protocol BacklotGoogleAuthorizing: AnyObject {
    func signIn() async throws -> BacklotGoogleAuthorization
    func handleOpenURL(_ url: URL) -> Bool
    func signOut()
}

#if canImport(GoogleSignIn)
@MainActor
final class BacklotGoogleSignInCoordinator: BacklotGoogleAuthorizing {
    private let clientID: String
    private let serverClientID: String

    init(clientID: String, serverClientID: String) throws {
        let clientID = clientID.trimmingCharacters(in: .whitespacesAndNewlines)
        let serverClientID = serverClientID.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clientID.isEmpty, !serverClientID.isEmpty else {
            throw BacklotGoogleSignInError.invalidConfiguration
        }

        self.clientID = clientID
        self.serverClientID = serverClientID
    }

    func signIn() async throws -> BacklotGoogleAuthorization {
        guard let presenter = Self.presentingViewController() else {
            throw BacklotGoogleSignInError.presentationUnavailable
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: clientID,
            serverClientID: serverClientID
        )

        try await configureSDKIfNeeded()
        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
        let user = result.user

        let subject = user.userID?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let idToken = user.idToken?.tokenString.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let accessToken = user.accessToken.tokenString.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !subject.isEmpty, !idToken.isEmpty, !accessToken.isEmpty else {
            throw BacklotGoogleSignInError.incompleteCredential
        }

        let profile = user.profile
        let email = profile?.email.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let displayName = profile?.name.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let avatarURL = profile?.imageURL(withDimension: 256)?.absoluteString ?? ""

        return BacklotGoogleAuthorization(
            providerSubject: subject,
            idToken: idToken,
            accessToken: accessToken,
            email: email,
            displayName: displayName,
            avatarURL: avatarURL
        )
    }

    func handleOpenURL(_ url: URL) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }

    private func configureSDKIfNeeded() async throws {
        try await withCheckedThrowingContinuation { continuation in
            GIDSignIn.sharedInstance.configure { error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    private static func presentingViewController() -> UIViewController? {
        let activeScenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }

        let root = activeScenes
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
            ?? activeScenes.flatMap(\.windows).first?.rootViewController

        return topViewController(from: root)
    }

    private static func topViewController(from root: UIViewController?) -> UIViewController? {
        if let navigation = root as? UINavigationController {
            return topViewController(from: navigation.visibleViewController)
        }
        if let tab = root as? UITabBarController {
            return topViewController(from: tab.selectedViewController)
        }
        if let presented = root?.presentedViewController {
            return topViewController(from: presented)
        }
        return root
    }
}
#endif
