-- LEGACY DATABASE ONLY. Do not run against the new Supabase production database.
-- This is a manual repair, never an automatic deployment migration.
-- Run in Supabase SQL Editor as postgres while the backend is stopped.
-- User authorized deletion of the old test submissions.
-- Deletes ALL submission rows and rows in FK-dependent tables (versions,
-- participation, comments/evaluations referencing submissions). Does not reset
-- accounts, subjects, groups or enrollments. It does not delete stored files.
BEGIN;
SET LOCAL lock_timeout = '10s';
TRUNCATE TABLE public.submissions CONTINUE IDENTITY CASCADE;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS student_id bigint;
ALTER TABLE public.submissions ALTER COLUMN student_id SET NOT NULL;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='public.submissions'::regclass
    AND contype='f' AND conkey=ARRAY[(SELECT attnum FROM pg_attribute
      WHERE attrelid='public.submissions'::regclass AND attname='student_id')]::smallint[]) THEN
    ALTER TABLE public.submissions ADD CONSTRAINT fk_submission_student
      FOREIGN KEY(student_id) REFERENCES public.users(id);
  END IF;
END $$;
COMMIT;
SELECT count(*) AS remaining_submissions FROM public.submissions;
