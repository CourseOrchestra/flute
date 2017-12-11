package ru.curs.flute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import redis.clients.jedis.JedisPool;
import ru.curs.celesta.Celesta;
import ru.curs.flute.conf.CommonParameters;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.source.*;

public class TaskSourcesTest {
	private static ApplicationContext ctx;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ctx = new AnnotationConfigApplicationContext(TestBeansProvider.class, CommonParameters.class,
				TaskSources.class);
	}

	@Test
	public void testTaskSources() {
		TaskSources ts = ctx.getBean(TaskSources.class);
		List<TaskSource> ss = ts.getSources();
		assertEquals(6, ss.size());

		String id0 = ss.get(0).getId();
		String id1 = ss.get(1).getId();
		String id2 = ss.get(3).getId();
		assertNotNull(id0);
		assertNotNull(id1);
		assertNotNull(id2);
		assertFalse(id0.equals(id1));
		assertFalse(id1.equals(id2));
		assertFalse(id2.equals(id0));
		assertEquals(id0, ss.get(0).getId());
		assertEquals(id1, ss.get(1).getId());
		assertEquals(id2, ss.get(3).getId());

		assertEquals("flute.tasks", ((SqlTablePoller) ss.get(0)).getTableName());
		assertEquals(6000, ((SqlTablePoller) ss.get(0)).getQueryPeriod());
		assertEquals(10000, ((SqlTablePoller) ss.get(0)).getTerminationTimeout());
		assertEquals(4, ((SqlTablePoller) ss.get(0)).getMaxThreads());
		assertEquals("foo.bar.baz", ((SqlTablePoller) ss.get(0)).getFinalizer());

		assertEquals("q1", ((RedisQueue) ss.get(1)).getQueueName());

		assertEquals("q2", ((RedisQueue) ss.get(2)).getQueueName());

		assertEquals("5 * * * *", ((ScheduledTaskSupplier) ss.get(3)).getSchedule());
		assertEquals("foo.module.script", ((ScheduledTaskSupplier) ss.get(3)).getScript());
		assertEquals("234", ((ScheduledTaskSupplier) ss.get(3)).getParams());

		for (int i = 4; i < 6; i++) {
			assertEquals("foo.hello.run", ((LoopTaskSupplier) ss.get(i)).getScript());
			assertEquals(1000, ((LoopTaskSupplier) ss.get(i)).getWaitOnSuccess());
			assertEquals(30000, ((LoopTaskSupplier) ss.get(i)).getWaitOnFailure());
			assertEquals("foo.hello.stop", ((LoopTaskSupplier) ss.get(i)).getFinalizer());
		}

	}

}

@Configuration
class TestBeansProvider {
	@Bean("confSource")
	@Scope("prototype")
	static InputStream getConfInputStream() throws EFluteCritical {
		return TestBeansProvider.class.getResourceAsStream("testsetup.xml");
	}

	@Bean
	@Scope("singleton")
	public GlobalParams getGlobalParams(final @Autowired CommonParameters params) {
		return new GlobalParams() {
			@Override
			public int getRetryWait() {
				return params.getRetryWait();
			}

			@Override
			public boolean isNeverStop() {
				return params.isNeverStop();
			}

			@Override
			public String getFluteUserId() {
				return "flute";
			}

			@Override
			public boolean isExposeRedis() {
				return false;
			}

		};
	}

	@Bean
	@Scope("singleton")
	public JDBCConnectionPool getJDBCConnectionPool() {
		return null;
	}

	@Bean
	@Scope("singleton")
	public JedisPool getJedisPool() {
		return null;
	}

	@Bean
	@Scope("singleton")
	public Celesta getCelesta() {
		return null;
	}
}
