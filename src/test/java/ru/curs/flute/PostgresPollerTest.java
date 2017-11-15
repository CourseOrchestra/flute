package ru.curs.flute;

import ru.curs.flute.JDBCConnectionPool.DBType;

public class PostgresPollerTest extends AbstractSQLTablePollerTest {
	@Override
	protected DBType getDBType() {
		return DBType.PostgreSQL;
	}

}
