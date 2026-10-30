/**
 * Backlot Edge Function: USER-SCOPED request/status ONLY. No worker entrypoint.
 *
 * This source is intentionally NOT deployed and DISABLED BY DEFAULT. The
 * related SQL contract is reviewed in isolation, not a production migration.
 * No secrets in app or website; this module never uses SERVICE_ROLE.
 */
import { handleRequest, type DeletionBackend, type Receipt, type User } from "./handler.ts";

function config(): { url: string; anon: string; enabled: boolean } {
  const enabled = Deno.env.get("BACKLOT_DELETION_API_ENABLED") === "true" &&
    Deno.env.get("BACKLOT_DELETION_SCHEMA_REVIEWED") === "true" &&
    Deno.env.get("BACKLOT_DELETION_STORAGE_FENCE_REVIEWED") === "true" &&
    Deno.env.get("BACKLOT_DELETION_OWNER_APPROVED") === "true";
  const url = Deno.env.get("SUPABASE_URL") ?? "";
  const anon = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  if (!enabled || !anon) return { url: "", anon: "", enabled: false };
  try {
    const u = new URL(url);
    // Don't use an accidental plaintext hosted URL. Local integration is
    // permitted on loopback; real deployed traffic must use HTTPS.
    if (u.protocol !== "https:" &&
        !(u.protocol === "http:" && ["localhost", "127.0.0.1"].includes(u.hostname))) {
      return { url: "", anon: "", enabled: false };
    }
    if (u.username || u.password || u.search || u.hash || u.pathname !== "/") {
      return { url: "", anon: "", enabled: false };
    }
  } catch {
    return { url: "", anon: "", enabled: false };
  }
  return { url: url.replace(/\/$/, ""), anon, enabled };
}
const settings = config();
async function call(token: string, url: string, method: string, body?: unknown): Promise<unknown> {
  const response = await fetch(url, {
    method,
    redirect: "error",
    signal: AbortSignal.timeout(8_000),
    headers: {
      "Authorization": "Bearer " + token,
      "apikey": settings.anon,
      "Accept": "application/json",
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) throw new Error("Upstream rejected authenticated operation");
  return await response.json();
}
const backend: DeletionBackend = {
  enabled: () => settings.enabled,
  async getUser(token: string): Promise<User | null> {
    try {
      const data = await call(token, settings.url + "/auth/v1/user", "GET");
      const result = data as { id?: unknown };
      return typeof result?.id === "string" ? { id: result.id } : null;
    } catch {
      return null;
    }
  },
  async request(token: string, confirmation: string): Promise<Receipt> {
    const result = await call(
      token, settings.url + "/rest/v1/rpc/backlot_request_deletion", "POST",
      { p_confirmation: confirmation },
    );
    return result as Receipt; // SQL RPC verifies auth.uid(), session & durable fence
  },
  async status(token: string): Promise<Receipt | null> {
    const result = await call(
      token, settings.url + "/rest/v1/rpc/backlot_my_deletion_status", "POST", {},
    );
    return result as Receipt | null;
  },
};
const requestHandler = (req: Request) => handleRequest(req, backend);
if (Deno.env.get("BACKLOT_RC28_LOOPBACK_ONLY") === "true") {
  Deno.serve({ hostname: "127.0.0.1", port: 8000 }, requestHandler);
} else {
  Deno.serve(requestHandler);
}
