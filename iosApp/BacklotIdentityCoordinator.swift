import BacklotShared
import Foundation

enum BacklotIdentityCoordinatorError: Error, LocalizedError {
    case googleNotConfigured

    var errorDescription: String? {
        switch self {
        case .googleNotConfigured:
            return "Google Sign-In is not configured for this Backlot build."
        }
    }
}

/// Native Identity V2 orchestration boundary.
///
/// This coordinator is the only place that joins native provider authentication, Supabase session
/// exchange/refresh, secure persistence, and Backlot's provider-neutral shared product session.
/// Provider credentials and Supabase tokens remain in native types and are never passed to
/// BacklotShared.
@MainActor
final class BacklotIdentityCoordinator {
    private let sessionStore: BacklotSessionStore
    private let identityRepository: BacklotNativeIdentityRepositoryProtocol
    private let appleSignIn: BacklotAppleSignInCoordinator
    private let appleExchange: BacklotAppleIdentityExchanging
    private let googleSignIn: BacklotGoogleAuthorizing?
    private let googleExchange: BacklotGoogleIdentityExchanging?
    private let sessionRefresher: BacklotNativeSessionRefreshing?

    init(
        sessionStore: BacklotSessionStore,
        identityRepository: BacklotNativeIdentityRepositoryProtocol = BacklotNativeIdentityRepository(),
        appleSignIn: BacklotAppleSignInCoordinator? = nil,
        appleExchange: BacklotAppleIdentityExchanging,
        googleSignIn: BacklotGoogleAuthorizing? = nil,
        googleExchange: BacklotGoogleIdentityExchanging? = nil,
        sessionRefresher: BacklotNativeSessionRefreshing? = nil
    ) {
        self.sessionStore = sessionStore
        self.identityRepository = identityRepository
        self.appleSignIn = appleSignIn ?? BacklotAppleSignInCoordinator()
        self.appleExchange = appleExchange
        self.googleSignIn = googleSignIn
        self.googleExchange = googleExchange
        self.sessionRefresher = sessionRefresher
    }

    /// Restore a native session into shared product state on app launch.
    ///
    /// A still-valid access token is promoted immediately. An expired access token is refreshed
    /// through Supabase when a refresher is configured; because Supabase rotates refresh tokens,
    /// the refreshed native session is persisted before it becomes visible to shared product state.
    @discardableResult
    func restorePersistedSession() async -> Bool {
        sessionStore.beginAuthentication()

        do {
            guard let nativeSession = try identityRepository.storedSession() else {
                sessionStore.signOut()
                return false
            }

            if !nativeSession.isExpired {
                promote(nativeSession)
                return true
            }

            guard let sessionRefresher else {
                sessionStore.signOut()
                return false
            }

            let refreshedSession = try await sessionRefresher.refresh(nativeSession)
            try identityRepository.persist(refreshedSession)
            promote(refreshedSession)
            return true
        } catch {
            // Keep any stored refresh token on transient/network failure so a later launch can retry.
            // Shared product state remains signed out until a native session is proven valid again.
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
            restorePreviousSessionOrSignOut()
            throw error
        }
    }

    /// Execute native Sign in with Google, exchange the Google ID/access tokens through Supabase,
    /// then persist and promote only the provider-neutral Backlot identity.
    func signInWithGoogle() async throws {
        guard let googleSignIn, let googleExchange else {
            throw BacklotIdentityCoordinatorError.googleNotConfigured
        }

        sessionStore.beginAuthentication()

        do {
            let authorization = try await googleSignIn.signIn()
            let nativeSession = try await googleExchange.exchange(authorization)
            try identityRepository.persist(nativeSession)
            promote(nativeSession)
        } catch {
            restorePreviousSessionOrSignOut()
            throw error
        }
    }

    func handleOpenURL(_ url: URL) -> Bool {
        googleSignIn?.handleOpenURL(url) ?? false
    }

    /// Do not claim sign-out until secure native credentials have actually been removed.
    func signOut() throws {
        try identityRepository.signOut()
        googleSignIn?.signOut()
        sessionStore.signOut()
    }

    private func restorePreviousSessionOrSignOut() {
        if let previous = try? identityRepository.persistedSession() {
            promote(previous)
        } else {
            sessionStore.signOut()
        }
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
            cloudAccountId: nativeSession.cloudAccountId,
            email: nativeSession.email,
            displayName: nativeSession.displayName,
            avatarURL: nativeSession.avatarURL
        )
    }
}
