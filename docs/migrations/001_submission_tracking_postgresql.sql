-- PostgreSQL / Supabase: review and run against a restored backup first.
-- Additive migration from the current IntelliTrack schema. No sample rows are deleted.
-- Stop the backend while migrating. Keep its previous deployment available for rollback.
BEGIN;

ALTER TABLE class_sections ADD COLUMN IF NOT EXISTS academic_term varchar(255);
ALTER TABLE project_groups ADD COLUMN IF NOT EXISTS academic_term varchar(255);
ALTER TABLE project_groups ADD COLUMN IF NOT EXISTS previous_group_id bigint;
ALTER TABLE submission_versions ADD COLUMN IF NOT EXISTS google_doc_url varchar(1000);

CREATE TABLE IF NOT EXISTS google_connections (
    id varchar(255) PRIMARY KEY,
    user_id bigint,
    purpose varchar(255),
    state_hash varchar(255),
    state_expires_at timestamptz,
    encrypted_verifier text,
    encrypted_refresh_token text,
    google_email varchar(255),
    calendar_id varchar(255),
    folder_id varchar(255),
    version bigint
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_google_connection_state ON google_connections(state_hash);
-- Backend uses its privileged JDBC connection; no browser Supabase access is needed.
ALTER TABLE google_connections ENABLE ROW LEVEL SECURITY;

-- Old Hibernate constraints may have generated names. Drop only the two old
-- deliverable uniqueness definitions, retaining all unrelated constraints.
DO $$
DECLARE constraint_row record;
BEGIN
    FOR constraint_row IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.conrelid='deliverables'::regclass AND c.contype='u'
        AND (SELECT array_agg(a.attname::text ORDER BY a.attname)
             FROM unnest(c.conkey) k(attnum)
             JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=k.attnum)
            IN (ARRAY['name','subject_id']::text[], ARRAY['class_section_id','name']::text[])
    LOOP
        EXECUTE format('ALTER TABLE deliverables DROP CONSTRAINT %I',constraint_row.conname);
    END LOOP;
    IF EXISTS (SELECT 1 FROM deliverables WHERE academic_term IS NOT NULL
               GROUP BY name,subject_id,academic_term HAVING count(*)>1) THEN
        RAISE EXCEPTION 'Duplicate named requirements in a subject/term. Reconcile them before migrating; no rows were deleted.';
    END IF;
    IF EXISTS (SELECT 1 FROM submissions WHERE student_id IS NOT NULL
               GROUP BY student_id,group_id,deliverable_id HAVING count(*)>1) THEN
        RAISE EXCEPTION 'Duplicate student/group/requirement submissions. Preserve history and reconcile before migrating.';
    END IF;
    IF EXISTS (SELECT 1 FROM project_group_students GROUP BY enrollment_id HAVING count(*)>1) THEN
        RAISE EXCEPTION 'An enrollment belongs to multiple groups. Review membership before migrating.';
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uk_deliverable_subject_term ON deliverables(name,subject_id,academic_term);
CREATE UNIQUE INDEX IF NOT EXISTS ux_submission_student_requirement ON submissions(student_id,group_id,deliverable_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_group_enrollment ON project_group_students(enrollment_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_group_carry ON project_groups(previous_group_id,subject_id,academic_term);
CREATE INDEX IF NOT EXISTS ix_section_subject_term ON class_sections(subject_id,academic_term);

-- Terms deliberately remain NULL until the coordinator assigns them. Do not guess
-- which school year old samples belong to. No timestamp conversion is performed.
COMMIT;
