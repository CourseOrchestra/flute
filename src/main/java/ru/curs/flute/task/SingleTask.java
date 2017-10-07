package ru.curs.flute.task;

import ru.curs.flute.source.LoopTaskSupplier;

/**
 * Created by ioann on 02.08.2017.
 */
public class SingleTask extends AbstractFluteTask {

  public SingleTask(LoopTaskSupplier ts, int id, String script, String params) {
    super(ts, id, script, params);
  }

}
