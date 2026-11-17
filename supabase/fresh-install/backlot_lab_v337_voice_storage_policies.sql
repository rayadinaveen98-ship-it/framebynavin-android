-- Backlot Lab V337, private Creator Voice Idea media policies
-- Applies only to newly created Backlot Lab. Bucket created separately through Supabase Storage API/dashboard.
-- Object paths: <authenticated user uuid>/<idea id>/media/<SHA-256>.m4a
-- No anonymous access, no delete privileges, no shared folders.
CREATE POLICY backlot_lab_voice_read_own ON storage.objects
  FOR SELECT TO authenticated
  USING (bucket_id = 'creator-voice-ideas' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text);
CREATE POLICY backlot_lab_voice_upload_own ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'creator-voice-ideas' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text);
CREATE POLICY backlot_lab_voice_update_own ON storage.objects
  FOR UPDATE TO authenticated
  USING (bucket_id = 'creator-voice-ideas' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text)
  WITH CHECK (bucket_id = 'creator-voice-ideas' AND (storage.foldername(name))[1] = (SELECT auth.uid())::text);
