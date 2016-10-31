package ru.curs.flute;

import java.sql.Connection;

public interface JDBCConnectionPool {
	Connection get() throws Exception;

	void putBack(Connection conn);

	void commit(Connection conn);
}
