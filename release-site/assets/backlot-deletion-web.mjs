/**
 * Backlot owner-only external deletion request page.
 * Never executes a request while preview config is OFF.
 * No third-party SDK, external scripts, trackers, localStorage, cookies,
 * backend admin keys or persistent access/refresh tokens.
 */
import { config } from "./backlot-deletion-config.mjs";
import {
  CONFIRM, STORAGE_KEY, reviewConfiguration, createChallenge, googleLoginUrl,
  initialCallback, validSavedPkce, parseReceipt, parseUser, requestHeaders,
} from "./backlot-deletion-core.mjs";

const $ = (id) => document.getElementById(id);
const info = $("request-status");
const startButton = $("request-signin");
const accountSection = $("request-account");
const accountText = $("request-account-email");
const statusText = $("request-cloud-status");
const phrase = $("request-phrase");
const reviewButton = $("request-review");
const confirmDialog = $("request-confirm-dialog");
const submitButton = $("request-submit");
const cancelButton = $("request-cancel");
const signOutButton = $("request-signout");
const refreshButton = $("request-refresh");
const options = $("request-options");
let session = null;
let busy = false;

function announce(message) { info.textContent = message; }
function lockUi(working) {
  busy = working;
  startButton.disabled = working;
  reviewButton.disabled = working || phrase.value !== CONFIRM || !session;
  submitButton.disabled = working || phrase.value !== CONFIRM || !session;
  refreshButton.disabled = working || !session;
  signOutButton.disabled = working || !session;
}
function clearSession() {
  session = null;
  phrase.value = "";
  accountText.textContent = "";
  statusText.textContent = "";
  options.hidden = true;
  accountSection.hidden = true;
  lockUi(false);
}
function showReceipt(receipt) {
  options.hidden = true;
  if (!receipt) {
    statusText.textContent = "No deletion request recorded for this account.";
    options.hidden = false;
    return;
  }
  if (receipt.status === "complete" && receipt.stage === "complete") {
    statusText.textContent = "The service reports that account deletion is complete.";
  } else {
    statusText.textContent = "Deletion request status: " + receipt.status +
      " (stage: " + receipt.stage + "). Your account has NOT been confirmed deleted.";
  }
}
async function readJsonBounded(response) {
  if (!response.body) throw Error("Empty server response");
  const declared = response.headers.get("content-length");
  if (declared && Number(declared) > 4096) throw Error("Response exceeded limit");
  const reader = response.body.getReader();
  const chunks = [];
  let size = 0;
  try {
    while (true) {
      const part = await reader.read();
      if (part.done) break;
      size += part.value.byteLength;
      if (size > 4096) {
        await reader.cancel();
        throw Error("Response exceeded limit");
      }
      chunks.push(part.value);
    }
  } finally {
    reader.releaseLock();
  }
  const bytes = new Uint8Array(size);
  let position = 0;
  for (const part of chunks) {
    bytes.set(part, position);
    position += part.length;
  }
  return JSON.parse(new TextDecoder("utf-8", { fatal: true }).decode(bytes));
}
async function requestJson(url, { method, headers, body } = {}) {
  const response = await fetch(url, {
    method: method ?? "GET",
    headers: headers ?? {}, body,
    redirect: "error", cache: "no-store",
    credentials: "omit", referrerPolicy: "no-referrer",
    signal: AbortSignal.timeout(15_000),
  });
  if (!response.ok) throw Error("Secure service unavailable or sign-in expired");
  return readJsonBounded(response);
}
async function freshIdentity(approved, token) {
  const user = await requestJson(approved.apiOrigin + "/auth/v1/user", {
    headers: requestHeaders(approved.publicKey, token),
  });
  return parseUser(user);
}
async function loadReceipt(approved) {
  if (!session) return;
  const response = await requestJson(
    approved.apiOrigin + "/functions/v1/backlot-account-deletion",
    { headers: requestHeaders(approved.publicKey, session.token) },
  );
  showReceipt(parseReceipt(response, true));
}
async function authenticateCode(approved, code) {
  const stored = sessionStorage.getItem(STORAGE_KEY);
  // Consume challenge BEFORE attempting exchange to prevent redirect replay.
  sessionStorage.removeItem(STORAGE_KEY);
  let data;
  try { data = stored ? JSON.parse(stored) : null; }
  catch { data = null; }
  const verifier = validSavedPkce(data, approved.target, Date.now());
  if (!verifier) throw Error("Sign-in expired. Start Google sign-in again.");
  const response = await requestJson(approved.apiOrigin + "/auth/v1/token?grant_type=pkce", {
    method: "POST",
    headers: { apikey: approved.publicKey, "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ auth_code: code, code_verifier: verifier }),
  });
  if (!response || typeof response.access_token !== "string" ||
      !/^[A-Za-z0-9._~-]{20,9000}$/.test(response.access_token)) {
    throw Error("Invalid OAuth code exchange");
  }
  const identity = await freshIdentity(approved, response.access_token);
  // Keep token in this page's memory only; avoid refresh token persistence.
  session = { token: response.access_token, identity };
  accountText.textContent = "Signed in as " + identity.email;
  accountSection.hidden = false;
  await loadReceipt(approved);
}
async function beginGoogleSignIn(approved) {
  if (busy) return;
  lockUi(true);
  announce("Opening Google sign-in. Your account will not be deleted by signing in.");
  try {
    const pkce = await createChallenge(crypto);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
      v: 1, verifier: pkce.verifier, timestamp: Date.now(), redirect: approved.target,
    }));
    window.location.assign(googleLoginUrl(approved.apiOrigin, approved.target, pkce.challenge));
  } catch {
    sessionStorage.removeItem(STORAGE_KEY);
    announce("Google sign-in could not start. No deletion request was submitted.");
    lockUi(false);
  }
}
async function performDeletion(approved) {
  if (busy || !session || phrase.value !== CONFIRM) return;
  lockUi(true);
  announce("Sending verified account-deletion request…");
  try {
    // Confirm bearer still resolves to the account displayed on this page.
    const identity = await freshIdentity(approved, session.token);
    if (identity.id !== session.identity.id) throw Error("Account switched");
    const data = await requestJson(
      approved.apiOrigin + "/functions/v1/backlot-account-deletion", {
        method: "POST",
        headers: requestHeaders(approved.publicKey, session.token, true),
        body: JSON.stringify({ confirmation: CONFIRM }),
      },
    );
    const receipt = parseReceipt(data);
    phrase.value = "";
    showReceipt(receipt);
    announce("The deletion service received your request. It is not necessarily complete.");
  } catch {
    announce("Unable to verify or submit account deletion. Do not assume the account was deleted.");
    statusText.textContent = "Request not confirmed. Sign in again or check the service status.";
  } finally {
    confirmDialog.close();
    lockUi(false);
  }
}
async function logOut(approved) {
  if (!session) return;
  const token = session.token;
  clearSession();
  sessionStorage.removeItem(STORAGE_KEY);
  announce("Signed out of this website. Signing out does not delete your account.");
  try {
    await requestJson(approved.apiOrigin + "/auth/v1/logout", {
      method: "POST",
      headers: requestHeaders(approved.publicKey, token),
    });
  } catch {
    // Local token already discarded. Server revocation may be unavailable.
  }
}

