package ru.curs.flute;

import ru.curs.flute.JDBCConnectionPool.DBType;

public class OraclePollerTest extends AbstractSQLTablePollerTest {
	@Override
	protected DBType getDBType() {
		return DBType.Oracle;
	}

}
