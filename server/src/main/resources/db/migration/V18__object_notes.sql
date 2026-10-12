-- The log of an object: dated lines of text written on the phone.
CREATE TABLE object_notes (
    id         UUID PRIMARY KEY,
    object_id  UUID          NOT NULL REFERENCES objects (id) ON DELETE CASCADE,
    text       VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL
);

CREATE INDEX object_notes_object_idx ON object_notes (object_id, created_at);
