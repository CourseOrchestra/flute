CREATE TABLE IF NOT EXISTS "tasks" (
    "id" INT NOT NULL PRIMARY KEY,
    "script" VARCHAR(250) NOT NULL,
    "type" VARCHAR(6) NOT NULL DEFAULT 'SCRIPT',
    "parameters" TEXT,
    "status" INT NOT NULL DEFAULT 0,
    "result" BLOB,
    "errortext" TEXT);

CREATE INDEX IF NOT EXISTS "idxtasks" ON "tasks" ("status");
