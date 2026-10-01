import SwiftUI
import BacklotShared

struct BacklotRootView: View {
    @State private var selection: BacklotTab = .today
    @StateObject private var sessionStore = BacklotSessionStore()

    var body: some View {
        TabView(selection: $selection) {
            BacklotPlaceholderScreen(
                eyebrow: "BACKLOT",
                title: "Today",
                message: sessionStore.isSignedIn
                    ? "Your creator day, next move and active projects will live here. Creator cloud identity is connected through the shared Backlot session."
                    : "Your creator day, next move and active projects will live here. This device is currently signed out of the creator cloud."
            )
            .tabItem { Label("Today", systemImage: "sparkles") }
            .tag(BacklotTab.today)

            BacklotPlaceholderScreen(
                eyebrow: "CAPTURE",
                title: "Ideas",
                message: "Text and Voice Ideas will sync with the same creator cloud as Android."
            )
            .tabItem { Label("Ideas", systemImage: "lightbulb") }
            .tag(BacklotTab.ideas)

            BacklotSharedCoreProofView()
                .tabItem { Label("Create", systemImage: "film.stack") }
                .tag(BacklotTab.create)

            BacklotPlaceholderScreen(
                eyebrow: "PLAN",
                title: "Calendar",
                message: "Publishing dates and creator deadlines will use native EventKit integration."
            )
            .tabItem { Label("Calendar", systemImage: "calendar") }
            .tag(BacklotTab.calendar)

            BacklotPlaceholderScreen(
                eyebrow: "LEARN",
                title: "Insights",
                message: "YouTube performance and Creator Brain guidance will land here."
            )
            .tabItem { Label("Insights", systemImage: "chart.line.uptrend.xyaxis") }
            .tag(BacklotTab.insights)
        }
        .tint(.primary)
        .environmentObject(sessionStore)
    }
}

private enum BacklotTab: Hashable {
    case today
    case ideas
    case create
    case calendar
    case insights
}

private struct BacklotPlaceholderScreen: View {
    let eyebrow: String
    let title: String
    let message: String

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text(eyebrow)
                        .font(.caption2.weight(.bold))
                        .tracking(1.5)
                        .foregroundStyle(.secondary)
                    Text(title)
                        .font(.largeTitle.bold())
                    Text(message)
                        .font(.body)
                        .foregroundStyle(.secondary)
                    Spacer(minLength: 24)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
            }
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

/**
 A temporary proof screen: Swift calls the same Kotlin Workflow V2 engine that Android now uses.
 It will be replaced by the real Project/Workspace UI after cloud identity and Ideas are connected.
 */
private struct BacklotSharedCoreProofView: View {
    private let template = BacklotSharedApi.shared.resolveWorkflow(
        creatorModeId: "film_entertainment",
        archetypeId: "analysis",
        archetypeLabel: "Analysis",
        platform: "YouTube",
        deliveryFormat: "Long-form",
        legacyContentType: "Long-form"
    )

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text("CREATE")
                        .font(.caption2.weight(.bold))
                        .tracking(1.5)
                        .foregroundStyle(.secondary)
                    Text("Shared Core Connected")
                        .font(.largeTitle.bold())

                    if let template {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(template.label)
                                .font(.headline)
                            Text("Workflow: \(template.stages.count) stages")
                                .foregroundStyle(.secondary)
                            Text(template.stages.first?.label ?? "No stage")
                                .font(.title3.weight(.semibold))
                            Text(template.stages.first?.action ?? "")
                                .foregroundStyle(.secondary)
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    } else {
                        Text("Shared workflow unavailable")
                            .foregroundStyle(.secondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
            }
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
