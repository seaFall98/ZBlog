-- V28: P3 comment root_id backfill
-- Fixes P0-2: comments without root_id are incorrectly listed as root comments.
-- root_id = id for roots; for replies, root_id is resolved from the parent chain.
-- P3 uses a 2-level model (root + direct reply), so two passes are sufficient.

-- Step 1: root comments — their own id is the root.
UPDATE comments SET root_id = id WHERE parent_id IS NULL AND root_id IS NULL;

-- Step 2: direct replies — root_id from the parent (which is a root after step 1).
UPDATE comments c SET root_id = (
  SELECT p.root_id FROM comments p WHERE p.id = c.parent_id
)
WHERE c.parent_id IS NOT NULL AND c.root_id IS NULL
  AND EXISTS (SELECT 1 FROM comments p WHERE p.id = c.parent_id AND p.root_id IS NOT NULL);

-- Step 3: one more pass for any remaining orphaned replies whose parent was also a reply.
-- This covers imported/nested data up to 3 levels deep without needing PL/pgSQL.
UPDATE comments c SET root_id = (
  SELECT p.root_id FROM comments p WHERE p.id = c.parent_id
)
WHERE c.parent_id IS NOT NULL AND c.root_id IS NULL
  AND EXISTS (SELECT 1 FROM comments p WHERE p.id = c.parent_id AND p.root_id IS NOT NULL);
