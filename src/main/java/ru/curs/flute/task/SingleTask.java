package ru.curs.flute.task;

import ru.curs.flute.source.LoopTaskSupplier;

/**
 * Created by ioann on 02.08.2017.
 */
public class SingleTask extends FluteTask {

  public SingleTask(LoopTaskSupplier ts, int id, TaskUnit taskUnit, String params) {
    super(ts, id, taskUnit, params);
  }

}
