-- Whether court staff recorded, on issuing the order, that it gives permission to issue a warrant or writ of restitution.
ALTER TABLE orders ADD COLUMN restitution_permission boolean;
