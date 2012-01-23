package ru.curs.flute.xml2spreadsheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Класс для запуска из командной строки.
 * 
 */
public class Main {
	private static final String DATA = "-data";
	private static final String TEMPLATE = "-template";
	private static final String DESCR = "-descr";
	private static final String OUT = "-out";
	private static final String SAX = "-sax";

	private enum State {
		READTOKEN, READDATA, READTEMPLATE, READDESCR, READOUT,
	};

	/**
	 * Главный метод класса.
	 * 
	 * @param args
	 *            аргументы
	 * @throws XML2SpreadSheetError
	 *             в случае, если произошла ошибка конвертации
	 * @throws FileNotFoundException
	 *             в случае, если файл не найден
	 */
	public static void main(String[] args) throws FileNotFoundException,
			XML2SpreadSheetError {

		FileInputStream iff = null;
		File descr = null;
		File template = null;
		FileOutputStream output = null;
		boolean useSAX = false;

		State state = State.READTOKEN;

		for (String s : args)
			switch (state) {
			case READTOKEN:
				if (DATA.equalsIgnoreCase(s))
					state = State.READDATA;
				else if (TEMPLATE.equalsIgnoreCase(s))
					state = State.READTEMPLATE;
				else if (DESCR.equalsIgnoreCase(s))
					state = State.READDESCR;
				else if (OUT.equalsIgnoreCase(s))
					state = State.READOUT;
				else if (SAX.equalsIgnoreCase(s))
					useSAX = true;
				else
					showHelp();
				break;
			case READDATA:
				iff = new FileInputStream(new File(s));
				state = State.READTOKEN;
				break;
			case READTEMPLATE:
				template = new File(s);
				state = State.READTOKEN;
				break;
			case READDESCR:
				descr = new File(s);
				state = State.READTOKEN;
				break;
			case READOUT:
				output = new FileOutputStream(new File(s));
				state = State.READTOKEN;
				break;
			}

		checkParams(iff, descr, template, output);
		XML2Spreadsheet.process(iff, descr, template, useSAX, output);
		
		System.out.println("Spreadsheet created successfully.");
	}

	private static void checkParams(FileInputStream iff, File descr,
			File template, FileOutputStream output) {
		if (iff == null || descr == null || template == null || output == null)
			showHelp();
	}

	private static void showHelp() {
		System.out
				.println("XML2Spreadsheet should be called with the following parameters (any order):");
		System.out.println(DATA + " XML data file");
		System.out.println(TEMPLATE + " XLS/XLSX template file");
		System.out.println(DESCR + " descriptor file");
		System.out.println("[" + SAX
				+ "] use SAX engine (instead of DOM) to parse data file");
		System.out.println(OUT + " output file");
		
		System.exit(1);
	}
}
