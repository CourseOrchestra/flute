package ru.curs.flute.source;

import it.sauronsoftware.cron4j.Scheduler;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.curs.flute.task.FluteTask;
import ru.curs.flute.task.QueueTask;
import ru.curs.flute.task.TaskUnit;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ioann on 02.08.2017.
 */
@Component
@Scope("prototype")
public class ScheduledTaskSupplier extends QueueSource implements HasTaskUnit {
  private final Scheduler scheduler = new Scheduler();

  private final BlockingQueue<QueueTask> queue = new LinkedBlockingQueue<>();

  private TaskUnit taskUnit;
  private String params;
  private String schedule;

  private String taskId = null;

  public void setSchedule(String schedule) {
    this.schedule = schedule;
    if (taskId == null) {
      taskId = scheduler.schedule(schedule, () -> {
        try {
          QueueTask t = new QueueTask(this, 0, taskUnit, params);
          internalAdd(t);
        } catch (Exception e) {
          // this will not happen
        }
      });
    } else {
      scheduler.reschedule(taskId, schedule);
    }
  }

  public void internalAdd(QueueTask t) throws InterruptedException {
    queue.put(t);
  }

  @Override
  public QueueTask getTask() throws InterruptedException {
    try {
      return queue.take();
    } catch (InterruptedException e) {
      if (taskId != null)
        scheduler.deschedule(taskId);
      throw e;
    }
  }

  public String getSchedule() {
    return schedule;
  }

  @Override
  public void run() {
    scheduler.start();
    super.run();
  }

  public void setParams(String params) {
    this.params = params;
  }

  public TaskUnit getTaskUnit() {
    return this.taskUnit;
  }

  public void setTaskUnit(TaskUnit taskUnit) {
    if (this.taskUnit == null)
      this.taskUnit = taskUnit;
    else
      throw new RuntimeException("TaskUnit already exists");
  }

  public String getParams() {
    return params;
  }

  @Override
  public void changeTaskState(FluteTask t) { }
}
