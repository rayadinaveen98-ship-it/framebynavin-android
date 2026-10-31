import test from "node:test";
import assert from "node:assert/strict";
import { webcrypto } from "node:crypto";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import {
  CONFIRM, STORAGE_KEY, REQUEST_PATH, reviewConfiguration, createChallenge,
  googleLoginUrl, initialCallback, validSavedPkce, parseReceipt, parseUser, requestHeaders,
} from "../assets/backlot-deletion-core.mjs";
import { config } from "../assets/backlot-deletion-config.mjs";

const site = "https://privacy.example.org";
const api = "https://kukkqgpzxnfanynbddiw.supabase.co";
const key = "sb_publishable_" + "A".repeat(32);
const accepted = { enabled: true, approvedOrigin: site, supabaseUrl: api, publishableKey: key };
const uuid = "21234567-89ab-4cde-8f01-123456789abc";
const receipt = {request_id:uuid,status:"pending",stage:"fenced"};
const source = readFileSync(fileURLToPath(new URL("../assets/backlot-deletion-web.mjs", import.meta.url)), "utf8");
const html = readFileSync(fileURLToPath(new URL("../deletion-request/index.html",import.meta.url)), "utf8");
const configSource = readFileSync(fileURLToPath(new URL("../assets/backlot-deletion-config.mjs", import.meta.url)), "utf8");

