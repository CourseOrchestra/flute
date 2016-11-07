package ru.curs.flute;

import java.sql.Connection;

public interface JDBCConnectionPool {

	public enum DBType {
		// these types are equal to H2 compatibility modes
		PostgreSQL, MSSQLServer, Oracle, MySQL
	}

	Connection get() throws Exception;

	void putBack(Connection conn);

	void commit(Connection conn);

	DBType getDBType();
}
