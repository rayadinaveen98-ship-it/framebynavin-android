/**
 * Backlot deletion request Edge Function HTTP handler.
 *
 * DEPLOYABLE source, but intentionally disabled by default. DO NOT DEPLOY or
 * enable until SQL, Storage and privileged-writer fencing + owner approval.
 * Accepts ONLY a user JWT; backend RPC enforces owner and fresh Auth session.
 * This endpoint never receives a service role key and never deletes data.
 */
export const CONFIRM = "DELETE MY BACKLOT ACCOUNT";
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const MAX_BODY = 256;
export type User = { id: string };
export type Receipt = { request_id: string; status: string; stage: string };
export interface DeletionBackend {
  enabled(): boolean;
  getUser(token: string): Promise<User | null>;
  request(token: string, confirmation: string): Promise<Receipt>;
  status(token: string): Promise<Receipt | null>;
}
function json(status: number, value: Record<string, unknown>): Response {
  return new Response(JSON.stringify(value), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store, private",
      "Pragma": "no-cache",
      "X-Content-Type-Options": "nosniff",
      "Referrer-Policy": "no-referrer",
      "X-Frame-Options": "DENY",
    },
  });
}
function bearer(req: Request): string | null {
  const value = req.headers.get("authorization");
  if (!value || !/^Bearer [A-Za-z0-9._~-]+$/.test(value) || value.length > 9000) return null;
  return value.substring(7);
}
/**
 * No Access-Control-Allow-Origin: an external browser must use a separately
 * secured owner-verified login/BFF flow. This is not a public web deletion URL.
 */
export async function handleRequest(req: Request, backend: DeletionBackend): Promise<Response> {
  const url = new URL(req.url);
  if (url.search || url.hash || !["/", "/functions/v1/backlot-account-deletion"].includes(url.pathname)) {
    return json(404, { error: "not_found" });
  }
  if (!backend.enabled()) return json(503, { error: "deletion_unavailable" });
  if (!["GET", "POST"].includes(req.method)) return json(405, { error: "method_not_allowed" });
  if (req.headers.get("origin")) {
    // Browser direct calls will be implemented only with approved web OAuth.
    return json(403, { error: "origin_not_supported" });
  }
  const token = bearer(req);
  if (!token) return json(401, { error: "auth_required" });
  try {
    const user = await backend.getUser(token);
    if (!user || !UUID.test(user.id)) return json(401, { error: "auth_required" });
    if (req.method === "GET") {
      const result = await backend.status(token);
      return json(200, result ?? { status: "none" });
    }
    if (req.headers.get("content-type")?.split(";")[0]?.trim().toLowerCase() !== "application/json") {
      return json(415, { error: "json_required" });
    }
    const length = req.headers.get("content-length");
    if (!length || !/^[0-9]+$/.test(length) || Number(length) <= 0 ||
        Number(length) > MAX_BODY) {
      return json(413, { error: "payload_invalid" });
    }
    let data: unknown;
    try {
      const body = await req.text();
      if (new TextEncoder().encode(body).byteLength !== Number(length)) {
        return json(400, { error: "length_mismatch" });
      }
      // Strictly limit accepted JSON to the single confirmation string.
      // Reject duplicate keys, not just additional parsed properties.
      const matchingKeys = [...body.matchAll(/"confirmation"\s*:/g)];
      if (matchingKeys.length !== 1) return json(400, { error: "invalid_json_fields" });
      data = JSON.parse(body);
    } catch {
      return json(400, { error: "malformed_json" });
    }
    if (!data || typeof data !== "object" || Array.isArray(data) ||
        Object.keys(data).length !== 1 ||
        !Object.prototype.hasOwnProperty.call(data, "confirmation")) {
      return json(400, { error: "invalid_fields" });
    }
    const record = data as Record<string, unknown>;
    if (record.confirmation !== CONFIRM) {
      return json(422, { error: "confirmation_required" });
    }
    const receipt = await backend.request(token, CONFIRM);
    if (!UUID.test(receipt.request_id) ||
        !["pending", "retryable", "complete"].includes(receipt.status)) {
      return json(503, { error: "temporary_unavailable" });
    }
    return json(202, {
      request_id: receipt.request_id,
      status: receipt.status,
      stage: receipt.stage,
    });
  } catch {
    // Never include database error text, private account IDs or JWTs.
    return json(503, { error: "temporarily_unavailable" });
  }
}
