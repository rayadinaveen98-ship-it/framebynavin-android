/**
 * Backlot deletion request Edge handler.
 *
 * All production deletion gates remain OFF. Web Origin is explicit allowlist
 * only: browser CORS is NOT authentication; every request still needs a user
 * bearer verified by Supabase Auth and owner-scoped SQL.
 */
export const CONFIRM = "DELETE MY BACKLOT ACCOUNT";
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const MAX_BODY = 256;
const STATES = new Set(["pending", "retryable", "complete"]);
const STAGES = new Set(["fenced", "storage_deleted", "data_deleted", "auth_deleted", "complete"]);

export type User = { id: string };
export type Receipt = { request_id: string; status: string; stage: string };
export interface DeletionBackend {
  enabled(): boolean;
  getUser(token: string): Promise<User | null>;
  request(token: string, confirmation: string): Promise<Receipt>;
  status(token: string): Promise<Receipt | null>;
}

function responseHeaders(origin?: string): Headers {
  const headers = new Headers({
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store, private",
    "Pragma": "no-cache",
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "no-referrer",
    "X-Frame-Options": "DENY",
    "Vary": "Origin",
  });
  if (origin) {
    // Exact reviewed web origin only. Deliberately no Allow-Credentials
    // because authentication must be an explicit bearer, not ambient cookies.
    headers.set("Access-Control-Allow-Origin", origin);
  }
  return headers;
}
function json(status: number, value: Record<string, unknown>, origin?: string): Response {
  return new Response(JSON.stringify(value), {
    status, headers: responseHeaders(origin),
  });
}
function bearer(req: Request): string | null {
  const value = req.headers.get("authorization");
  if (!value || !/^Bearer [A-Za-z0-9._~-]+$/.test(value) || value.length > 9000) return null;
  return value.substring(7);
}
function preflight(req: Request, origin: string): Response {
  const method = req.headers.get("access-control-request-method");
  if (method !== "GET" && method !== "POST") {
    return json(405, { error: "method_not_allowed" }, origin);
  }
  const raw = req.headers.get("access-control-request-headers") ?? "";
  const accepted = new Set(["authorization", "apikey", "content-type"]);
  const requested = raw.split(",").map((h) => h.trim().toLowerCase()).filter(Boolean);
  if (!requested.includes("authorization") ||
      requested.some((h) => !accepted.has(h))) {
    return json(403, { error: "headers_not_allowed" }, origin);
  }
  const headers = responseHeaders(origin);
  headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  headers.set("Access-Control-Allow-Headers", "authorization, apikey, content-type");
  headers.set("Access-Control-Max-Age", "0");
  return new Response(null, { status: 204, headers });
}
async function bodyLimited(req: Request): Promise<{ body: string; length: number; oversize: boolean }> {
  if (!req.body) return { body: "", length: 0, oversize: false };
  const reader = req.body.getReader();
  const chunks: Uint8Array[] = [];
  let length = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      length += value.byteLength;
      if (length > MAX_BODY) {
        await reader.cancel();
        return { body: "", length, oversize: true };
      }
      chunks.push(value);
    }
  } finally {
    reader.releaseLock();
  }
  const combined = new Uint8Array(length);
  let offset = 0;
  for (const chunk of chunks) {
    combined.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return { body: new TextDecoder("utf-8", { fatal: true }).decode(combined), length, oversize: false };
}

/**
 * browserAllowedOrigin MUST come from an explicitly owner-approved HTTPS
 * origin. By default it is undefined and ALL browser requests are rejected,
 * preserving Android's original native-client behavior.
 */
export async function handleRequest(
  req: Request, backend: DeletionBackend, browserAllowedOrigin?: string,
): Promise<Response> {
  const url = new URL(req.url);
  if (url.search || url.hash || !["/", "/functions/v1/backlot-account-deletion"].includes(url.pathname)) {
    return json(404, { error: "not_found" });
  }
  const origin = req.headers.get("origin");
  if (origin && (!browserAllowedOrigin || origin !== browserAllowedOrigin)) {
    return json(403, { error: "origin_not_supported" });
  }
  const safeOrigin = origin ?? undefined;
  const respond = (code: number, value: Record<string, unknown>) => json(code, value, safeOrigin);
  if (!backend.enabled()) return respond(503, { error: "deletion_unavailable" });
  if (req.method === "OPTIONS") {
    return origin ? preflight(req, origin) : respond(405, { error: "method_not_allowed" });
  }
  if (!["GET", "POST"].includes(req.method)) return respond(405, { error: "method_not_allowed" });

  const token = bearer(req);
  if (!token) return respond(401, { error: "auth_required" });
  try {
    const user = await backend.getUser(token);
    if (!user || !UUID.test(user.id)) return respond(401, { error: "auth_required" });
    if (req.method === "GET") {
      const receipt = await backend.status(token);
      if (receipt && (!UUID.test(receipt.request_id) || !STATES.has(receipt.status) ||
                      !STAGES.has(receipt.stage))) {
        return respond(503, { error: "temporarily_unavailable" });
      }
      return respond(200, receipt ?? { status: "none" });
    }
    if (req.headers.get("content-type")?.split(";")[0]?.trim().toLowerCase() !== "application/json") {
      return respond(415, { error: "json_required" });
    }
    // Browsers are forbidden to set Content-Length. If a native client sends
    // it, enforce its value. In all cases read at most MAX_BODY+1 bytes.
    const declared = req.headers.get("content-length");
    if (declared !== null &&
        (!/^[0-9]+$/.test(declared) || Number(declared) <= 0 || Number(declared) > MAX_BODY)) {
      return respond(413, { error: "payload_invalid" });
    }
    let data: unknown;
    try {
      const raw = await bodyLimited(req);
      if (raw.oversize) return respond(413, { error: "payload_invalid" });
      if (raw.length === 0) return respond(400, { error: "malformed_json" });
      if (declared !== null && Number(declared) !== raw.length) {
        return respond(400, { error: "length_mismatch" });
      }
      const matches = [...raw.body.matchAll(/"confirmation"\s*:/g)];
      if (matches.length !== 1) return respond(400, { error: "invalid_json_fields" });
      data = JSON.parse(raw.body);
    } catch {
      return respond(400, { error: "malformed_json" });
    }
    if (!data || typeof data !== "object" || Array.isArray(data) ||
        Object.keys(data).length !== 1 ||
        !Object.prototype.hasOwnProperty.call(data, "confirmation")) {
      return respond(400, { error: "invalid_fields" });
    }
    if ((data as Record<string, unknown>).confirmation !== CONFIRM) {
      return respond(422, { error: "confirmation_required" });
    }
    const receipt = await backend.request(token, CONFIRM);
    if (!receipt || !UUID.test(receipt.request_id) || !STATES.has(receipt.status) ||
        !STAGES.has(receipt.stage)) {
      return respond(503, { error: "temporarily_unavailable" });
    }
    return respond(202, {
      request_id: receipt.request_id,
      status: receipt.status,
      stage: receipt.stage,
    });
  } catch {
    return respond(503, { error: "temporarily_unavailable" });
  }
}
