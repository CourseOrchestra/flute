package ru.curs.flute.source;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

@Component
@Import({
		SqlTablePoller.class, RedisQueue.class,
		ScheduledTaskSupplier.class, LoopTaskSupplier.class
})
public class TaskSources extends XMLParamsParser {
	private final List<TaskSource> tsList = new ArrayList<>();
	private final Map<String, Runnable> startActions = new HashMap<>();
	private final Map<String, Consumer<String>> textActions = new HashMap<>();

	private TaskSource currentSource;

	TaskSources(@Autowired @Qualifier("confSource") InputStream is, final @Autowired ApplicationContext ctx)
			throws EFluteCritical {

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
			textActions.put("script", ((ScheduledTaskSupplier) currentSource)::setScript);
			textActions.put("params", ((ScheduledTaskSupplier) currentSource)::setParams);
		});
		startActions.put("looptask", () -> {
			currentSource = ctx.getBean(LoopTaskSupplier.class);
			tsList.add(currentSource);
			initTextActions();
			textActions.put("script", ((LoopTaskSupplier) currentSource)::setScript);
			textActions.put("params", ((LoopTaskSupplier) currentSource)::setParams);
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
			processInt(s, "maxthreads", false, ((QueueSource)currentSource)::setMaxThreads);
		});
		textActions.put("terminationtimeout", s -> {
			processInt(s, "terminationtimeout", true, ((QueueSource)currentSource)::setTerminationTimeout);
		});
		textActions.put("finalizer", currentSource::setFinalizer);
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
