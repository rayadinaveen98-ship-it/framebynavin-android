import { CONFIRM, handleRequest, type DeletionBackend } from "./handler.ts";

const UID = "8f571e95-a5cd-4f38-aa11-127310b78ab1";
const RID = "9fbf2f34-f55b-427b-a4ec-fb24607824c8";
const TOKEN = "eyJhbGciOiJIUzI1NiJ9.valid-mock.jwt";
const URL = "http://127.0.0.1:8000/";
function assert(condition: unknown, message: string): asserts condition {
  if (!condition) throw Error(message);
}
function backend(allow = true) {
  const calls = { user: 0, created: 0, status: 0 };
  const deps: DeletionBackend = {
    enabled: () => allow,
    async getUser(_t: string) { calls.user++; return { id: UID }; },
    async request(_t: string, _c: string) {
      calls.created++;
      return { request_id: RID, status: "pending", stage: "fenced" };
    },
    async status(_t: string) {
      calls.status++;
      return { request_id: RID, status: "retryable", stage: "fenced" };
    },
  };
  return { deps, calls };
}
function req(method: string, data?: unknown, options: Record<string, string> = {}): Request {
  const body = data === undefined ? undefined : JSON.stringify(data);
  return new Request(URL, {
    method,
    headers: {
      Authorization: "Bearer " + TOKEN,
      ...(body ? { "Content-Type": "application/json", "Content-Length": String(new TextEncoder().encode(body).length) } : {}),
      ...options,
    },
    body,
  });
}
Deno.test("default-disabled blocks even authenticated requests without contacting backend", async () => {
  const { deps, calls } = backend(false);
  const response = await handleRequest(req("POST", { confirmation: CONFIRM }), deps);
  assert(response.status === 503, "Enabled accidentally");
  assert(calls.user === 0 && calls.created === 0, "Disabled flow contacted backend");
  assert((await response.json()).error === "deletion_unavailable", "Error code");
});
Deno.test("missing bearer is denied before Auth lookup", async () => {
  const b = backend();
  const response = await handleRequest(req("GET", undefined, { Authorization: "" }), b.deps);
  assert(response.status === 401 && b.calls.user === 0, "Unauthenticated allowed");
});
Deno.test("service-role style unsupported token cannot bypass verified Auth result", async () => {
  const b = backend();
  b.deps.getUser = async () => null;
  assert((await handleRequest(req("POST", { confirmation: CONFIRM }), b.deps)).status === 401, "Unverified bearer");
  assert(b.calls.created === 0, "Unauthorized request persisted");
});
Deno.test("only explicit confirmation accepted", async () => {
  const b = backend();
  for (const confirmation of ["DELETE", CONFIRM.toLowerCase(), ""]) {
    const response = await handleRequest(req("POST", { confirmation }), b.deps);
    assert(response.status === 422, "Invalid destructive confirmation accepted");
  }
  assert(b.calls.created === 0, "Request recorded without confirmation");
});
Deno.test("client may never supply user_id or arbitrary request ID", async () => {
  const b = backend();
  for (const payload of [
    { confirmation: CONFIRM, user_id: UID },
    { confirmation: CONFIRM, request_id: RID },
    { confirmation: CONFIRM, account_id: UID },
  ]) {
    assert((await handleRequest(req("POST", payload), b.deps)).status === 400, "Client ID accepted");
  }
  assert(b.calls.created === 0, "Wrong owner API called");
});
Deno.test("legitimate user request returns durable receipt and safe headers", async () => {
  const b = backend();
  const r = await handleRequest(req("POST", { confirmation: CONFIRM }), b.deps);
  const value = await r.json();
  assert(r.status === 202 && value.request_id === RID, "Missing receipt");
  assert(r.headers.get("Cache-Control")?.includes("no-store"), "Cached private receipt");
  assert(!r.headers.has("Access-Control-Allow-Origin"), "Unexpected CORS");
  assert(!r.headers.has("Set-Cookie"), "Session cookie leaked");
  assert(JSON.stringify(value).indexOf(UID) < 0, "Account ID leaked");
  assert(b.calls.created === 1, "Worker called?");
});
Deno.test("status is account-scoped by verified bearer, no client target", async () => {
  const b = backend();
  const r = await handleRequest(req("GET"), b.deps);
  const d = await r.json();
  assert(r.status === 200 && d.status === "retryable", "Status invisible");
  assert(b.calls.status === 1, "Missing status lookup");
});
Deno.test("reject URL queries and unauthorized paths", async () => {
  const b = backend();
  for (const path of [URL + "?user_id=" + UID, URL + "worker", URL + "#frag"]) {
    const r = await handleRequest(new Request(path,{headers:{Authorization:"Bearer "+TOKEN}}),b.deps);
    assert(r.status===404, "Unknown path allowed");
  }
  assert(b.calls.created===0,"Unknown path used worker");
});
Deno.test("browser origin and unsafe methods denied", async () => {
  const b=backend();
  assert((await handleRequest(req("POST",{confirmation:CONFIRM},{Origin:"https://evil.example"}),b.deps)).status===403,"CORS accepted");
  assert((await handleRequest(req("DELETE"),b.deps)).status===405,"Worker delete exposed");
  assert((await handleRequest(req("OPTIONS"),b.deps)).status===405,"Options bypass");
});
Deno.test("oversized JSON content length rejected", async () => {
  const b=backend();
  const r=await handleRequest(req("POST",{confirmation:CONFIRM},{ "Content-Length":"257" }),b.deps);
  assert(r.status===413 && b.calls.created===0,"Large body accepted");
});
Deno.test("malformed JSON and duplicate confirmation keys rejected", async () => {
  const b=backend();
  const raw='{"confirmation":"DELETE MY BACKLOT ACCOUNT","confirmation":"NO"}';
  const r=await handleRequest(new Request(URL,{
    method:"POST",
    headers:{Authorization:"Bearer "+TOKEN,"Content-Type":"application/json",
      "Content-Length":String(raw.length)},
    body:raw,
  }),b.deps);
  assert(r.status===400 && b.calls.created===0,"Duplicate JSON keys accepted");
});
Deno.test("upstream failure returns generic error not user content", async () => {
  const b=backend();
  b.deps.request=async ()=>{throw Error("secret-token-and-private-email")};
  const r=await handleRequest(req("POST",{confirmation:CONFIRM}),b.deps);
  assert(r.status===503,"Unexpected internal error status");
  assert(!(await r.text()).includes("secret-token"),"Internal error leaked");
});
