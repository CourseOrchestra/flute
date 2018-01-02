package ru.curs.flute;

import org.junit.Ignore;
import ru.curs.flute.JDBCConnectionPool.DBType;

@Ignore
public class OraclePollerTest extends AbstractSQLTablePollerTest {
	@Override
	protected DBType getDBType() {
		return DBType.Oracle;
	}

}
