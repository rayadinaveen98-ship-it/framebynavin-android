import Foundation

#if canImport(Supabase)
import Supabase
#endif

/// Native boundary that exchanges a provider credential for a Backlot/Supabase auth session.
///
/// Provider ID tokens, raw nonces and Supabase access/refresh tokens stay on the native side of
/// the architecture. The shared KMP product model only receives provider-neutral identity fields.
protocol BacklotAppleIdentityExchanging {
    func exchange(_ authorization: BacklotAppleAuthorization) async throws -> BacklotNativeAuthSession
}

enum BacklotSupabaseIdentityExchangeError: Error, LocalizedError {
    case invalidConfiguration
    case incompleteSession

    var errorDescription: String? {
        switch self {
        case .invalidConfiguration:
            return "Backlot Supabase Auth is not configured with a valid project URL and publishable key."
        case .incompleteSession:
            return "Supabase returned an incomplete authentication session."
        }
    }
}

#if canImport(Supabase)
/// Supabase implementation of Backlot's native Apple identity exchange.
///
/// The app injects only a project URL and client-safe publishable key. No secret/service-role key
/// belongs in an iOS binary. The Apple ID token is exchanged through Supabase's native OIDC flow
/// using the *raw* nonce that corresponds to the SHA-256 nonce sent to AuthenticationServices.
final class BacklotSupabaseAppleIdentityExchange: BacklotAppleIdentityExchanging {
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

    func exchange(_ authorization: BacklotAppleAuthorization) async throws -> BacklotNativeAuthSession {
        let session = try await client.auth.signInWithIdToken(
            credentials: OpenIDConnectCredentials(
                provider: .apple,
                idToken: authorization.identityToken,
                nonce: authorization.rawNonce
            )
        )

        let firstSignInName = authorization.displayName
            .trimmingCharacters(in: .whitespacesAndNewlines)

        // Apple returns the full name only when permission is first granted. Persist it immediately
        // to Supabase user metadata so later launches/sign-ins can recover the creator's name.
        let resolvedUser: User
        if firstSignInName.isEmpty {
            resolvedUser = session.user
        } else {
            resolvedUser = try await client.auth.update(
                user: UserAttributes(data: [
                    "full_name": .string(firstSignInName),
                ])
            )
        }

        let email = resolvedUser.email?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let appleEmail = authorization.email
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let resolvedEmail = (email?.isEmpty == false ? email : nil) ?? appleEmail

        let metadataName = resolvedUser.userMetadata["full_name"]?.stringValue?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let resolvedName = firstSignInName.isEmpty ? (metadataName ?? "") : firstSignInName

        let avatarURL = resolvedUser.userMetadata["avatar_url"]?.stringValue
            ?? resolvedUser.userMetadata["picture"]?.stringValue
            ?? ""

        let subject = authorization.providerSubject
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let cloudAccountId = resolvedUser.id.uuidString.lowercased()
        guard
            !subject.isEmpty,
            !cloudAccountId.isEmpty,
            !session.accessToken.isEmpty,
            !session.refreshToken.isEmpty,
            session.expiresAt.isFinite,
            session.expiresAt > 0
        else {
            throw BacklotSupabaseIdentityExchangeError.incompleteSession
        }

        return BacklotNativeAuthSession(
            provider: .apple,
            providerSubject: subject,
            cloudAccountId: cloudAccountId,
            email: resolvedEmail,
            displayName: resolvedName,
            avatarURL: avatarURL,
            supabaseAccessToken: session.accessToken,
            supabaseRefreshToken: session.refreshToken,
            expiresAtEpochSeconds: Int64(session.expiresAt)
        )
    }
}
#endif
