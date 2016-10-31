package ru.curs.flute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import ru.curs.celesta.Celesta;
import ru.curs.flute.EFluteCritical;
import ru.curs.flute.EFluteNonCritical;
import ru.curs.flute.FluteTask;
import ru.curs.flute.GlobalParams;
import ru.curs.flute.RedisQueue;

public class RedisQueueTest {
	private static ApplicationContext ctx;

	@BeforeClass
	public static void setup() {
		ctx = new AnnotationConfigApplicationContext(TestConf.class);
	}

	@Test
	public void test1() throws InterruptedException, ExecutionException, EFluteCritical {
		JedisPool jp = ctx.getBean(JedisPool.class);
		try (Jedis j = jp.getResource()) {
			j.del("fookey");
			j.lpush("fookey", "{script:a, params:b}");
			j.lpush("fookey", "{script:b, params:c}");
			assertEquals(2, j.llen("fookey").intValue());

			RedisQueue q = ctx.getBean(RedisQueue.class);

			q.setMaxThreads(2);
			q.setQueueName("fookey");

			FluteTask t = q.getTask();
			assertEquals("a", t.getScript());
			assertEquals("b", t.getParams());
			assertSame(q, t.getSource());

			t = q.getTask();
			assertEquals("b", t.getScript());
			assertEquals("c", t.getParams());
			assertSame(q, t.getSource());

			CompletableFuture<FluteTask> f = CompletableFuture.supplyAsync(() -> {
				try {
					return q.getTask();
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			});
			Thread.sleep(10);

			assertFalse(f.isDone());

			j.lpush("fookey", "{script:cccc, params:dddd}");

			t = f.get();
			assertEquals("cccc", t.getScript());
			assertEquals("dddd", t.getParams());
			assertSame(q, t.getSource());

			j.lpush("fookey", "{s{");
			j.lpush("fookey", "{s:123}");
			f = CompletableFuture.supplyAsync(() -> {
				try {
					return q.getTask();
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			});
			j.lpush("fookey", "{script:cccc, params:dddd}");
			t = f.get();
			assertEquals("cccc", t.getScript());
			assertEquals("dddd", t.getParams());
			assertSame(q, t.getSource());

		}

	}

	@Test
	public void jsontest() throws EFluteNonCritical {
		FluteTask t = new FluteTask(null, "foo.bar", "param1");
		assertEquals("{\"script\":\"foo.bar\",\"params\":\"param1\"}", RedisQueue.toJSON(t));

		RedisQueue q = ctx.getBean(RedisQueue.class);

		t = q.fromJSON("{script: \"other.script\",\n\"params\": \"other parameter\"}");
		assertEquals("other.script", t.getScript());
		assertEquals("other parameter", t.getParams());
		assertSame(q, t.getSource());

		boolean itWas = false;
		try {
			q.fromJSON("{s{");
		} catch (EFluteNonCritical e) {
			itWas = true;
		}
		assertTrue(itWas);

		itWas = false;
		try {
			q.fromJSON("{s:123}");
		} catch (EFluteNonCritical e) {
			itWas = true;
		}
		assertTrue(itWas);

		itWas = false;
		try {
			q.fromJSON("{\"script\":[1,2],\"params\":\"param1\"}");
		} catch (EFluteNonCritical e) {
			itWas = true;
		}
		assertTrue(itWas);

		itWas = false;
		try {
			q.fromJSON("[{\"script\":[1,2],\"params\":\"param1\"}]");
		} catch (EFluteNonCritical e) {
			itWas = true;
		}
		assertTrue(itWas);
	}

}

@Configuration
@Import(RedisQueue.class)
class TestConf {
	@Bean
	public JedisPool getJedisPool() {
		// use localhost
		JedisPool p = new JedisPool();
		return p;
	}

	@Bean
	public GlobalParams getGlobalParams() {
		return new GlobalParams() {
			@Override
			public int getRetryWait() {
				return 0;
			}

			@Override
			public boolean isNeverStop() {
				return true;
			}

			@Override
			public String getFluteUserId() {
				return "flute";
			}

		};
	}

	@Bean
	public Celesta getCelesta() {
		return null;
	}

}
