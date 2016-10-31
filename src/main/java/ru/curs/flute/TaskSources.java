package ru.curs.flute;

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

@Component
@Import({ SQLTablePoller.class, RedisQueue.class, CRONTasksSupplier.class })
class TaskSources extends XMLParamsParser {
	private final List<TaskSource> tsList = new ArrayList<>();
	private final Map<String, Runnable> startActions = new HashMap<>();
	private final Map<String, Consumer<String>> textActions = new HashMap<>();

	private TaskSource currentSource;

	TaskSources(@Autowired @Qualifier("confSource") InputStream is, final @Autowired ApplicationContext ctx)
			throws EFluteCritical {

		startActions.put("dbtable", () -> {
			currentSource = ctx.getBean(SQLTablePoller.class);
			tsList.add(currentSource);
			initTextActions();
			textActions.put("tablename", ((SQLTablePoller) currentSource)::setTableName);
			textActions.put("pollingperiod", (s) -> {
				processInt(s, "pollingperiod", true, ((SQLTablePoller) currentSource)::setQueryPeriod);
			});
		});
		startActions.put("redisqueue", () -> {
			currentSource = ctx.getBean(RedisQueue.class);
			tsList.add(currentSource);
			initTextActions();
			textActions.put("queuename", ((RedisQueue) currentSource)::setQueueName);
		});
		startActions.put("crontask", () -> {
			currentSource = ctx.getBean(CRONTasksSupplier.class);
			tsList.add(currentSource);
			initTextActions();
			textActions.put("schedule", ((CRONTasksSupplier) currentSource)::setSchedule);
			textActions.put("script", ((CRONTasksSupplier) currentSource)::setScript);
			textActions.put("params", ((CRONTasksSupplier) currentSource)::setParams);
		});

		parse(is, "queues");
	}

	private void initTextActions() {
		textActions.clear();
		textActions.put("maxThreads", (s) -> {
			processInt(s, "maxThreads", false, currentSource::setMaxThreads);
		});
		textActions.put("terminationtimeout", (s) -> {
			processInt(s, "terminationtimeout", true, currentSource::setTerminationTimeout);
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
	ContentHandler getSAXHandler() {
		return new SAXHandler();
	}
}
