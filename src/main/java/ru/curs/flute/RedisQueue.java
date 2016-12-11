package ru.curs.flute;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

@Component
@Scope("prototype")
class RedisQueue extends TaskSource {
	private final static int INTERRUPTION_CHECK_PERIOD = 10; // seconds

	@Autowired
	private JedisPool pool;

	private String queueName;

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public Object getQueueName() {
		return queueName;
	}

	@Override
	FluteTask getTask() throws EFluteCritical, InterruptedException {
		FluteTask result;
		while (true) {
			List<String> val;
			try (Jedis j = pool.getResource()) {
				// val = j.brpop(0, queueName);
				while ((val = j.brpop(INTERRUPTION_CHECK_PERIOD, queueName)) == null)
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

	static String toJSON(FluteTask t) {
		JsonObject o = new JsonObject();
		o.addProperty("script", t.getScript());
		o.addProperty("params", t.getParams());
		return o.toString();
	}

	private static ThreadLocal<JsonParser> jp = ThreadLocal.withInitial(JsonParser::new);

	FluteTask fromJSON(String string) throws EFluteNonCritical {
		JsonObject o;
		try {
			JsonElement e = jp.get().parse(string);
			o = e.getAsJsonObject();
		} catch (RuntimeException e) {
			throw new EFluteNonCritical("Message parsing error: " + e.getMessage());
		}

		JsonElement script = o.get("script");
		if (script == null)
			throw new EFluteNonCritical(String.format("No script value found in message '%s'", string));

		JsonElement params = o.get("params");

		JsonElement idElement = o.get("id");
		int id;
		if (idElement == null)
			id = 0;
		else {
			try {
				id = Integer.parseInt(idElement.getAsString());
			} catch (Exception e) {
				id = 0;
			}
		}

		try {
			String taskParams;
			if (params == null || params.isJsonNull()) {
				taskParams = null;
			} else if (params.isJsonPrimitive()) {
				taskParams = params.getAsString();
			} else {
				taskParams = params.toString();
			}
			FluteTask result = new FluteTask(this, id, script.getAsString(), taskParams);
			return result;
		} catch (RuntimeException e) {
			throw new EFluteNonCritical(String.format("Message parse error: script and params should be strings."));
		}
	}
}
