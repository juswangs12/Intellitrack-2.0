-- Fix submission status column to support all enum values (up to 50 chars)
ALTER TABLE submissions MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING';

-- Fix revision_count column to have a default value of 0
ALTER TABLE submissions MODIFY COLUMN revision_count INT NOT NULL DEFAULT 0;

-- Drop old unique constraints from deliverables to allow same name for different subjects/sections
ALTER TABLE deliverables DROP INDEX IF EXISTS UKhoca4jutxpuf9y6mo34uav5q6;
ALTER TABLE deliverables DROP INDEX IF EXISTS UK_name_subject_id;
ALTER TABLE deliverables DROP INDEX IF EXISTS UK_name_class_section_id;

-- Add subject-aware unique constraints to deliverables
ALTER TABLE deliverables ADD CONSTRAINT IF NOT EXISTS UK_deliverable_name_subject 
    UNIQUE (name, subject_id);


-- Add subject_id column to project_groups to bind groups to subjects
ALTER TABLE project_groups ADD COLUMN IF NOT EXISTS subject_id BIGINT;

-- Add foreign key constraint for project_groups.subject_id
ALTER TABLE project_groups ADD CONSTRAINT IF NOT EXISTS fk_project_group_subject 
    FOREIGN KEY (subject_id) REFERENCES subjects(id);
