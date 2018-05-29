package ru.curs.flute.task;

import ru.curs.flute.source.QueueSource;

/**
 * Created by ioann on 02.08.2017.
 */
public class QueueTask extends FluteTask {

  private final QueueSource ts;
  
  public QueueTask(QueueSource ts, int id, TaskUnit taskUnit, String params) {
    super(ts, id, taskUnit, params);
    this.ts = ts;
  }

  @Override
  public void setState(FluteTaskState newState) {
    super.setState(newState);
    ts.changeTaskState(this);
  }
}
