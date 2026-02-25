-- Convert plan column from PostgreSQL ENUM to VARCHAR

-- Step 1: Add temporary varchar column
ALTER TABLE app_user ADD COLUMN plan_temp VARCHAR(32);

-- Step 2: Copy existing data to temp column (cast enum to text)
UPDATE app_user SET plan_temp = plan::text;

-- Step 3: Drop old enum column
ALTER TABLE app_user DROP COLUMN plan;

-- Step 4: Rename temp column to plan
ALTER TABLE app_user RENAME COLUMN plan_temp TO plan;

-- Step 5: Set NOT NULL constraint and default value
ALTER TABLE app_user ALTER COLUMN plan SET NOT NULL;
ALTER TABLE app_user ALTER COLUMN plan SET DEFAULT 'FREE';

-- Add comment
COMMENT ON COLUMN app_user.plan IS 'User subscription plan: FREE, PRO, or ENTERPRISE';
