package ru.curs.flute;

import java.sql.Connection;

public interface JDBCConnectionPool {

	enum DBType {
		// these types are equal to H2 compatibility modes
		PostgreSQL, MSSQLServer, Oracle, H2
	}

	Connection get() throws Exception;

	void putBack(Connection conn);

	void commit(Connection conn);

	DBType getDBType();
}
