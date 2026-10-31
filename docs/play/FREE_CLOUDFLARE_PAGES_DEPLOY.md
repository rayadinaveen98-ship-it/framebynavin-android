# Backlot — free Cloudflare Pages hosting (DRAFT, not live)

The user selected **Cloudflare Pages Free**, no custom domain or recurring hosting charge required to serve this static site. This repository branch stages a completely disabled privacy/account-deletion preview. Do NOT submit it to Play Console as a functional deletion page.

### Owner setup (no secrets shared in chat)
1. Sign in to your own Cloudflare account, open **Workers & Pages → Create → Pages → Connect to Git** (menu wording may differ). Connect your Backlot GitHub repository.
2. Select the reviewed preview branch **`feature/backlot-play-free-pages-static-preview-build272`**, if using a preview environment; DO NOT make draft/disabled site the public production compliance URL yet.
3. Framework preset: None. Build command: blank. Build output directory: **`release-site`**. Root directory: repository root.
4. Preview the generated `*.pages.dev` domain and review the privacy, deletion-status and secure-request pages. The pages include `noindex,nofollow` and visible draft disclaimers.
5. Before **public production**: approve a developer support email/contact, final privacy disclosures, data retention rules and a genuine external deletion request path. Google Play permits a staffed support-email link or request form provided users can actually request deletion without reinstalling, and all account-associated data are removed: https://support.google.com/googleplay/android-developer/answer/13327111
6. Only after verifying the actual request handling and deletion/receipt process, review removing disabled markers/noindex, update in-app privacy link, publish the real site to a dedicated production branch, and use that verified URL in Play Console.

### Security and cost
The site is static and the PKCE account deletion request controls are **disabled**, without production OAuth configuration or service-role credentials. Static preview itself should not require a paid hosting plan, but account/plan limits remain Cloudflare-defined. No Supabase Edge Function was deployed in this task. Do not turn on the web sign-in without reviewing Google OAuth redirect origins and fenced data deletion end to end.

Official Google Play requirements: https://support.google.com/googleplay/android-developer/answer/13327111
