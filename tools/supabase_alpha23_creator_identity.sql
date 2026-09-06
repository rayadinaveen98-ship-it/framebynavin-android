-- FrameByNavin Alpha23 Creator Identity
-- Applied to Supabase project kukkqgpzxnfanynbddiw on 2026-09-06.
alter table public.creator_profiles
  add column if not exists username text,
  add column if not exists username_normalized text;

create unique index if not exists creator_profiles_username_normalized_uq
  on public.creator_profiles (username_normalized)
  where username_normalized is not null;

create or replace function public.claim_creator_username(p_username text, p_display_name text default null)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
  v_user_id uuid := auth.uid();
  v_username text := lower(trim(coalesce(p_username, '')));
  v_display_name text := nullif(trim(coalesce(p_display_name, '')), '');
  v_row public.creator_profiles%rowtype;
begin
  if v_user_id is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;
  if length(v_username) < 3 or length(v_username) > 24 or v_username !~ '^[a-z0-9_]+$' then
    raise exception 'Username must be 3-24 characters using letters, numbers or underscore' using errcode = '22023';
  end if;
  if v_username in ('admin','administrator','framebynavin','support','help','official','system','moderator','null','undefined') then
    raise exception 'That username is reserved' using errcode = '22023';
  end if;
  begin
    insert into public.creator_profiles (user_id, display_name, username, username_normalized, updated_at)
    values (v_user_id, v_display_name, v_username, v_username, now())
    on conflict (user_id) do update
      set display_name = coalesce(v_display_name, public.creator_profiles.display_name),
          username = v_username,
          username_normalized = v_username,
          updated_at = now()
    returning * into v_row;
  exception when unique_violation then
    raise exception 'Username is already taken' using errcode = '23505';
  end;
  return jsonb_build_object(
    'user_id', v_row.user_id,
    'display_name', coalesce(v_row.display_name, ''),
    'username', coalesce(v_row.username, ''),
    'avatar_url', coalesce(v_row.avatar_url, ''),
    'created_at', v_row.created_at,
    'updated_at', v_row.updated_at
  );
end;
$$;

revoke all on function public.claim_creator_username(text, text) from public;
revoke all on function public.claim_creator_username(text, text) from anon;
grant execute on function public.claim_creator_username(text, text) to authenticated;
