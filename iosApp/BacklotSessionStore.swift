import Combine
import BacklotShared

/// Native iOS owner for Backlot's provider-neutral shared session state.
///
/// Google/Apple/Supabase credentials stay in their native adapters. This store receives only a
/// completed product identity and converts it into the same shared session snapshot Android can use.
@MainActor
final class BacklotSessionStore: ObservableObject {
    @Published private(set) var snapshot: BacklotSessionSnapshot

    init() {
        snapshot = BacklotSharedApi.shared.signedOutSession()
    }

    var isSignedIn: Bool {
        snapshot.isSignedIn
    }

    /// Call before starting native sign-in or account switching. The shared reducer deliberately
    /// clears any previously visible identity while the next provider account is resolving.
    func beginAuthentication() {
        snapshot = BacklotSharedApi.shared.resolvingSession()
    }

    /// Native auth adapters call this only after provider authentication has completed.
    func acceptIdentity(
        provider: BacklotIdentityProvider,
        providerSubject: String,
        email: String = "",
        displayName: String = "",
        avatarURL: String = ""
    ) {
        snapshot = BacklotSharedApi.shared.signedInSession(
            provider: provider,
            providerSubject: providerSubject,
            email: email,
            displayName: displayName,
            avatarUrl: avatarURL
        )
    }

    func signOut() {
        snapshot = BacklotSharedApi.shared.signOutSession()
    }
}