test("shipping configuration is explicitly disabled with no website origin or publishable key", () => {
  assert.equal(config.enabled,false);
  assert.equal(config.approvedOrigin,"");
  assert.equal(config.publishableKey,"");
  assert.equal(reviewConfiguration(config,site+REQUEST_PATH).ready,false);
  assert.ok(configSource.includes("enabled: false"));
  assert.ok(!source.includes("service_role"));
  assert.ok(!source.includes("localStorage"));
  assert.ok(!source.includes("document.cookie"));
  assert.ok(source.includes("if (!approved.ready)"));
});
test("strict exact-origin, HTTPS, path and API configuration", () => {
  assert.equal(reviewConfiguration(accepted,site+REQUEST_PATH).ready,true);
  assert.equal(reviewConfiguration(accepted,site+REQUEST_PATH+"?code=placeholder").ready,true);
  for(const url of [
    "http://privacy.example.org/deletion-request/",
    "https://privacy.example.org.evil.invalid/deletion-request/",
    site+"/delete-account/",
    site+"/deletion-request",
    "https://not-approved.example.org/deletion-request/",
  ]) {
    assert.equal(reviewConfiguration(accepted,url).ready,false,url);
  }
  for(const unsafe of [
    {...accepted,enabled:false},
    {...accepted,approvedOrigin:"https://not-approved.example.org"},
    {...accepted,publishableKey:"service_role_secret"},
    {...accepted,supabaseUrl:"http://evil.invalid"},
    {...accepted,supabaseUrl:"https://kukkqgpzxnfanynbddiw.supabase.co/evil"},
  ]) {
    assert.equal(reviewConfiguration(unsafe,site+REQUEST_PATH).ready,false);
  }
});
test("PKCE verifier/challenge are cryptographically linked using SHA256", async () => {
  const {verifier,challenge}=await createChallenge(webcrypto);
  assert.match(verifier,/^[A-Za-z0-9_-]{64}$/);
  assert.match(challenge,/^[A-Za-z0-9_-]{43}$/);
  const hash=new Uint8Array(await webcrypto.subtle.digest("SHA-256",new TextEncoder().encode(verifier)));
  const actual=Buffer.from(hash).toString("base64url");
  assert.equal(challenge,actual);
  assert.notEqual(verifier,challenge);
});
test("Google sign-in URL has PKCE S256 and exact redirect; no credentials in URL", () => {
  const challenge="x".repeat(43);
  const u=new URL(googleLoginUrl(api,site+REQUEST_PATH,challenge));
  assert.equal(u.origin,api);
  assert.equal(u.pathname,"/auth/v1/authorize");
  assert.equal(u.searchParams.get("provider"),"google");
  assert.equal(u.searchParams.get("redirect_to"),site+REQUEST_PATH);
  assert.equal(u.searchParams.get("code_challenge"),challenge);
  assert.equal(u.searchParams.get("code_challenge_method"),"s256");
  assert.equal(u.searchParams.has("service_role"),false);
  assert.throws(()=>googleLoginUrl(api,site+REQUEST_PATH,"weak"),/PKCE/);
});
test("callback rejects duplicate malformed or provider-error codes", () => {
  assert.deepEqual(initialCallback(""),{code:null});
  assert.deepEqual(initialCallback("?code=abc12345"),{code:"abc12345"});
  for(const value of ["?code=abc12345&code=def12345","?code=x","?error=access_denied","?code=%3Cscript%3E123"]) {
    assert.deepEqual(initialCallback(value),{error:true},value);
  }
});
test("PKCE challenge lives in same-tab sessionStorage for <=5 minutes only", () => {
  const target=site+REQUEST_PATH;
  const now=1000000;
  const saved={v:1,verifier:"z".repeat(64),timestamp:now-40000,redirect:target};
  assert.equal(validSavedPkce(saved,target,now),saved.verifier);
  assert.equal(validSavedPkce(saved,target,now+400000),null);
  assert.equal(validSavedPkce(saved,"https://wrong.example/deletion-request/",now),null);
  assert.equal(validSavedPkce({...saved,verifier:"<script>"},target,now),null);
  assert.equal(validSavedPkce({...saved,timestamp:now+10},target,now),null);
  assert.equal(validSavedPkce(null,target,now),null);
  assert.equal(STORAGE_KEY,"backlot-web-pkce-v1");
});
test("strict receipt states, schema and absence of false success", () => {
  assert.deepEqual(parseReceipt({status:"none"},true),null);
  assert.deepEqual(parseReceipt(receipt),{requestId:uuid,status:"pending",stage:"fenced"});
  assert.deepEqual(parseReceipt({...receipt,status:"complete",stage:"complete"}).status,"complete");
  for(const payload of [
    {status:"none"}, {...receipt,status:"deleted"}, {...receipt,stage:"unknown"},
    {...receipt,request_id:"invalid"}, {...receipt,user_id:uuid}, [],
  ]) assert.throws(()=>parseReceipt(payload),/receipt/);
});
test("authenticated owner identity and user bearer have strict shape", () => {
  assert.deepEqual(parseUser({id:uuid,email:"creator@example.org"}),{id:uuid,email:"creator@example.org"});
  assert.throws(()=>parseUser({id:"fake",email:"creator@example.org"}),/user/);
  assert.throws(()=>parseUser({id:uuid}),/user/);
  const headers=requestHeaders(key,"a".repeat(80),true);
  assert.equal(headers.Authorization,"Bearer "+"a".repeat(80));
  assert.equal(headers.apikey,key);
  assert.equal(headers["Content-Type"],"application/json");
  assert.equal(headers["Cache-Control"],"no-store");
  assert.throws(()=>requestHeaders(key,"bad"),/session/);
});
test("site uses only first-party resources and never claims deletion is live", () => {
  assert.ok(html.includes('data-deletion-request="disabled-preview"'));
  assert.ok(html.includes('name="robots" content="noindex,nofollow"'));
  assert.ok(html.includes("not available"));
  assert.ok(html.includes('type="module" src="../assets/backlot-deletion-web.mjs"'));
  assert.ok(html.includes('id="request-confirm-dialog"'));
  assert.ok(html.includes('id="request-signin" disabled'));
  assert.ok(html.includes('id="request-review" disabled'));
  assert.ok(html.includes('id="request-submit" type="button" disabled'));
  assert.ok(html.includes("Personal Reminders"));
  assert.ok(html.includes("YouTube channel"));
  assert.ok(!html.includes("https://cdn."));
  assert.equal(CONFIRM,"DELETE MY BACKLOT ACCOUNT");
  assert.ok(source.includes("credentials: \"omit\""));
  assert.ok(source.includes("sessionStorage.removeItem(STORAGE_KEY)"));
  assert.ok(source.includes('phrase.value === CONFIRM'));
  assert.ok(source.includes("window.history.replaceState"));
});
