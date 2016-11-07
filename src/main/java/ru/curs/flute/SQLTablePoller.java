package ru.curs.flute;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The task source that polls SQL table on a regular basis.
 */
@Component
@Scope("prototype")
class SQLTablePoller extends TaskSource {

	private static final int DEFAULT_QUERY_PERIOD = 5000;

	@Autowired
	private JDBCConnectionPool pool;

	private PreparedStatement selectNextStmt;
	private PreparedStatement markNextStmt;
	private Connection mainConn;

	public JDBCConnectionPool getPool() {
		return pool;
	}

	private String tableName;
	private int queryPeriod = DEFAULT_QUERY_PERIOD;
	private int errorTextMaxLength;

	private String getSelectNextStmt() {
		switch (pool.getDBType()) {
		case MSSQLServer:
			return "SELECT TOP 1 \"id\", \"script\", \"parameters\" FROM %s WHERE \"status\" = 0 ORDER BY \"id\"";
		case Oracle:
			return "SELECT * FROM (SELECT \"id\", \"script\", \"parameters\" FROM %s WHERE \"status\" = 0 ORDER BY \"id\") WHERE ROWNUM <=1";
		case PostgreSQL:
		case MySQL:
		default:
			return "SELECT \"id\", \"script\", \"parameters\" FROM %s WHERE \"status\" = 0 ORDER BY \"id\" LIMIT 1";
		}
	}

	private void init() throws EFluteCritical {
		if (tableName == null)
			throw new EFluteCritical("No table name for SQL Table Poller specified.");
		try {
			if (mainConn == null || mainConn.isClosed()) {
				mainConn = pool.get();
				selectNextStmt = mainConn.prepareStatement(String.format(getSelectNextStmt(), tableName));
				markNextStmt = mainConn
						.prepareStatement(String.format("UPDATE %s SET \"status\" = 1 WHERE \"id\" = ?", tableName));
			}
		} catch (Exception e) {
			throw new EFluteCritical(e.getMessage());
		}
	}

	public int getQueryPeriod() {
		return queryPeriod;
	}

	public void setQueryPeriod(int queryPeriod) {
		this.queryPeriod = queryPeriod;
	}

	public int getErrorTextMaxLength() {
		return errorTextMaxLength;
	}

	public void setErrorTextMaxLength(int errorTextMaxLength) {
		this.errorTextMaxLength = errorTextMaxLength;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	@Override
	FluteTask getTask() throws EFluteCritical, InterruptedException {
		init();
		while (true) {
			try (ResultSet rs = selectNextStmt.executeQuery()) {
				if (rs.next()) {
					int id = rs.getInt(1);
					String script = rs.getString(2);
					String params = rs.getString(3);
					markNextStmt.setInt(1, id);
					markNextStmt.executeUpdate();
					return new FluteTask(this, id, script, params);
				}
			} catch (SQLException e) {
				throw new EFluteCritical("Error during getting the next task data: " + e.getMessage());
			}
			Thread.sleep(queryPeriod);
		}
	}

}
