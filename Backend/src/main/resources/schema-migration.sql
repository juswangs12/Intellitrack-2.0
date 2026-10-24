-- Local MySQL only: runs AFTER Hibernate creates/updates tables and columns.
-- Preserve rows and fail startup on unresolved schema/data conflicts.
ALTER TABLE submissions MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING';
ALTER TABLE submissions MODIFY COLUMN revision_count INT NOT NULL DEFAULT 0;

-- Establish the replacement before removing obsolete constraints.
SET @tracking_has_index = (
    SELECT COUNT(*) FROM (
        SELECT index_name FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'deliverables' AND non_unique = 0
        GROUP BY index_name
        HAVING GROUP_CONCAT(column_name ORDER BY column_name) = 'academic_term,name,subject_id'
    ) AS matching_indexes
);
SET @tracking_ddl = IF(@tracking_has_index > 0, 'SELECT 1',
    'ALTER TABLE deliverables ADD CONSTRAINT uk_deliverable_subject_term UNIQUE (name,subject_id,academic_term)');
PREPARE tracking_statement FROM @tracking_ddl;
EXECUTE tracking_statement;
DEALLOCATE PREPARE tracking_statement;

-- Keep an index supporting the section foreign key before dropping an old
-- (class_section_id,name) unique index that may be its only supporting index.
SET @tracking_has_section_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'deliverables'
      AND column_name = 'class_section_id' AND seq_in_index = 1 AND non_unique = 1
);
SET @tracking_ddl = IF(@tracking_has_section_index > 0, 'SELECT 1',
    'ALTER TABLE deliverables ADD INDEX ix_tracking_deliverable_section (class_section_id)');
PREPARE tracking_statement FROM @tracking_ddl;
EXECUTE tracking_statement;
DEALLOCATE PREPARE tracking_statement;

-- Match definitions, not generated names; retain every unrelated index.
SET @tracking_old_indexes = (
    SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', REPLACE(index_name, '`', '``'), '`') SEPARATOR ', ')
    FROM (
        SELECT index_name FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'deliverables'
          AND non_unique = 0 AND index_name <> 'PRIMARY'
        GROUP BY index_name
        HAVING GROUP_CONCAT(column_name ORDER BY column_name)
            IN ('name,subject_id', 'class_section_id,name')
    ) AS old_indexes
);
SET @tracking_ddl = IF(@tracking_old_indexes IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE deliverables ', @tracking_old_indexes));
PREPARE tracking_statement FROM @tracking_ddl;
EXECUTE tracking_statement;
DEALLOCATE PREPARE tracking_statement;


-- Session revocation after password changes. Existing accounts start at version zero.
SET @tracking_ddl = IF(EXISTS (
  SELECT 1 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'session_version'
), 'SELECT 1', 'ALTER TABLE users ADD COLUMN session_version BIGINT NULL DEFAULT 0');
PREPARE tracking_statement FROM @tracking_ddl;
EXECUTE tracking_statement;
DEALLOCATE PREPARE tracking_statement;
