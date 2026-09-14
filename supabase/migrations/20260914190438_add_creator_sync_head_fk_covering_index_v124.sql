-- Stage H release-readiness hardening.
-- Supabase Performance Advisor 0001 flagged the composite foreign key from
-- creator_sync_heads(user_id, revision) to creator_sync_snapshots(user_id, revision)
-- as lacking a covering child-side index. The table is new/empty at migration time,
-- and this changes no row ownership, RLS policy, RPC, or sync semantics.

create index if not exists creator_sync_heads_snapshot_idx
    on public.creator_sync_heads (user_id, revision);
