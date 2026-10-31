/**
 * OFF BY DEFAULT. Never flip this flag simply to test a real account.
 * The approved HTTPS site domain, Google OAuth redirect allowlist,
 * reviewed support contact, live deletion backend and privileged Storage
 * fences MUST be accepted together before any public release.
 *
 * The publishable key is nonsecret, but is deliberately left blank until
 * an owner-reviewed website deployment configuration exists.
 */
export const config = Object.freeze({
  enabled: false,
  approvedOrigin: "",
  supabaseUrl: "https://kukkqgpzxnfanynbddiw.supabase.co",
  publishableKey: "",
});
