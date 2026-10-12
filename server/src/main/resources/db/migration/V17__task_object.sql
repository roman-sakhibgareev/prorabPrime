-- A task may belong to an object; it goes with the object when that is deleted for good.
ALTER TABLE tasks ADD COLUMN object_id UUID REFERENCES objects (id) ON DELETE CASCADE;

CREATE INDEX tasks_object_idx ON tasks (object_id);
