package ru.curs.flute.task;

import ru.curs.flute.source.QueueSource;

/**
 * Created by ioann on 02.08.2017.
 */
public class QueueTask extends AbstractFluteTask<QueueSource> {

  public QueueTask(QueueSource ts, int id, String script, String params) {
    super(ts, id, script, params);
  }

  @Override
  public void setState(FluteTaskState newState) {
    super.setState(newState);
    getSource().changeTaskState(this);
  }
}
