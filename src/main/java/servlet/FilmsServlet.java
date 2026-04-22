package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.FilmDao;
import model.Film;
import servlet.FormatSupport.DataFormat;

@WebServlet("/films")
public class FilmsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String ALLOWED_METHODS = "GET,POST,PUT,DELETE,OPTIONS";
	private static final String ALLOWED_HEADERS = "Content-Type,Accept,Authorization";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		DataFormat format = FormatSupport.resolveResponseFormat(request);
		setResponseMeta(response, format);

		FilmDao dao = new FilmDao();
		String idParam = request.getParameter("id");
		String titleParam = request.getParameter("title");

		if (!isBlank(idParam)) {
			int id = parseIntOrDefault(idParam, -1);
			if (id <= 0) {
				writeError(response, format, HttpServletResponse.SC_BAD_REQUEST, "Invalid id");
				return;
			}

			Film film = dao.getFilmByID(id);
			if (film == null) {
				writeError(response, format, HttpServletResponse.SC_NOT_FOUND, "Film not found");
				return;
			}

			writeSingle(response.getWriter(), format, film);
			return;
		}

		if (!isBlank(titleParam)) {
			ArrayList<Film> filmsByTitle = dao.getFilmsByTitle(titleParam);
			writeList(response.getWriter(), format, filmsByTitle);
			return;
		}

		ArrayList<Film> allFilms = dao.getAllFilms();
		writeList(response.getWriter(), format, allFilms);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		DataFormat format = FormatSupport.resolveResponseFormat(request);
		setResponseMeta(response, format);

		if (isDeleteRequest(request)) {
			Map<String, String> payload = FormatSupport.parsePayload(request);
			int id = parseIntOrDefault(FormatSupport.getValue(request, payload, "id"), -1);
			if (id <= 0) {
				writeError(response, format, HttpServletResponse.SC_BAD_REQUEST, "Invalid id");
				return;
			}

			FilmDao dao = new FilmDao();
			boolean deleted = dao.deleteFilm(id);
			if (!deleted) {
				writeError(response, format, HttpServletResponse.SC_NOT_FOUND, "Failed to delete film");
				return;
			}

			if (shouldRedirectToHome(request)) {
				response.sendRedirect(request.getContextPath() + "/home");
				return;
			}

			writeMessage(response.getWriter(), format, true, "Film deleted");
			return;
		}

		Film film = parseFilmFromRequest(request);
		FilmDao dao = new FilmDao();
		if (film != null && film.getId() > 0) {
			if (!isFilmUpdateValid(film)) {
				writeError(response, format, HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid film fields");
				return;
			}

			boolean updated = dao.updateFilm(film);
			if (!updated) {
				writeError(response, format, HttpServletResponse.SC_NOT_FOUND, "Failed to update film");
				return;
			}

			if (shouldRedirectToHome(request)) {
				response.sendRedirect(request.getContextPath() + "/home");
				return;
			}

			writeMessage(response.getWriter(), format, true, "Film updated");
			return;
		}

		if (!isFilmInsertValid(film)) {
			writeError(response, format, HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid film fields");
			return;
		}

		boolean inserted = dao.insertFilm(film);
		if (!inserted) {
			writeError(response, format, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to insert film");
			return;
		}

		if (shouldRedirectToHome(request)) {
			response.sendRedirect(request.getContextPath() + "/home");
			return;
		}

		response.setStatus(HttpServletResponse.SC_CREATED);
		writeMessage(response.getWriter(), format, true, "Film inserted");
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		DataFormat format = FormatSupport.resolveResponseFormat(request);
		setResponseMeta(response, format);

		Film film = parseFilmFromRequest(request);
		if (!isFilmUpdateValid(film)) {
			writeError(response, format, HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid film fields");
			return;
		}

		FilmDao dao = new FilmDao();
		boolean updated = dao.updateFilm(film);
		if (!updated) {
			writeError(response, format, HttpServletResponse.SC_NOT_FOUND, "Failed to update film");
			return;
		}

		writeMessage(response.getWriter(), format, true, "Film updated");
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		DataFormat format = FormatSupport.resolveResponseFormat(request);
		setResponseMeta(response, format);

		Map<String, String> payload = FormatSupport.parsePayload(request);
		int id = parseIntOrDefault(FormatSupport.getValue(request, payload, "id"), -1);
		if (id <= 0) {
			writeError(response, format, HttpServletResponse.SC_BAD_REQUEST, "Invalid id");
			return;
		}

		FilmDao dao = new FilmDao();
		boolean deleted = dao.deleteFilm(id);
		if (!deleted) {
			writeError(response, format, HttpServletResponse.SC_NOT_FOUND, "Failed to delete film");
			return;
		}

		writeMessage(response.getWriter(), format, true, "Film deleted");
	}

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		addCorsHeaders(response);
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	private Film parseFilmFromRequest(HttpServletRequest request) throws IOException {
		Map<String, String> payload = FormatSupport.parsePayload(request);
		Film film = new Film();
		film.setId(parseIntOrDefault(FormatSupport.getValue(request, payload, "id"), -1));
		film.setTitle(FormatSupport.getValue(request, payload, "title"));
		film.setYear(parseIntOrDefault(FormatSupport.getValue(request, payload, "year"), 0));
		film.setDirector(FormatSupport.getValue(request, payload, "director"));
		film.setStars(FormatSupport.getValue(request, payload, "stars"));
		film.setReview(FormatSupport.getValue(request, payload, "review"));
		return film;
	}

	private boolean isFilmInsertValid(Film film) {
		return film != null && film.getYear() > 0 && !isBlank(film.getTitle()) && !isBlank(film.getDirector())
				&& !isBlank(film.getStars()) && !isBlank(film.getReview());
	}

	private boolean isFilmUpdateValid(Film film) {
		return isFilmInsertValid(film) && film.getId() > 0;
	}

	private void writeList(PrintWriter out, DataFormat format, ArrayList<Film> films) {
		switch (format) {
		case XML:
			out.print("<response><success>true</success><count>" + films.size() + "</count><data>");
			for (Film film : films) {
				out.print(FormatSupport.filmAsXml(film));
			}
			out.print("</data></response>");
			break;
		case TEXT:
			out.print("success=true\ncount=" + films.size());
			for (Film film : films) {
				out.print("\n---\n" + FormatSupport.filmAsText(film));
			}
			break;
		case JSON:
		default:
			out.print("{\"success\":true,\"count\":" + films.size() + ",\"data\":[");
			for (int i = 0; i < films.size(); i++) {
				out.print(FormatSupport.filmAsJson(films.get(i)));
				if (i < films.size() - 1) {
					out.print(",");
				}
			}
			out.print("]}");
			break;
		}
	}

	private void writeSingle(PrintWriter out, DataFormat format, Film film) {
		switch (format) {
		case XML:
			out.print("<response><success>true</success><data>" + FormatSupport.filmAsXml(film) + "</data></response>");
			break;
		case TEXT:
			out.print("success=true\n" + FormatSupport.filmAsText(film));
			break;
		case JSON:
		default:
			out.print("{\"success\":true,\"data\":" + FormatSupport.filmAsJson(film) + "}");
			break;
		}
	}

	private void writeMessage(PrintWriter out, DataFormat format, boolean success, String message) {
		out.print(FormatSupport.messageBody(format, success, message));
	}

	private void writeError(HttpServletResponse response, DataFormat format, int status, String message)
			throws IOException {
		response.setStatus(status);
		response.getWriter().print(FormatSupport.messageBody(format, false, message));
	}

	private void setResponseMeta(HttpServletResponse response, DataFormat format) {
		addCorsHeaders(response);
		response.setContentType(FormatSupport.contentType(format));
		response.setCharacterEncoding("UTF-8");
	}

	private void addCorsHeaders(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
		response.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS);
		response.setHeader("Access-Control-Max-Age", "3600");
	}

	private int parseIntOrDefault(String value, int defaultValue) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private boolean shouldRedirectToHome(HttpServletRequest request) {
		return "home".equalsIgnoreCase(request.getParameter("redirect"));
	}

	private boolean isDeleteRequest(HttpServletRequest request) {
		return "delete".equalsIgnoreCase(request.getParameter("action"));
	}
}
