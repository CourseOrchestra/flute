package ru.curs.flute.source;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.exception.EFluteNonCritical;
import ru.curs.flute.task.AbstractFluteTask;
import ru.curs.flute.task.FluteTaskState;
import ru.curs.flute.task.QueueTask;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by ioann on 02.08.2017.
 */
public abstract class QueueSource extends TaskSource {
  static final int DEFAULT_TERMINATION_TIMEOUT = 4000;
  static final int INTERRUPTION_CHECK_PERIOD = 10;
  private int maxThreads = DEFAULT_MAX_THREADS;


  private static ThreadLocal<JsonParser> jp = ThreadLocal.withInitial(JsonParser::new);

  private final ResizeableSemaphore semaphore = new ResizeableSemaphore();

  private int terminationTimeout = DEFAULT_TERMINATION_TIMEOUT;


  public abstract void changeTaskState(AbstractFluteTask t);

  @Override
  public void run() {
    ExecutorService threads = Executors.newCachedThreadPool();
    retrycycle: while (true) {

      try {
        while (true) {
          semaphore.acquire();
          AbstractFluteTask command = getTask();
          threads.execute(command);
        }
      } catch (InterruptedException e) {
        try {
          threads.shutdown();
          threads.awaitTermination(terminationTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e1) {
          // do nothing, contintue shutdown
        }
        // send termination signal to all still executing tasks
        List<Runnable> waitingTasks = threads.shutdownNow();
        waitingTasks.forEach((t) -> {
          ((QueueTask) t).setState(FluteTaskState.INTERRUPTED);
        });
        break retrycycle;
      } catch (EFluteCritical e) {
        e.printStackTrace();
        System.out.printf("Task source %s stopped execution on critical error (see stderr for details).%n",
            this.toString());
        if (params == null || !params.isNeverStop()) {
          break retrycycle;
        } else if (params.getRetryWait() > 0) {
          try {
            System.out.printf("Restarting in %d milliseconds...%n", params.getRetryWait());
            Thread.sleep(params.getRetryWait());
          } catch (InterruptedException e1) {
            // do nothing, return
            break retrycycle;
          }
          System.out.println("done.");
        }
      }
    }
  }

  public static String toJSON(QueueTask t) {
    JsonObject o = new JsonObject();
    o.addProperty("script", t.getScript());
    o.addProperty("params", t.getParams());
    return o.toString();
  }

  public QueueTask fromJSON(String string) throws EFluteNonCritical {
    JsonObject o;
    try {
      JsonElement e = jp.get().parse(string);
      o = e.getAsJsonObject();
    } catch (RuntimeException e) {
      throw new EFluteNonCritical("Message parsing error: " + e.getMessage());
    }

    JsonElement script = o.get("script");
    if (script == null)
      throw new EFluteNonCritical(String.format("No script value found in message '%s'", string));

    JsonElement params = o.get("params");

    JsonElement idElement = o.get("id");
    int id;
    if (idElement == null)
      id = 0;
    else {
      try {
        id = Integer.parseInt(idElement.getAsString());
      } catch (Exception e) {
        id = 0;
      }
    }

    try {
      String taskParams;
      if (params == null || params.isJsonNull()) {
        taskParams = null;
      } else if (params.isJsonPrimitive()) {
        taskParams = params.getAsString();
      } else {
        taskParams = params.toString();
      }
      QueueTask result = new QueueTask(this, id, script.getAsString(), taskParams);
      return result;
    } catch (RuntimeException e) {
      throw new EFluteNonCritical(String.format("Message parse error: script and params should be strings."));
    }
  }

  @Override
  public void release() {
    semaphore.release();
  }

  private static final class ResizeableSemaphore extends Semaphore {
    private static final long serialVersionUID = 1L;

    /**
     * Create a new semaphore with 0 permits.
     */
    ResizeableSemaphore() {
      super(DEFAULT_MAX_THREADS);
    }

    @Override
    protected void reducePermits(int reduction) {
      super.reducePermits(reduction);
    }
  }

  public synchronized void setMaxThreads(int newMax) {
    if (newMax < 1) {
      throw new IllegalArgumentException("Maximum threads number must be at least 1," + " was " + newMax);
    }

    int delta = newMax - this.maxThreads;

    if (delta == 0) {
      return;
    } else if (delta > 0) {
      this.semaphore.release(delta);
    } else {
      this.semaphore.reducePermits(-delta);
    }

    this.maxThreads = newMax;
  }

  public int getMaxThreads() {
    return maxThreads;
  }

  public int getTerminationTimeout() {
    return terminationTimeout;
  }

  public void setTerminationTimeout(int terminationTimeout) {
    this.terminationTimeout = terminationTimeout;
  }

}
