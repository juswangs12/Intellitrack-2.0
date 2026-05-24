-- Fix submission status column to support all enum values (up to 50 chars)
ALTER TABLE submissions MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING';

-- Fix revision_count column to have a default value of 0
ALTER TABLE submissions MODIFY COLUMN revision_count INT NOT NULL DEFAULT 0;
