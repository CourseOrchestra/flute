package ru.curs.flute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import redis.clients.jedis.JedisPool;
import ru.curs.celesta.vintage.Celesta;
import ru.curs.flute.conf.CommonParameters;
import ru.curs.flute.source.*;
import ru.curs.flute.task.TaskUnit;

public class TaskSourcesTest {
    private ApplicationContext ctx;

    @Test
    public void testTaskSources() {
        this.ctx = new AnnotationConfigApplicationContext(TestBeansProvider.class, GoodConfSourceConfig.class, CommonParameters.class,
                TaskSources.class);
        TaskSources ts = ctx.getBean(TaskSources.class);
        List<TaskSource> ss = ts.getSources();
        assertEquals(10, ss.size());

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
        assertEquals("foo.bar.baz", ss.get(0).getFinalizer().getQualifier());
        assertEquals(TaskUnit.Type.SCRIPT, ss.get(0).getFinalizer().getType());

        assertEquals("q1", ((RedisQueue) ss.get(1)).getQueueName());

        assertEquals("q2", ((RedisQueue) ss.get(2)).getQueueName());

        assertEquals("5 * * * *", ((ScheduledTaskSupplier) ss.get(3)).getSchedule());
        assertEquals("foo.module.script", ((ScheduledTaskSupplier) ss.get(3)).getTaskUnit().getQualifier());
        assertEquals(TaskUnit.Type.SCRIPT, ((ScheduledTaskSupplier) ss.get(3)).getTaskUnit().getType());
        assertEquals("234", ((ScheduledTaskSupplier) ss.get(3)).getParams());

        assertEquals("5 * * * *", ((ScheduledTaskSupplier) ss.get(4)).getSchedule());
        assertEquals("ru.curs.flute.Foo#foo", ((ScheduledTaskSupplier) ss.get(4)).getTaskUnit().getQualifier());
        assertEquals(TaskUnit.Type.PROC, ((ScheduledTaskSupplier) ss.get(4)).getTaskUnit().getType());
        assertEquals("234", ((ScheduledTaskSupplier) ss.get(4)).getParams());

        for (int i = 5; i < 7; i++) {
            assertEquals("foo.hello.run", ((LoopTaskSupplier) ss.get(i)).getTaskUnit().getQualifier());
            assertEquals(TaskUnit.Type.SCRIPT, ((LoopTaskSupplier) ss.get(i)).getTaskUnit().getType());
            assertEquals(1000, ((LoopTaskSupplier) ss.get(i)).getWaitOnSuccess());
            assertEquals(30000, ((LoopTaskSupplier) ss.get(i)).getWaitOnFailure());
            assertEquals("foo.hello.stop", (ss.get(i)).getFinalizer().getQualifier());
        }

        for (int i = 7; i < 10; i++) {
            assertEquals("ru.curs.flute.Hello#run", ((LoopTaskSupplier) ss.get(i)).getTaskUnit().getQualifier());
            assertEquals(TaskUnit.Type.PROC, ((LoopTaskSupplier) ss.get(i)).getTaskUnit().getType());
            assertEquals(1000, ((LoopTaskSupplier) ss.get(i)).getWaitOnSuccess());
            assertEquals(30000, ((LoopTaskSupplier) ss.get(i)).getWaitOnFailure());
            assertEquals("ru.curs.flute.Hello#stop", (ss.get(i)).getFinalizer().getQualifier());
        }
    }

    @Test(expected = BeanCreationException.class)
    public void testFailWhenFinalizerTypeIsDifferentFromTaskUnit() {
        this.ctx = new AnnotationConfigApplicationContext(TestBeansProvider.class, BadConfSourceConfig.class, CommonParameters.class,
                TaskSources.class);
    }
}


@Configuration
class GoodConfSourceConfig {
    @Bean("confSource")
    @Scope("prototype")
    static InputStream getConfInputStream() {
        return TestBeansProvider.class.getResourceAsStream("testsetup.xml");
    }
}

@Configuration
class BadConfSourceConfig {
    @Bean("confSource")
    @Scope("prototype")
    static InputStream getConfInputStream() {
        return TestBeansProvider.class.getResourceAsStream("badTestSetup.xml");
    }
}

@Configuration
class TestBeansProvider {


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
    public JDBCConnectionPool getJDBCConnectionPool() {
        return null;
    }

    @Bean
    public JedisPool getJedisPool() {
        return null;
    }

    @Bean
    public Celesta getCelesta() {
        return null;
    }
}
