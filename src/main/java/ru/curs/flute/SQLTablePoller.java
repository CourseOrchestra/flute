package ru.curs.flute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The task source that polls SQL table on a regular basis.
 */
@Component
@Scope("prototype")
class SQLTablePoller extends TaskSource {

	@Autowired
	private JDBCConnectionPool pool;

	public JDBCConnectionPool getPool() {
		return pool;
	}

	private String tableName;
	private int queryPeriod;
	private int errorTextMaxLength;

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
	FluteTask getTask() {
		// TODO Auto-generated method stub
		return null;
	}

}
