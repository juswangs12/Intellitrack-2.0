-- Apply before deploying to a PostgreSQL environment using ddl-auto=validate.
-- Local MySQL ddl-auto=update creates these additions on restart.
ALTER TABLE student_enrollments ADD COLUMN IF NOT EXISTS progression varchar(255);
ALTER TABLE student_enrollments ADD COLUMN IF NOT EXISTS progression_reason varchar(255);
ALTER TABLE student_enrollments ADD COLUMN IF NOT EXISTS previous_enrollment_id bigint;
CREATE TABLE IF NOT EXISTS enrollment_former_groups (
    enrollment_id bigint NOT NULL REFERENCES student_enrollments(id),
    group_id bigint NOT NULL
);
-- NULL progression is intentionally interpreted as Pending / INC by the application.
