package ru.curs.flute;

import static org.junit.Assert.assertEquals;

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
import ru.curs.flute.CRONTasksSupplier;
import ru.curs.flute.CommonParameters;
import ru.curs.flute.EFluteCritical;
import ru.curs.flute.GlobalParams;
import ru.curs.flute.JDBCConnectionPool;
import ru.curs.flute.RedisQueue;
import ru.curs.flute.SQLTablePoller;
import ru.curs.flute.TaskSource;
import ru.curs.flute.TaskSources;

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
		assertEquals(4, ss.size());

		assertEquals("flute.tasks", ((SQLTablePoller) ss.get(0)).getTableName());
		assertEquals(5000, ((SQLTablePoller) ss.get(0)).getQueryPeriod());
		assertEquals(10000, ((SQLTablePoller) ss.get(0)).getTerminationTimeout());

		assertEquals("q1", ((RedisQueue) ss.get(1)).getQueueName());

		assertEquals("q2", ((RedisQueue) ss.get(2)).getQueueName());

		assertEquals("5 * * * *", ((CRONTasksSupplier) ss.get(3)).getSchedule());
		assertEquals("foo.module.script", ((CRONTasksSupplier) ss.get(3)).getScript());
		assertEquals("234", ((CRONTasksSupplier) ss.get(3)).getParams());

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
