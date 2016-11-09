package ru.curs.flute;

import ru.curs.flute.JDBCConnectionPool.DBType;

public class MSSQLPollerTest extends AbstractSQLTablePollerTest {
	@Override
	protected DBType getDBType() {
		return DBType.MSSQLServer;
	}

}
