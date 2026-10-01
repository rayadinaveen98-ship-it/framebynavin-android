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
    ///
    /// A nil cloud account id is accepted only for migration of provider-only sessions created by
    /// earlier builds. Fresh Supabase sign-ins use the canonical cloud-backed session path.
    func acceptIdentity(
        provider: BacklotIdentityProvider,
        providerSubject: String,
        cloudAccountId: String? = nil,
        email: String = "",
        displayName: String = "",
        avatarURL: String = ""
    ) {
        if let cloudAccountId {
            snapshot = BacklotSharedApi.shared.signedInCloudSession(
                provider: provider,
                providerSubject: providerSubject,
                cloudAccountId: cloudAccountId,
                email: email,
                displayName: displayName,
                avatarUrl: avatarURL
            )
        } else {
            snapshot = BacklotSharedApi.shared.signedInSession(
                provider: provider,
                providerSubject: providerSubject,
                email: email,
                displayName: displayName,
                avatarUrl: avatarURL
            )
        }
    }

    func signOut() {
        snapshot = BacklotSharedApi.shared.signOutSession()
    }
}
