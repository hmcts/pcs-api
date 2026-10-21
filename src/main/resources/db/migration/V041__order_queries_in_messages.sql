-- Court staff's query on an order they return is in the order's message thread, not a case note.
ALTER TABLE order_steps DROP COLUMN note_id;