async function start() {
  const query = window.location.search;
  // Auth codes and provider errors must never remain in browser history.
  if (query) window.history.replaceState(null, "", window.location.pathname);
  const approved = reviewConfiguration(config, window.location.href);
  if (!approved.ready) {
    sessionStorage.removeItem(STORAGE_KEY);
    clearSession();
    startButton.disabled = true;
    announce("Account deletion requests are not available in this preview. No request was submitted.");
    return;
  }
  const callback = initialCallback(query);
  if (callback.error) {
    announce("Google sign-in did not finish. No account-deletion request was sent.");
  }
  startButton.disabled = false;
  startButton.addEventListener("click", () => beginGoogleSignIn(approved));
  phrase.addEventListener("input", () => lockUi(busy));
  reviewButton.addEventListener("click", () => {
    if (session && !busy && phrase.value === CONFIRM) confirmDialog.showModal();
  });
  cancelButton.addEventListener("click", () => confirmDialog.close());
  submitButton.addEventListener("click", () => performDeletion(approved));
  refreshButton.addEventListener("click", async () => {
    if (busy) return;
    lockUi(true);
    try {
      const identity = await freshIdentity(approved, session.token);
      if (identity.id !== session.identity.id) throw Error("Account switched");
      await loadReceipt(approved);
      announce("Request status refreshed from the secure service.");
    } catch {
      announce("Could not verify the deletion status. Please sign in again.");
    } finally { lockUi(false); }
  });
  signOutButton.addEventListener("click", () => logOut(approved));
  if (callback.code) {
    lockUi(true);
    announce("Verifying Google sign-in. This does not delete your account.");
    try {
      await authenticateCode(approved, callback.code);
      announce("Signed in. Review deletion scope before making any request.");
    } catch {
      clearSession();
      announce("Could not confirm Google sign-in. No deletion request was submitted.");
    } finally { lockUi(false); }
  }
}
start().catch(() => {
  clearSession();
  startButton.disabled = true;
  announce("Secure deletion is currently unavailable. No request was submitted.");
});
