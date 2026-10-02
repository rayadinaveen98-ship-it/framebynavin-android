import Foundation

struct BacklotGoogleRuntimeConfiguration: Equatable, Sendable {
    let iosClientID: String
    let serverClientID: String
    let reversedClientID: String
}

struct BacklotRuntimeConfiguration: Equatable, Sendable {
    let supabaseURL: URL
    let supabasePublishableKey: String
    let google: BacklotGoogleRuntimeConfiguration?

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

        let googleIOSClientID = resolvedValue(
            key: "BACKLOT_GOOGLE_IOS_CLIENT_ID",
            bundle: bundle,
            environment: environment
        )
        let googleServerClientID = resolvedValue(
            key: "BACKLOT_GOOGLE_SERVER_CLIENT_ID",
            bundle: bundle,
            environment: environment
        )
        let googleReversedClientID = resolvedValue(
            key: "BACKLOT_GOOGLE_REVERSED_CLIENT_ID",
            bundle: bundle,
            environment: environment
        )

        let google: BacklotGoogleRuntimeConfiguration?
        if
            let googleIOSClientID,
            let googleServerClientID,
            let googleReversedClientID,
            isRegisteredURLScheme(googleReversedClientID, bundle: bundle)
        {
            google = BacklotGoogleRuntimeConfiguration(
                iosClientID: googleIOSClientID,
                serverClientID: googleServerClientID,
                reversedClientID: googleReversedClientID
            )
        } else {
            google = nil
        }

        return BacklotRuntimeConfiguration(
            supabaseURL: url,
            supabasePublishableKey: publishableKey,
            google: google
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

    private static func isRegisteredURLScheme(_ scheme: String, bundle: Bundle) -> Bool {
        guard
            let urlTypes = bundle.object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]]
        else {
            return false
        }

        return urlTypes.contains { type in
            guard let schemes = type["CFBundleURLSchemes"] as? [String] else {
                return false
            }
            return schemes.contains(scheme)
        }
    }
}
