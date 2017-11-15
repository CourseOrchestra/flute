CREATE GRAIN g VERSION '1.30';

CREATE TABLE table1(
    id INT NOT NULL IDENTITY PRIMARY KEY,
    script VARCHAR(250) NOT NULL,
    parameters TEXT,
    status INT NOT NULL DEFAULT 0,
    result BLOB,
    errortext TEXT) with no version check;

CREATE INDEX idxtable1 ON table1 (status);

CREATE TABLE table2(
    id INT NOT NULL IDENTITY PRIMARY KEY,
    cronschedule VARCHAR(250) NOT NULL,
    script VARCHAR(250) NOT NULL,
    disabled BIT NOT NULL DEFAULT 'FALSE') with no version check;
