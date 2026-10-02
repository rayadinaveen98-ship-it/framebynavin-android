import Foundation

struct BacklotRuntimeConfiguration: Equatable, Sendable {
    let supabaseURL: URL
    let supabasePublishableKey: String

    static func load(
        bundle: Bundle = .main,
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> BacklotRuntimeConfiguration? {
        let urlString = resolvedValue(
            key: "BACKLOT_SUPABASE_URL",
            bundle: bundle,
            environment: environment
        )
        let publishableKey = resolvedValue(
            key: "BACKLOT_SUPABASE_PUBLISHABLE_KEY",
            bundle: bundle,
            environment: environment
        )

        guard
            let urlString,
            let url = URL(string: urlString),
            url.scheme?.lowercased() == "https",
            url.host != nil,
            let publishableKey
        else {
            return nil
        }

        return BacklotRuntimeConfiguration(
            supabaseURL: url,
            supabasePublishableKey: publishableKey
        )
    }

    private static func resolvedValue(
        key: String,
        bundle: Bundle,
        environment: [String: String]
    ) -> String? {
        let raw = environment[key] ?? bundle.object(forInfoDictionaryKey: key) as? String
        guard let raw else { return nil }

        let value = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard
            !value.isEmpty,
            !value.hasPrefix("$("),
            !value.lowercased().contains("replace-me")
        else {
            return nil
        }
        return value
    }
}
