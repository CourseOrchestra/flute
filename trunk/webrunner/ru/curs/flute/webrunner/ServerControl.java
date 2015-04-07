package ru.curs.flute.webrunner;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Сервлет, обслуживающий веб-интерфейс Флейты.
 */
public final class ServerControl extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Обработчик GET-запросов.
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		if (request.getRequestURI().endsWith("start")) {
			FluteRunner.RUNNER.start();
		} else if (request.getRequestURI().endsWith("stop")) {
			FluteRunner.RUNNER.stop();
		}
		response.getWriter().append(FluteRunner.RUNNER.getConsole());

	}

}
