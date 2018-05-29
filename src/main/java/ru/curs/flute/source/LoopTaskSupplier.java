package ru.curs.flute.source;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.curs.flute.task.SingleTask;
import ru.curs.flute.task.TaskUnit;

/**
 * Created by ioann on 02.08.2017.
 */
@Component
@Scope("prototype")
public class LoopTaskSupplier extends TaskSource implements HasTaskUnit {
  public static final int DEFAULT_WAIT_ON_SUCCESS = 1000;
  public static final int DEFAULT_WAIT_ON_FAILURE = 1000;

  private int waitOnSuccess = DEFAULT_WAIT_ON_SUCCESS;
  private int waitOnFailure = DEFAULT_WAIT_ON_FAILURE;

  private TaskUnit taskUnit;
  private String params;
  private int count = 1;

  @Override
  public SingleTask getTask() throws InterruptedException {
    return new SingleTask(this, 0, taskUnit, params);
  }

  @Override
  public void run() {
    try {
      while (true) {
        SingleTask command = getTask();
        command.run();
        switch (command.getState()) {
          case SUCCESS:
            Thread.sleep(waitOnSuccess);
            break;
          case FAIL:
            Thread.sleep(waitOnFailure);
            break;
          case INTERRUPTED:
          default:
            return;
        }
      }
    } catch (InterruptedException e) {
      return;
    }
  }

  public int getWaitOnSuccess() {
    return waitOnSuccess;
  }

  public int getWaitOnFailure() {
    return waitOnFailure;
  }

  public void setWaitOnSuccess(int waitOnSuccess) {
    this.waitOnSuccess = waitOnSuccess;
  }

  public void setWaitOnFailure(int waitOnFailure) {
    this.waitOnFailure = waitOnFailure;
  }

  public TaskUnit getTaskUnit() {
    return taskUnit;
  }

  public void setTaskUnit(TaskUnit taskUnit) {
    if (this.taskUnit == null)
      this.taskUnit = taskUnit;
    else
      throw new RuntimeException("TaskUnit already exists");
  }

  public void setParams(String params) {
    this.params = params;
  }

  public String getParams() {
    return params;
  }

  @Override
  public void release() {}

  public void setCount(int count) {
    this.count = count;
  }

  int getCount() {
    return count;
  }
}
