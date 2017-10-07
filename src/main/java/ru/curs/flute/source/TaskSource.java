package ru.curs.flute.source;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import redis.clients.jedis.JedisPool;
import ru.curs.celesta.Celesta;
import ru.curs.celesta.CelestaException;
import ru.curs.flute.task.FluteTask;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.exception.EFluteNonCritical;
import ru.curs.flute.GlobalParams;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Created by ioann on 02.08.2017.
 */
public abstract class TaskSource implements Runnable {

  static final int DEFAULT_MAX_THREADS = 4;

  @Autowired
  GlobalParams params;

  @Autowired
  private Celesta celesta;

  @Autowired
  private ApplicationContext ctx;

  
  private String finalizer;
  private final String id = UUID.randomUUID().toString();
  private Optional<JedisPool> jedisPool = Optional.empty();

  public abstract FluteTask getTask() throws InterruptedException, EFluteCritical;

  public abstract void release();

  public String getId() {
    return id;
  }


  public void setFinalizer(String finalizer) {
    this.finalizer = finalizer;
  }

  public String getFinalizer() {
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
      String threadName = String.format("%08X-%s", getId().hashCode(), task.getScript());
      Thread.currentThread().setName(threadName);
      // ]
      String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
      celesta.login(sesId, params.getFluteUserId());
      celesta.runPython(sesId, task.getScript(), task);
      celesta.logout(sesId, false);
    } catch (CelestaException e) {
      throw new EFluteNonCritical(String.format("Celesta execution error: %s", e.getMessage()));
    }
  }

  public void tearDown() {
    if (finalizer != null) {
      try {
        String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
        FluteTask task = new FluteTask(this, 0, finalizer, null);
        celesta.login(sesId, params.getFluteUserId());
        celesta.runPython(sesId, finalizer, task);
        celesta.logout(sesId, false);
      } catch (CelestaException e) {
        System.out.printf("Celesta execution error during finalization: %s%n", e.getMessage());
      }
    }
  }
}
