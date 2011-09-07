package ru.curs.flute;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Класс, выполняющий синтаксический анализ выражений для заполнения листа
 * отчёта данными и производящий JDBC-statement.
 * 
 */
public final class StatementProducer {

	/**
	 * Регексп для разделителя, начинающего "спецячейку" (возможно, окруженного
	 * пробелами).
	 */
	public static final Pattern DELIMITER = Pattern.compile("\\s*~~\\s*");
	/**
	 * Регексп для T-SQL идентификатора хранимой процедуры.
	 */
	public static final Pattern IDENTIFIER = Pattern
			.compile("((\\[[^]]+\\])|([A-Za-z_][A-Za-z0-9_]*))((\\.\\[[^]]+\\])|(\\.[A-Za-z_][A-Za-z0-9_]*))*");

	/**
	 * Регексп для открывающей скобки, возможно, окруженной пробелами.
	 */
	public static final Pattern OPENPAR = Pattern.compile("\\s*\\(\\s*");

	/**
	 * Регексп для запятой, возможно, окруженной пробелами.
	 */
	public static final Pattern COMMA = Pattern.compile("\\s*,\\s*");

	/**
	 * Регексп для закрывающей скобки, возможно, окруженной пробелами.
	 */
	public static final Pattern CLOSEPAR = Pattern.compile("\\s*\\)\\s*");

	/**
	 * Регексп для идентификатора диапазона.
	 */
	public static final Pattern REGIONID = Pattern
			.compile("[A-Za-z_][A-Za-z0-9_]*");

	/**
	 * Регексп для числа (в общем случае, с плавающей точкой --- целые числа
	 * также должны попадать под этот регексп.
	 */
	public static final Pattern NUMBER = Pattern
			.compile("[+-]?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

	/**
	 * Регексп для целого числа.
	 */
	public static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");

	/**
	 * Регексп для литерала-строки.
	 */
	public static final Pattern STRING = Pattern.compile("'([^']+|'')*'");

	/**
	 * И совсем уж нехитрый регексп для литерала-XPath-выражения.
	 */
	public static final Pattern XPATH = Pattern.compile("X\\{([^}]+)\\}");

	private String rangeName;
	private String procedureName;
	private Document xmlParams;
	private final List<ProcedureParameter> parameters = new LinkedList<ProcedureParameter>();

	/**
	 * Проверка строки на соответствие ожидаемому выражению.
	 * 
	 * @param string
	 *            проверяемая строка.
	 * @param xmlParams
	 *            XML-параметры, для создания подстановки данных в хранимую
	 *            процедуру.
	 * @return экземпляр анализатора, если строка имеет верную структуру, или
	 *         null в обратном случае.
	 * @throws XPathExpressionException
	 *             В случае, если обнаружен XPath-параметр, и при этом у него
	 *             неверный синтаксис...
	 */
	public static StatementProducer validate(String string, Document xmlParams)
			throws XPathExpressionException {

		Lexer buf = new Lexer(string);
		StatementProducer result = new StatementProducer();
		result.xmlParams = xmlParams;

		// Читаем делимитер
		if (buf.readLexem(DELIMITER) == null)
			return null;

		// Читаем и запоминаем имя процедуры
		result.procedureName = buf.readLexem(IDENTIFIER);
		if (result.procedureName == null)
			return null;

		// Читаем открывающую скобку.
		if (buf.readLexem(OPENPAR) == null)
			return null;

		if (buf.readLexem(CLOSEPAR) == null) {
			// Случай непустого перечня аргументов.
			do {
				ProcedureParameter p = result.parseParameter(buf);
				if (p != null)
					result.parameters.add(p);
				else
					return null;
			} while (buf.readLexem(COMMA) != null);

			// Читаем закрывающую скобку.
			if (buf.readLexem(CLOSEPAR) == null)
				return null;
		}

		if (buf.readLexem(DELIMITER) != null)
			result.rangeName = buf.readLexem(REGIONID);
		return result;
	}

	/**
	 * Возвращает имя для именования диапазона.
	 */
	public String getRangeName() {
		return rangeName;
	}

	/**
	 * Возвращает имя хранимой процедуры.
	 */
	public String getProcName() {
		return procedureName;
	}

	/**
	 * Возвращает строковой шаблон для формирования JDBC-стейтмента.
	 */
	public String getStatementTemplate() {
		StringBuilder sb = new StringBuilder();
		sb.append("{call ");
		sb.append(procedureName);
		sb.append('(');
		for (int i = 0; i < parameters.size(); i++) {
			if (i > 0)
				sb.append(',');
			sb.append('?');
		}
		sb.append(")}");
		return sb.toString();
	}

	/**
	 * Возвращает JDBC-объект, ответственный за получение данных в соответствии
	 * с заданными параметрами.
	 * 
	 * @param conn
	 *            Подключение
	 * @throws EXLReporterRuntime
	 *             в случае, если не удалось подлючиться.
	 */
	public PreparedStatement produceStatement(Connection conn)
			throws EXLReporterRuntime {

		PreparedStatement result;
		try {
			result = conn.prepareStatement(getStatementTemplate());
			int i = 0;
			for (ProcedureParameter p : parameters) {
				i++;
				p.apply(i, result);
			}
		} catch (SQLException e) {
			throw new EXLReporterRuntime(
					"Processor's thread failed to create prepared statement on the database: "
							+ e.getMessage());
		}
		return result;
	}

	/**
	 * Возвращает читаемое человеком описание вызова процедуры.
	 */
	public String getProcCall() {
		StringBuilder sb = new StringBuilder();
		sb.append(procedureName);
		sb.append('(');
		for (int i = 0; i < parameters.size(); i++) {
			if (i > 0)
				sb.append(',');
			sb.append(parameters.get(i).stringValue());
		}
		sb.append(")");
		return sb.toString();
	}

	private ProcedureParameter parseParameter(Lexer l)
			throws XPathExpressionException {

		String lexem = l.readLexem(StatementProducer.STRING);
		if (lexem != null)
			return new StringParameter(lexem);

		lexem = l.readLexem(StatementProducer.NUMBER);
		if (lexem != null) {
			Matcher m = StatementProducer.INTEGER.matcher(lexem);
			if (m.matches())
				return new IntParameter(Integer.parseInt(lexem));
			else
				return new DoubleParameter(Double.parseDouble(lexem));
		}

		lexem = l.readLexem(StatementProducer.XPATH);
		if (lexem != null)
			return new XPathParameter(lexem);

		return null;
	}

	private abstract class ProcedureParameter {
		abstract void apply(int index, PreparedStatement stmt)
				throws SQLException;

		abstract String stringValue();

	}

	private class IntParameter extends ProcedureParameter {
		private final int value;

		IntParameter(int value) {
			this.value = value;
		}

		@Override
		void apply(int index, PreparedStatement stmt) throws SQLException {
			stmt.setInt(index, value);
		}

		@Override
		String stringValue() {
			return String.valueOf(value);
		}
	}

	private class DoubleParameter extends ProcedureParameter {

		private final double value;

		DoubleParameter(double value) {
			this.value = value;
		}

		@Override
		void apply(int index, PreparedStatement stmt) throws SQLException {
			stmt.setDouble(index, value);
		}

		@Override
		String stringValue() {
			return String.valueOf(value);
		}

	}

	private class StringParameter extends ProcedureParameter {

		private final String value;

		StringParameter(String value) {
			int state = 0;
			StringBuilder sb = new StringBuilder(value.length() - 2);
			iteration: for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				switch (state) {
				case 0:
					if (c == '\'')
						state = 1;
					break;
				case 1:
					if (c == '\'')
						state = 2;
					else
						sb.append(c);
					break;
				case 2:
					if (c == '\'') {
						sb.append('\'');
						state = 1;
					} else
						break iteration;
				}
			}

			this.value = sb.toString();
		}

		@Override
		void apply(int index, PreparedStatement stmt) throws SQLException {
			stmt.setString(index, value);
		}

		@Override
		String stringValue() {
			return '"' + value + '"';
		}

	}

