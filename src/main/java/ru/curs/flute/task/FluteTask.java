package ru.curs.flute.task;

import ru.curs.celesta.dbutils.BLOB;
import ru.curs.flute.exception.EFluteNonCritical;
import ru.curs.flute.source.TaskSource;

import java.io.OutputStream;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Encapsulates Flute task in/out parameters.
 */
public class FluteTask implements Runnable {
  //TODO::Runnable вынести во вложенный класс TaskRunner

  private final TaskSource ts;
  private final TaskUnit taskUnit;
  private final String params;
  private final int id;

  private Throwable error;
  private FluteTaskState state = FluteTaskState.NEW;
  private String message;

  private final BLOB blob = new BLOB();

  public FluteTask(TaskSource ts, int id, TaskUnit taskUnit, String params) {
    this.ts = ts;
    this.id = id;
    this.taskUnit = taskUnit;
    this.params = params;
  }

  public TaskSource getSource() {
    return ts;
  }

  public String getSourceId() {
    return ts.getId();
  }

  public FluteTaskState getState() {
    return state;
  }

  public void setState(FluteTaskState newState) {
    state = newState;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public TaskUnit getTaskUnit() {
    return this.taskUnit;
  }

  public String getParams() {
    return params;
  }

  public int getId() {
    return id;
  }

  public OutputStream getResultstream() {
    return blob.getOutStream();
  }

  public BLOB getBLOB() {
    return blob;
  }

  public Throwable getError() {
    return error;
  }

  protected void doJob() throws InterruptedException, EFluteNonCritical {
	  ts.process(this);
  }
  
  public Jedis getJedis() {
	  return ts.getJedisPool().map(JedisPool::getResource).orElse(null);
  }
  
  public JedisPool getJedisPool(){
	  return ts.getJedisPool().orElse(null);
  }

  @Override
  public void run() {
    setState(FluteTaskState.INPROCESS);
    try {
      doJob();
      setState(FluteTaskState.SUCCESS);
    } catch (InterruptedException e) {
      setState(FluteTaskState.INTERRUPTED);
    } catch (Throwable e) {
      setState(FluteTaskState.FAIL);
      error = e;
      message = e.getMessage();
      System.err.printf("Task failed: %s%n", e.getMessage());
    } finally {
      ts.release();
    }
  }
}
