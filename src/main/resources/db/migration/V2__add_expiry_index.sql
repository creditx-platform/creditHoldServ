-- Add index for efficient hold expiry queries
-- This index supports queries on STATUS and EXPIRES_AT columns for fast lookup of expired holds
CREATE INDEX IDX_CHS_HOLDS_STATUS_EXPIRES_AT ON CHS_HOLDS(STATUS, EXPIRES_AT);
