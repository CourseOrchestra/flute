package ru.curs.flute;

import java.io.InputStream;
import java.util.HashMap;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ru.curs.flute.EFluteCritical;

/**
 * Common Flute parameters, parsed from XML configuration file.
 */
@Component
class CommonParameters extends XMLParamsParser {
	// JDBC
	private String connString;
	private String dbUser;
	private String dbPassword;

	// Redis
	private String redisHost = "localhost";
	private int redisPort = 6379;
	private String redisPassword = null;
	private boolean exposeRedis = false;

	// Celesta
	private String scorePath;
	private String pylibPath;
	private String javalibPath;
	private String fluteUserId = "flute";
	private boolean logLogins = false;
	private boolean skipDBUpdate = false;
	private boolean forceDBInitialize = false;

	// Common
	private boolean neverStop = true;
	private int retryWait = 60000;

	private final HashMap<String, Consumer<String>> textActions = new HashMap<>();

	public CommonParameters(@Autowired @Qualifier("confSource") InputStream is) throws EFluteCritical {
		parse(is, "common parameters");
	}

	public String getConnString() {
		return connString;
	}

	public String getDBUser() {
		return dbUser;
	}

	public String getDBPassword() {
		return dbPassword;
	}

	public String getScorePath() {
		return scorePath;
	}

	public String getPylibPath() {
		return pylibPath;
	}

	public String getFluteUserId() {
		return fluteUserId;
	}

	public boolean isNeverStop() {
		return neverStop;
	}

	public String getRedisHost() {
		return redisHost;
	}

	public int getRedisPort() {
		return redisPort;
	}

	public int getRetryWait() {
		return retryWait;
	}

	public boolean isLogLogins() {
		return logLogins;
	}

	public boolean isSkipDBUpdate() {
		return skipDBUpdate;
	}

	public boolean isForceDBInitialize() {
		return forceDBInitialize;
	}

	private void setConnString(String connString) {
		this.connString = connString;
	}

	private void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	private void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	private void setRedisHost(String redisHost) {
		this.redisHost = redisHost;
	}

	private void setRedisPort(int redisPort) {
		this.redisPort = redisPort;
	}

	private void setScorePath(String scorePath) {
		this.scorePath = scorePath;
	}

	private void setPylibPath(String pylibPath) {
		this.pylibPath = pylibPath;
	}

	private void setFluteUserId(String fluteUserId) {
		this.fluteUserId = fluteUserId;
	}

	private void setNeverStop(boolean neverStop) {
		this.neverStop = neverStop;
	}

	private void setRetryWait(int retryWait) {
		this.retryWait = retryWait;
	}

	private void setLogLogins(boolean logLogins) {
		this.logLogins = logLogins;
	}

	private void setSkipDBUpdate(boolean skipDBUpdate) {
		this.skipDBUpdate = skipDBUpdate;
	}

	private void setForceDBInitialize(boolean forceDBInitialize) {
		this.forceDBInitialize = forceDBInitialize;
	}

	{
		textActions.put("neverstop", (s) -> {
			setNeverStop(Boolean.parseBoolean(s));
		});
		textActions.put("retrywait", (s) -> {
			processInt(s, "retrywait", true, this::setRetryWait);
		});
		textActions.put("dbconnstring", this::setConnString);
		textActions.put("dbuser", this::setDbUser);
		textActions.put("dbpassword", this::setDbPassword);
		textActions.put("redishost", this::setRedisHost);
		textActions.put("redisport", (s) -> {
			processInt(s, "redisport", false, this::setRedisPort);
		});
		textActions.put("redispassword", this::setRedisPassword);
		textActions.put("exposeredis", (s) -> {
			setExposeRedis(Boolean.parseBoolean(s));
		});

		textActions.put("scorepath", this::setScorePath);
		textActions.put("pylibpath", this::setPylibPath);
		textActions.put("javalibpath", this::setJavaLibPath);
		textActions.put("fluteuserid", this::setFluteUserId);
		textActions.put("loglogins", (s) -> {
			setLogLogins(Boolean.parseBoolean(s));
		});
		textActions.put("skipdbupdate", (s) -> {
			setSkipDBUpdate(Boolean.parseBoolean(s));
		});
		textActions.put("forcedbinitialize", (s) -> {
			setForceDBInitialize(Boolean.parseBoolean(s));
		});

	}

	class SAXHandler extends DefaultHandler {
		private int level = 0;
		private Consumer<String> charactersAction = null;

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (level == 2 && charactersAction != null) {
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
			charactersAction = textActions.get(localName);
		}
	}

	@Override
	ContentHandler getSAXHandler() {
		return new SAXHandler();
	}

	String getRedisPassword() {
		return redisPassword;
	}

	void setRedisPassword(String redisPassword) {
		this.redisPassword = redisPassword;
	}

	boolean isExposeRedis() {
		return exposeRedis;
	}

	void setExposeRedis(boolean exposeRedis) {
		this.exposeRedis = exposeRedis;
	}

	void setJavaLibPath(String javalibPath) {
		this.javalibPath = javalibPath;
	}

	String getJavaLibPath() {
		return javalibPath;
	}

}
