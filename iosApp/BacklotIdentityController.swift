import Combine
import Foundation

@MainActor
final class BacklotIdentityController: ObservableObject {
    let sessionStore: BacklotSessionStore

    @Published private(set) var isConfigured: Bool = false
    @Published private(set) var isWorking: Bool = false
    @Published private(set) var errorMessage: String?

    private let coordinator: BacklotIdentityCoordinator?
    private var sessionObservation: AnyCancellable?

    init(
        sessionStore: BacklotSessionStore = BacklotSessionStore(),
        configuration: BacklotRuntimeConfiguration? = BacklotRuntimeConfiguration.load()
    ) {
        self.sessionStore = sessionStore

        #if canImport(Supabase)
        if let configuration {
            do {
                let appleExchange = try BacklotSupabaseAppleIdentityExchange(
                    supabaseURL: configuration.supabaseURL,
                    publishableKey: configuration.supabasePublishableKey
                )
                let sessionRefresher = try BacklotSupabaseSessionRefresher(
                    supabaseURL: configuration.supabaseURL,
                    publishableKey: configuration.supabasePublishableKey
                )
                coordinator = BacklotIdentityCoordinator(
                    sessionStore: sessionStore,
                    appleExchange: appleExchange,
                    sessionRefresher: sessionRefresher
                )
                isConfigured = true
            } catch {
                coordinator = nil
                errorMessage = error.localizedDescription
            }
        } else {
            coordinator = nil
        }
        #else
        coordinator = nil
        #endif

        sessionObservation = sessionStore.objectWillChange.sink { [weak self] _ in
            self?.objectWillChange.send()
        }
    }

    var isSignedIn: Bool {
        sessionStore.isSignedIn
    }

    func restoreSession() async {
        guard let coordinator else { return }
        isWorking = true
        errorMessage = nil
        defer { isWorking = false }
        _ = await coordinator.restorePersistedSession()
    }

    func signInWithApple() async {
        guard let coordinator else { return }
        isWorking = true
        errorMessage = nil
        defer { isWorking = false }

        do {
            try await coordinator.signInWithApple()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func signOut() {
        guard let coordinator else {
            sessionStore.signOut()
            return
        }
        errorMessage = nil
        do {
            try coordinator.signOut()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
