CREATE GRAIN flute VERSION '1.00';

CREATE TABLE tasks(
    id INT NOT NULL IDENTITY PRIMARY KEY,
    script VARCHAR(250) NOT NULL,
    parameters TEXT,
    status INT NOT NULL DEFAULT 0,
    result BLOB,
    errortext TEXT) with no version check;

CREATE INDEX idxtasks ON tasks (status);