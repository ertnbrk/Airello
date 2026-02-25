-- =====================================================
-- V13: Fix sprint.status VARCHAR -> ENUM migration
-- =====================================================

-- 1. ENUM oluştur (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type
        WHERE typname = 'sprint_status'
    ) THEN
CREATE TYPE sprint_status AS ENUM (
            'PLANNED',
            'ACTIVE',
            'DONE'
        );
END IF;
END
$$;

-- 2. Eski VARCHAR DEFAULT'u kaldır
ALTER TABLE sprint
    ALTER COLUMN status DROP DEFAULT;

-- 3. Kolon tipini ENUM'a çevir (mevcut veriyi koruyarak)
ALTER TABLE sprint
ALTER COLUMN status
    TYPE sprint_status
    USING status::sprint_status;

-- 4. ENUM DEFAULT'u açık cast ile geri ekle
ALTER TABLE sprint
    ALTER COLUMN status
        SET DEFAULT 'PLANNED'::sprint_status;
