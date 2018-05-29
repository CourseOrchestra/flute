package ru.curs.flute.source;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.curs.celesta.dbutils.BLOB;
import ru.curs.flute.task.FluteTask;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.JDBCConnectionPool;
import ru.curs.flute.task.QueueTask;
import ru.curs.flute.task.TaskUnit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by ioann on 02.08.2017.
 */
@Component
@Scope("prototype")
public class SqlTablePoller extends QueueSource {

  private static final int DEFAULT_QUERY_PERIOD = 5000;

  @Autowired
  private JDBCConnectionPool pool;

  private PreparedStatement selectNextStmt;
  private PreparedStatement markNextStmt;
  private Connection mainConn;

  private String tableName;
  private int queryPeriod = DEFAULT_QUERY_PERIOD;
  private int errorTextMaxLength;

  private String getSelectNextStmt() {
    switch (pool.getDBType()) {
      case MSSQLServer:
        return "SELECT TOP 1 \"id\", \"script\", \"type\", \"parameters\" FROM %s WHERE \"status\" = 0 ORDER BY \"id\"";
      case Oracle:
        return "SELECT * FROM (SELECT \"id\", \"script\", \"type\", \"parameters\" FROM %s WHERE \"status\" = 0 ORDER BY \"id\") WHERE ROWNUM <=1";
      case PostgreSQL:
      case H2:
      default:
        return "SELECT \"id\", \"script\", \"type\", \"parameters\" FROM %s WHERE \"status\" = 0 ORDER BY \"id\" LIMIT 1";
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
  public QueueTask getTask() throws EFluteCritical, InterruptedException {
    init();
    while (true) {
      try (ResultSet rs = selectNextStmt.executeQuery()) {
        if (rs.next()) {
          int id = rs.getInt(1);
          String qualifier = rs.getString(2);
          String type = rs.getString(3);
          String params = rs.getString(4);
          markNextStmt.setInt(1, id);
          markNextStmt.executeUpdate();
          return new QueueTask(this, id, new TaskUnit(qualifier, TaskUnit.Type.valueOf(type)), params);
        }
      } catch (SQLException e) {
        throw new EFluteCritical("Error during getting the next task data: " + e.getMessage());
      } finally {
        pool.commit(mainConn);
      }
      Thread.sleep(queryPeriod);
    }
  }

  @Override
  public void changeTaskState(FluteTask task) {

    int newState;

    switch (task.getState()) {
      case SUCCESS:
        newState = 2;
        break;
      case FAIL:
        newState = 3;
        break;
      case INTERRUPTED:
        newState = 4;
        break;
      default:
        return;
    }

    try {
      PreparedStatement finalizeTaskStmt = mainConn.prepareStatement(String.format(
          "UPDATE %s SET \"status\" = ?, \"result\" = ?, \"errortext\" = ? WHERE \"id\" = ?", tableName));

      finalizeTaskStmt.setInt(1, newState);
      BLOB blob = task.getBLOB();
      if (blob.isNull()) {
        finalizeTaskStmt.setNull(2, java.sql.Types.NULL);
      } else {
        finalizeTaskStmt.setBinaryStream(2, blob.getInStream(), blob.size());
      }
      int limit = errorTextMaxLength;
      if (limit > 0 && task.getMessage().length() > limit) {
        finalizeTaskStmt.setString(3, task.getMessage().substring(0, limit));
      } else {
        finalizeTaskStmt.setString(3, task.getMessage());
      }

      finalizeTaskStmt.setInt(4, task.getId());
      finalizeTaskStmt.executeUpdate();
    } catch (Exception e) {
      System.out.printf("System could not finalize task %s.%d properly%n", this.getTableName(), task.getId());
      e.printStackTrace();
    }
  }

}
