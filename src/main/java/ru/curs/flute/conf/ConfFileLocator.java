package ru.curs.flute.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.curs.flute.exception.EFluteCritical;

/**
 * Configuration file locator.
 */
@Configuration
public class ConfFileLocator {
	private static File overridenFile = null;

	public static File getConfFile() {
		String path = getMyPath();
		File f = new File(path + "flute.xml");
		System.out.println();
		return f;
	}

	public static void setFile(File f) {
		overridenFile = f;
	}

	@Bean("confSource")
	@Scope("prototype")
	static InputStream getConfInputStream() throws EFluteCritical {
		File confFile = overridenFile != null && overridenFile.exists() ? overridenFile : getConfFile();
		InputStream result;
		try {
			result = new FileInputStream(confFile);
		} catch (FileNotFoundException e) {
			throw new EFluteCritical(String.format("Configuration file %s cannot be found.",
					overridenFile == null ? confFile : overridenFile));
		}
		return result;
	}

	private static String getMyPath() {
		String path = ConfFileLocator.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		File f = new File(path.replace("%20", " "));
		if (f.getAbsolutePath().toLowerCase().endsWith(".jar")) {
			return f.getParent() + File.separator;
		} else {
			return f.getAbsolutePath() + File.separator;
		}
	}
}
