import BacklotShared
import Foundation

/// Native Identity V2 orchestration boundary.
///
/// This coordinator is the only place that joins native provider authentication, Supabase session
/// exchange, secure persistence, and Backlot's provider-neutral shared product session. Provider
/// credentials and Supabase tokens remain in native types and are never passed to BacklotShared.
@MainActor
final class BacklotIdentityCoordinator {
    private let sessionStore: BacklotSessionStore
    private let identityRepository: BacklotNativeIdentityRepositoryProtocol
    private let appleSignIn: BacklotAppleSignInCoordinator
    private let appleExchange: BacklotAppleIdentityExchanging

    init(
        sessionStore: BacklotSessionStore,
        identityRepository: BacklotNativeIdentityRepositoryProtocol = BacklotNativeIdentityRepository(),
        appleSignIn: BacklotAppleSignInCoordinator = BacklotAppleSignInCoordinator(),
        appleExchange: BacklotAppleIdentityExchanging
    ) {
        self.sessionStore = sessionStore
        self.identityRepository = identityRepository
        self.appleSignIn = appleSignIn
        self.appleExchange = appleExchange
    }

    /// Restore an unexpired native session into the shared product state on app launch.
    ///
    /// Expired credentials are rejected by the repository and never promoted into shared state.
    @discardableResult
    func restorePersistedSession() -> Bool {
        sessionStore.beginAuthentication()

        do {
            guard let nativeSession = try identityRepository.persistedSession() else {
                sessionStore.signOut()
                return false
            }
            promote(nativeSession)
            return true
        } catch {
            sessionStore.signOut()
            return false
        }
    }

    /// Execute native Sign in with Apple and promote the resulting Supabase-backed identity only
    /// after the secure native session has been persisted successfully.
    func signInWithApple() async throws {
        sessionStore.beginAuthentication()

        do {
            let authorization = try await appleSignIn.signIn()
            let nativeSession = try await appleExchange.exchange(authorization)
            try identityRepository.persist(nativeSession)
            promote(nativeSession)
        } catch {
            // Failed account switching must not strand the UI in RESOLVING. If a previous valid
            // session exists, restore it; otherwise return to the explicit signed-out state.
            if let previous = try? identityRepository.persistedSession(), let previous {
                promote(previous)
            } else {
                sessionStore.signOut()
            }
            throw error
        }
    }

    /// Do not claim sign-out until secure native credentials have actually been removed.
    func signOut() throws {
        try identityRepository.signOut()
        sessionStore.signOut()
    }

    private func promote(_ nativeSession: BacklotNativeAuthSession) {
        let provider: BacklotIdentityProvider
        switch nativeSession.provider {
        case .apple:
            provider = .apple
        case .google:
            provider = .google
        }

        sessionStore.acceptIdentity(
            provider: provider,
            providerSubject: nativeSession.providerSubject,
            email: nativeSession.email,
            displayName: nativeSession.displayName,
            avatarURL: nativeSession.avatarURL
        )
    }
}
