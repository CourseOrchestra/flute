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
		String path = getJarPath();
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

	private static String getJarPath() {
		final String result;

		String path = ConfFileLocator.class.getResource(ConfFileLocator.class.getSimpleName() + ".class").getPath();
		path = path.replace("%20", " ");

		if (path.contains(".jar")) {
			if (path.startsWith("file:")) {
				path = path.replace("file:", "");
			}
			path = path.substring(0, path.indexOf("jar!"));

			File f = new File(path).getParentFile();
			result = f.getPath() + File.separator;
		} else {
			File f = new File(path).getParentFile();
			result = f.getParent() + File.separator;
		}

		return result;
	}
}
