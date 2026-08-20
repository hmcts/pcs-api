ALTER TABLE hearing
  ADD COLUMN cancelled BOOLEAN,
  ADD COLUMN cancellation_reason VARCHAR(500);

