-- Add refresh_token column to users table
-- This script adds the refresh_token column to the existing users table

-- Check if the column already exists before adding
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'users' 
        AND column_name = 'refresh_token'
    ) THEN
        ALTER TABLE users ADD COLUMN refresh_token VARCHAR(500);
        RAISE NOTICE 'refresh_token column added to users table';
    ELSE
        RAISE NOTICE 'refresh_token column already exists in users table';
    END IF;
END $$;

