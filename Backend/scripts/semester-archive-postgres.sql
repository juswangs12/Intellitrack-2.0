-- Local MySQL ddl-auto=update creates this table on backend restart.
CREATE TABLE IF NOT EXISTS semester_archives (
    term varchar(255) PRIMARY KEY,
    archived_at timestamp(6)
);
