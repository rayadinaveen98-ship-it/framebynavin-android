-- FrameByNavin v2.0 storage simplification.
-- Supabase remains authentication + Creator ID/profile infrastructure only.
-- Private creator work is local-first and backed up to Google Drive appDataFolder.

-- The old generation fence must not gate Creator ID/profile writes.
drop trigger if exists creator_profiles_write_epoch on public.creator_profiles;

-- Retired server-owned backup/lifecycle RPC surface.
drop function if exists public.creator_cloud_status();
drop function if exists public.creator_delete_owned_data(bigint);
drop function if exists public.creator_download_backup(uuid);
drop function if exists public.creator_restore_points();
drop function if exists public.creator_resume_owned_data(bigint);
drop function if exists public.save_creator_backup(
  text,
  text,
  integer,
  text,
  timestamp with time zone,
  date,
  text,
  text,
  integer,
  integer,
  integer,
  integer
);

-- Retired server-owned creator-data tables.
drop table if exists public.creator_backups;
drop table if exists public.creator_devices;
drop table if exists public.creator_cloud_lifecycle;

-- No longer needed after all generation-fenced creator-data writes are removed.
drop function if exists public.creator_enforce_write_epoch();

-- Intentionally retained:
--   public.creator_profiles
--   public.claim_creator_username(text, text)
--   public.touch_updated_at()
--   creator_profiles_touch_updated_at trigger
--   creator_profiles own-row RLS policies
