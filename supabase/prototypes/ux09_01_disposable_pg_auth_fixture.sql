-- UX09-01 LOCAL/DISPOSABLE PostgreSQL 16 TEST FIXTURE ONLY.
-- NEVER apply to hosted Backlot Lab. This intentionally stubs Supabase Auth.
-- Only use a throwaway database; identifiers below are synthetic and public.
\set ON_ERROR_STOP on

do $roles$
begin
  if not exists (select 1 from pg_roles where rolname='anon') then
    create role anon nologin;
  end if;
  if not exists (select 1 from pg_roles where rolname='authenticated') then
    create role authenticated nologin;
  end if;
end;
$roles$;

create schema auth;
create table auth.users (id uuid primary key);
insert into auth.users (id) values
('11111111-1111-4111-8111-111111111111'),
('22222222-2222-4222-8222-222222222222');

-- Simulates a locally verified JWT subject ONLY under a throwaway Postgres
-- role. This is NOT a signed real Supabase JWT, GoTrue, or production proof.
create function auth.uid() returns uuid
language sql stable security invoker set search_path = ''
as $mock$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $mock$;
create function auth.jwt() returns jsonb
language sql stable security invoker set search_path = ''
as $mock$
  select pg_catalog.jsonb_build_object('is_anonymous',
    coalesce(current_setting('request.jwt.is_anonymous', true), 'false')::boolean)
$mock$;

revoke all on schema auth from public;
grant usage on schema auth to authenticated;
grant execute on function auth.uid() to authenticated;
grant execute on function auth.jwt() to authenticated;
grant usage on schema public to authenticated, anon;

-- NO credential fixtures: each test session SET ROLE authenticated and sets
-- synthetic request.jwt.claim.sub. Real A/B Auth and RLS API tests are later.
