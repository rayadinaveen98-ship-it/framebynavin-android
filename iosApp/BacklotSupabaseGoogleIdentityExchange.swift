import Foundation

#if canImport(Supabase)
import Supabase
#endif

protocol BacklotGoogleIdentityExchanging {
    func exchange(_ authorization: BacklotGoogleAuthorization) async throws -> BacklotNativeAuthSession
}

#if canImport(Supabase)
final class BacklotSupabaseGoogleIdentityExchange: BacklotGoogleIdentityExchanging {
    private let client: SupabaseClient

    init(supabaseURL: URL, publishableKey: String) throws {
        let key = publishableKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard
            let scheme = supabaseURL.scheme?.lowercased(),
            scheme == "https",
            supabaseURL.host != nil,
            !key.isEmpty
        else {
            throw BacklotSupabaseIdentityExchangeError.invalidConfiguration
        }

        client = SupabaseClient(
            supabaseURL: supabaseURL,
            supabaseKey: key
        )
    }

    func exchange(_ authorization: BacklotGoogleAuthorization) async throws -> BacklotNativeAuthSession {
        let session = try await client.auth.signInWithIdToken(
            credentials: OpenIDConnectCredentials(
                provider: .google,
                idToken: authorization.idToken,
                accessToken: authorization.accessToken
            )
        )

        let user = session.user
        let providerSubject = authorization.providerSubject
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let cloudAccountId = user.id.uuidString.lowercased()

        let supabaseEmail = user.email?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let fallbackEmail = authorization.email
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let resolvedEmail = (supabaseEmail?.isEmpty == false ? supabaseEmail : nil) ?? fallbackEmail

        let metadataName = user.userMetadata["full_name"]?.stringValue?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            ?? user.userMetadata["name"]?.stringValue?
                .trimmingCharacters(in: .whitespacesAndNewlines)
        let fallbackName = authorization.displayName
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let resolvedName = (metadataName?.isEmpty == false ? metadataName : nil) ?? fallbackName

        let metadataAvatar = user.userMetadata["avatar_url"]?.stringValue?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            ?? user.userMetadata["picture"]?.stringValue?
                .trimmingCharacters(in: .whitespacesAndNewlines)
        let fallbackAvatar = authorization.avatarURL
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let resolvedAvatar = (metadataAvatar?.isEmpty == false ? metadataAvatar : nil) ?? fallbackAvatar

        guard
            !providerSubject.isEmpty,
            !cloudAccountId.isEmpty,
            !session.accessToken.isEmpty,
            !session.refreshToken.isEmpty,
            session.expiresAt.isFinite,
            session.expiresAt > 0
        else {
            throw BacklotSupabaseIdentityExchangeError.incompleteSession
        }

        return BacklotNativeAuthSession(
            provider: .google,
            providerSubject: providerSubject,
            cloudAccountId: cloudAccountId,
            email: resolvedEmail,
            displayName: resolvedName,
            avatarURL: resolvedAvatar,
            supabaseAccessToken: session.accessToken,
            supabaseRefreshToken: session.refreshToken,
            expiresAtEpochSeconds: Int64(session.expiresAt)
        )
    }
}
#endif
