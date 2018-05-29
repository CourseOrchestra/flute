package ru.curs.flute.source;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import redis.clients.jedis.JedisPool;
import ru.curs.celesta.CelestaException;
import ru.curs.celesta.vintage.Celesta;
import ru.curs.flute.task.FluteTask;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.exception.EFluteNonCritical;
import ru.curs.flute.GlobalParams;
import ru.curs.flute.task.TaskUnit;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by ioann on 02.08.2017.
 */
public abstract class TaskSource implements Runnable {

    static final int DEFAULT_MAX_THREADS = 4;

    private static final Map<TaskUnit.Type, FluteTaskProcessor> TASK_PROCESSORS;
    private static final Map<TaskUnit.Type, FluteTaskFinalizer> TASK_FINALIZERS;

    static {
        Map<TaskUnit.Type, FluteTaskProcessor> processors = new HashMap<>();
        processors.put(TaskUnit.Type.SCRIPT, TaskSource::processScript);
        processors.put(TaskUnit.Type.PROC, TaskSource::processProc);
        TASK_PROCESSORS = Collections.unmodifiableMap(processors);

        Map<TaskUnit.Type, FluteTaskFinalizer> finalizers = new HashMap<>();
        finalizers.put(TaskUnit.Type.SCRIPT, TaskSource::tearDownScript);
        finalizers.put(TaskUnit.Type.PROC, TaskSource::tearDownProc);
        TASK_FINALIZERS = Collections.unmodifiableMap(finalizers);
    }

  @Autowired
  GlobalParams params;

  @Autowired
  private Celesta celesta;

  @Autowired
  private ApplicationContext ctx;


  private TaskUnit finalizer;
  private final String id = UUID.randomUUID().toString();
  private Optional<JedisPool> jedisPool = Optional.empty();

  public abstract FluteTask getTask() throws InterruptedException, EFluteCritical;

  public abstract void release();

  public String getId() {
    return id;
  }


  public void setFinalizer(TaskUnit finalizer) {
    this.finalizer = finalizer;
  }

  public TaskUnit getFinalizer() {
    return finalizer;
  }


  public Optional<JedisPool> getJedisPool() {
	if (!jedisPool.isPresent() && params.isExposeRedis())
		jedisPool = Optional.of(ctx.getBean(JedisPool.class));
	return jedisPool;
  }

  public void process(FluteTask task) throws InterruptedException, EFluteNonCritical {
    try {
      // [Jedis problem debug
      String threadName = String.format("%08X-%s", getId().hashCode(), task.getTaskUnit().getQualifier());
      Thread.currentThread().setName(threadName);
      // ]
      String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
      TASK_PROCESSORS.get(task.getTaskUnit().getType())
              .process(this.celesta, sesId, task, this.params);
    } catch (CelestaException e) {
      task.setMessage(e.getMessage());
      throw new EFluteNonCritical(String.format("Celesta execution error: %s", e.getMessage()));
    }
  }

  public void tearDown() {
    if (finalizer != null) {
      try {
        String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
        TASK_FINALIZERS.get(this.finalizer.getType())
                .tearDown(this.celesta, sesId, this, this.finalizer, this.params);
      } catch (CelestaException e) {
        System.out.printf("Celesta execution error during finalization: %s%n", e.getMessage());
      }
    }
  }

  private static void processScript(Celesta celesta, String sessionId, FluteTask task, GlobalParams params) {
    celesta.login(sessionId, params.getFluteUserId());
    celesta.runPython(sessionId, task.getTaskUnit().getQualifier(), task);
    celesta.logout(sessionId, false);
  }

  private static void processProc(Celesta celesta, String sessionId, FluteTask task, GlobalParams params) {
    celesta.javaLogin(sessionId, params.getFluteUserId());
    celesta.runProc(sessionId, task.getTaskUnit().getQualifier(), task);
    celesta.javaLogout(sessionId, false);
  }

  private static void tearDownScript(Celesta celesta, String sessionId, TaskSource currentTask, TaskUnit finalizer, GlobalParams params) {
    FluteTask task = new FluteTask(currentTask, 0, finalizer, null);
    celesta.login(sessionId, params.getFluteUserId());
    celesta.runPython(sessionId, finalizer.getQualifier(), task);
    celesta.logout(sessionId, false);
  }

  private static void tearDownProc(Celesta celesta, String sessionId, TaskSource currentTask, TaskUnit finalizer, GlobalParams params) {
    FluteTask task = new FluteTask(currentTask, 0, finalizer, null);
    celesta.javaLogin(sessionId, params.getFluteUserId());
    celesta.runProc(sessionId, finalizer.getQualifier(), task);
    celesta.javaLogout(sessionId, false);
  }

  @FunctionalInterface
  interface FluteTaskProcessor {
    void process(Celesta celesta, String sessionId, FluteTask task, GlobalParams params);
  }

  @FunctionalInterface
  interface FluteTaskFinalizer {
    void tearDown(Celesta celesta, String sessionId, TaskSource currentTask, TaskUnit finalizer, GlobalParams params);
  }
}
