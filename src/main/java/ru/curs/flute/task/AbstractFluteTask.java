package ru.curs.flute.task;

import ru.curs.celesta.dbutils.BLOB;
import ru.curs.flute.exception.EFluteNonCritical;
import ru.curs.flute.source.TaskSource;

import java.io.OutputStream;

/**
 * Created by ioann on 02.08.2017.
 */
public class AbstractFluteTask<T extends TaskSource> implements Runnable {
  //TODO::Runnable вынести в какой-нибудь TaskExecutor

  private final T ts;
  private final String script;
  private final String params;
  private final int id;

  private Throwable error;
  private FluteTaskState state = FluteTaskState.NEW;
  private String message;

  private final BLOB blob = new BLOB();

  public AbstractFluteTask(T ts, int id, String script, String params) {
    this.ts = ts;
    this.id = id;
    this.script = script;
    this.params = params;
  }

  public T getSource() {
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

  public String getScript() {
    return script;
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
