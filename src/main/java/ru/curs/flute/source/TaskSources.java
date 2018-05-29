package ru.curs.flute.source;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.XMLParamsParser;
import ru.curs.flute.task.TaskUnit;

@Component
@Import({
        SqlTablePoller.class, RedisQueue.class,
        ScheduledTaskSupplier.class, LoopTaskSupplier.class
})
public class TaskSources extends XMLParamsParser {
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("([A-Za-z][A-Za-z0-9]*)(\\.[A-Za-z_]\\w*)+");
    private static final Pattern PROC_PATTERN = Pattern.compile("([A-Za-z]\\w*)(\\.[A-Za-z]\\w*)+(\\$[A-Za-z]\\w*)*#([A-Za-z]\\w*)");

    private final List<TaskSource> tsList = new ArrayList<>();
    private final Map<String, Runnable> startActions = new HashMap<>();
    private final Map<String, Consumer<String>> textActions = new HashMap<>();
    private final ApplicationContext ctx;

    private TaskSource currentSource;

    TaskSources(@Autowired @Qualifier("confSource") InputStream is, final @Autowired ApplicationContext ctx)
            throws EFluteCritical {
        this.ctx = ctx;
        startActions.put("dbtable", () -> {
            currentSource = ctx.getBean(SqlTablePoller.class);
            tsList.add(currentSource);
            initTextActions();
            textActions.put("tablename", ((SqlTablePoller) currentSource)::setTableName);
            textActions.put("pollingperiod", s -> {
                processInt(s, "pollingperiod", true, ((SqlTablePoller) currentSource)::setQueryPeriod);
            });
        });
        startActions.put("redisqueue", () -> {
            currentSource = ctx.getBean(RedisQueue.class);
            tsList.add(currentSource);
            initTextActions();
            textActions.put("queuename", ((RedisQueue) currentSource)::setQueueName);
        });
        startActions.put("scheduledtask", () -> {
            currentSource = ctx.getBean(ScheduledTaskSupplier.class);
            tsList.add(currentSource);
            initTextActions();
            textActions.put("schedule", ((ScheduledTaskSupplier) currentSource)::setSchedule);
            textActions.put("script", script -> {
                TaskUnit taskUnit = new TaskUnit(script, TaskUnit.Type.SCRIPT);
                ((ScheduledTaskSupplier) currentSource).setTaskUnit(taskUnit);
            });
            textActions.put("proc", script -> {
                TaskUnit taskUnit = new TaskUnit(script, TaskUnit.Type.PROC);
                ((ScheduledTaskSupplier) currentSource).setTaskUnit(taskUnit);
            });
            textActions.put("params", ((ScheduledTaskSupplier) currentSource)::setParams);
        });
        startActions.put("looptask", () -> {
            currentSource = ctx.getBean(LoopTaskSupplier.class);
            tsList.add(currentSource);
            initTextActions();
            textActions.put("script", script -> {
                TaskUnit taskUnit = new TaskUnit(script, TaskUnit.Type.SCRIPT);
                ((LoopTaskSupplier) currentSource).setTaskUnit(taskUnit);
            });
            textActions.put("proc", script -> {
                TaskUnit taskUnit = new TaskUnit(script, TaskUnit.Type.PROC);
                ((LoopTaskSupplier) currentSource).setTaskUnit(taskUnit);
            });
            textActions.put("params", ((LoopTaskSupplier) currentSource)::setParams);
            textActions.put("count",
                    s ->
                        processInt(s, "count", true, ((LoopTaskSupplier) currentSource)::setCount)
                    );
            textActions.put("waitonsuccess", s -> {
                processInt(s, "waitonsuccess", true, ((LoopTaskSupplier) currentSource)::setWaitOnSuccess);
            });
            textActions.put("waitonfailure", s -> {
                processInt(s, "waitonfailure", true, ((LoopTaskSupplier) currentSource)::setWaitOnFailure);
            });

        });

        parse(is, "queues");
    }

    private void initTextActions() {
        textActions.clear();
        textActions.put("maxthreads", s -> {
            processInt(s, "maxthreads", false, ((QueueSource) currentSource)::setMaxThreads);
        });
        textActions.put("terminationtimeout", s -> {
            processInt(s, "terminationtimeout", true, ((QueueSource) currentSource)::setTerminationTimeout);
        });


        textActions.put("finalizer", finalizer -> {
            Matcher scriptMatcher = SCRIPT_PATTERN.matcher(finalizer);
            Matcher procMatcher = PROC_PATTERN.matcher(finalizer);

            final TaskUnit.Type type;

            if (scriptMatcher.matches())
                type = TaskUnit.Type.SCRIPT;
            else if (procMatcher.matches())
                type = TaskUnit.Type.PROC;
            else
                throw new RuntimeException(String.format("Finalizer format isn't supported: ", finalizer));

            if (currentSource instanceof HasTaskUnit) {
                TaskUnit currentTaskUnit = ((HasTaskUnit) currentSource).getTaskUnit();
                if (!type.equals(currentTaskUnit.getType())) {
                    throw new RuntimeException("Finalizer must have the same type as the task unit");
                }
            }

            TaskUnit taskUnit = new TaskUnit(finalizer, type);
            currentSource.setFinalizer(taskUnit);
        });
    }

    public List<TaskSource> getSources() {
        return tsList;
    }

    class SAXHandler extends DefaultHandler {
        private int level = 0;
        private Consumer<String> charactersAction = null;

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            // Queue parameters are at 3rd level
            if (level == 3 && charactersAction != null) {
                charactersAction.accept((new String(ch, start, length)).trim());
                charactersAction = null;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            level--;
            if ("looptask".equals(localName)) {
                LoopTaskSupplier prev = (LoopTaskSupplier) currentSource;
                for (int i = 1; i < prev.getCount(); i++) {
                    LoopTaskSupplier current = ctx.getBean(LoopTaskSupplier.class);
                    current.setTaskUnit(prev.getTaskUnit());
                    current.setParams(prev.getParams());
                    current.setCount(prev.getCount());
                    current.setWaitOnFailure(prev.getWaitOnFailure());
                    current.setWaitOnSuccess(prev.getWaitOnSuccess());
                    current.setFinalizer(prev.getFinalizer());
                    currentSource = current;
                    tsList.add(currentSource);
                }
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            level++;
            if (level == 2) {
                startActions.getOrDefault(localName, () -> {
                }).run();
            } else if (level == 3) {
                charactersAction = textActions.get(localName);
            }
        }
    }

    @Override
    public ContentHandler getSAXHandler() {
        return new SAXHandler();
    }
}
