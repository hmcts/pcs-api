-- Defence pack progresses partially-sent (defence posted, counter-claim pending) to sent (complete).
ALTER TYPE claim_activity_type ADD VALUE IF NOT EXISTS 'DEFENCE_PACK_PARTIALLY_SENT';
ALTER TYPE claim_activity_type ADD VALUE IF NOT EXISTS 'DEFENCE_PACK_SENT';