	private class XPathParameter extends ProcedureParameter {

		private final String value;

		XPathParameter(String value) throws XPathExpressionException {
			XPath xpath = XPathFactory.newInstance().newXPath();

			// Cнимаем X{}!
			Matcher m = StatementProducer.XPATH.matcher(value);
			m.matches();
			XPathExpression expr = xpath.compile(m.group(1));
			NodeList nodes = (NodeList) expr.evaluate(xmlParams,
					XPathConstants.NODESET);

			if (nodes.getLength() > 0) {

				StringWriter writer = new StringWriter();

				try {
					Transformer transformer = TransformerFactory.newInstance()
							.newTransformer();
					transformer
							.setOutputProperty(OutputKeys.ENCODING, "UTF-16");
					transformer.transform(new DOMSource(nodes.item(0)),
							new StreamResult(writer));

				} catch (Exception e) {
					// Ну и фиг
				}

				this.value = writer.toString();
			} else
				this.value = null;

		}

		@Override
		void apply(int index, PreparedStatement stmt) throws SQLException {
			if (value != null)
				stmt.setNString(index, value);
			else
				stmt.setNull(index, Types.VARCHAR);
		}

		@Override
		String stringValue() {
			return '"' + value + '"';
		}

	}
}

final class Lexer implements CharSequence {
	private final CharSequence buf;
	private int position;

	Lexer(CharSequence buf) {
		this.buf = buf;
	}

	@Override
	public int length() {
		return buf.length() - position;
	}

	@Override
	public char charAt(int index) {
		return buf.charAt(index + position);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return buf.subSequence(start + position, end + position);
	}

	String readLexem(Pattern p) {
		Matcher m = p.matcher(this);
		String result = m.lookingAt() ? m.group(0) : null;
		if (result != null)
			position += result.length();
		return result;
	}
}
