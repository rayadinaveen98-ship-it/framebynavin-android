/**
 * Backlot cloud account-deletion web flow primitives. Dependency-free and
 * testable with Node. NO credentials, tokens or backend writes at module load.
 *
 * The preview's config is intentionally OFF. GoTrue PKCE exchange requires the
 * verifier to stay in the SAME tab that initiated Google sign-in.
 */
export const CONFIRM = "DELETE MY BACKLOT ACCOUNT";
export const STORAGE_KEY = "backlot-web-pkce-v1";
export const REQUEST_PATH = "/deletion-request/";
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const STATES = new Set(["pending", "retryable", "complete"]);
const STAGES = new Set(["fenced", "storage_deleted", "data_deleted", "auth_deleted", "complete"]);

export function reviewConfiguration(config, currentUrl) {
  if (!config || config.enabled !== true) return { ready: false };
  const loc = new URL(currentUrl);
  if (loc.protocol !== "https:" || loc.username || loc.password ||
      loc.pathname !== REQUEST_PATH || loc.port || loc.origin !== config.approvedOrigin) {
    return { ready: false };
  }
  if (typeof config.supabaseUrl !== "string" || typeof config.publishableKey !== "string") {
    return { ready: false };
  }
  const api = new URL(config.supabaseUrl);
  if (api.protocol !== "https:" || !api.hostname.endsWith(".supabase.co") ||
      api.pathname !== "/" || api.search || api.hash || api.username || api.password ||
      api.origin !== config.supabaseUrl) return { ready: false };
  if (!/^sb_publishable_[A-Za-z0-9_-]{20,}$/.test(config.publishableKey)) {
    return { ready: false };
  }
  const target = loc.origin + REQUEST_PATH;
  return { ready: true, target, apiOrigin: api.origin, publicKey: config.publishableKey };
}

export function base64Url(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

export async function createChallenge(cryptoApi) {
  const random = cryptoApi.getRandomValues(new Uint8Array(48));
  const verifier = base64Url(random);
  const digest = await cryptoApi.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
  return { verifier, challenge: base64Url(new Uint8Array(digest)) };
}

export function googleLoginUrl(apiOrigin, redirectTarget, challenge) {
  if (!/^[A-Za-z0-9_-]{43}$/.test(challenge)) throw Error("Invalid PKCE challenge");
  const authorize = new URL(apiOrigin + "/auth/v1/authorize");
  authorize.searchParams.set("provider", "google");
  authorize.searchParams.set("redirect_to", redirectTarget);
  authorize.searchParams.set("code_challenge", challenge);
  authorize.searchParams.set("code_challenge_method", "s256");
  // Strong Google re-authentication is a separate approval gate, not guaranteed
  // by OAuth alone. The backend verifies a newly created Auth session.
  return authorize.href;
}

export function initialCallback(search) {
  const params = new URLSearchParams(search);
  const codes = params.getAll("code");
  if (params.has("error") || params.has("error_description")) return { error: true };
  if (codes.length === 0) return { code: null };
  if (codes.length !== 1 || !/^[A-Za-z0-9._~-]{8,1024}$/.test(codes[0])) {
    return { error: true };
  }
  return { code: codes[0] };
}

export function validSavedPkce(value, redirect, now) {
  if (!value || typeof value !== "object" || value.v !== 1 ||
      value.redirect !== redirect || typeof value.timestamp !== "number" ||
      now < value.timestamp || now - value.timestamp > 300_000 ||
      typeof value.verifier !== "string" ||
      !/^[A-Za-z0-9_-]{43,128}$/.test(value.verifier)) return null;
  return value.verifier;
}

export function parseReceipt(data, allowNone = false) {
  if (allowNone && data && Object.keys(data).length === 1 && data.status === "none") return null;
  if (!data || typeof data !== "object" || Array.isArray(data) ||
      Object.keys(data).length !== 3 || !UUID.test(data.request_id) ||
      !STATES.has(data.status) || !STAGES.has(data.stage)) {
    throw Error("Deletion receipt verification failed");
  }
  return { requestId: data.request_id, status: data.status, stage: data.stage };
}

export function parseUser(data) {
  if (!data || typeof data !== "object" || Array.isArray(data) ||
      typeof data.id !== "string" || !UUID.test(data.id) ||
      typeof data.email !== "string" || data.email.length > 320) {
    throw Error("Authenticated user response invalid");
  }
  return { id: data.id, email: data.email };
}

export function requestHeaders(key, accessToken, withBody = false) {
  if (typeof key !== "string" || typeof accessToken !== "string" ||
      !/^[A-Za-z0-9._~-]{20,9000}$/.test(accessToken)) throw Error("Invalid session");
  const result = {
    apikey: key, Authorization: "Bearer " + accessToken,
    Accept: "application/json", "Cache-Control": "no-store",
  };
  if (withBody) result["Content-Type"] = "application/json";
  return result;
}
