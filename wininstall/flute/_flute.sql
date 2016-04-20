CREATE GRAIN flute VERSION '1.30';

CREATE TABLE tasks(
    id INT NOT NULL IDENTITY PRIMARY KEY,
    script VARCHAR(250) NOT NULL,
    parameters TEXT,
    status INT NOT NULL DEFAULT 0,
    result BLOB,
    errortext TEXT) with no version check;

CREATE INDEX idxtasks ON tasks (status);

CREATE TABLE crontable(
    id INT NOT NULL IDENTITY PRIMARY KEY,
    cronschedule VARCHAR(250) NOT NULL,
    script VARCHAR(250) NOT NULL,
    disabled BIT NOT NULL DEFAULT 'FALSE') with no version check;
