package ru.curs.flute.source;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import ru.curs.flute.task.AbstractFluteTask;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.exception.EFluteNonCritical;
import ru.curs.flute.task.QueueTask;

import java.util.List;

/**
 * Created by ioann on 02.08.2017.
 */
@Component
@Scope("prototype")
public class RedisQueue extends QueueSource {

  @Autowired
  private JedisPool pool;

  String queueName;

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public Object getQueueName() {
    return queueName;
  }

  @Override
  public void changeTaskState(AbstractFluteTask t) {}

  @Override
  public QueueTask getTask() throws EFluteCritical, InterruptedException {
    QueueTask result;
    while (true) {
      List<String> val;
      try (Jedis j = pool.getResource()) {
        // val = j.brpop(0, queueName);
        while ((val = j.brpop(INTERRUPTION_CHECK_PERIOD, queueName)) == null || val.isEmpty())
          if (Thread.interrupted()) {
            System.out.println("Shutting down Redis queue.");
            throw new InterruptedException();
          }
      } catch (JedisException e) {
        throw new EFluteCritical("Redis error: " + e.getMessage());
      }
      try {
        result = fromJSON(val.get(1));
        break;
      } catch (EFluteNonCritical e) {
        System.err.printf("Message from Redis queue '%s' skipped: %s%n", queueName, e.getMessage());
      }
    }
    return result;
  }

  public JedisPool getPool() {
    return pool;
  }
}
