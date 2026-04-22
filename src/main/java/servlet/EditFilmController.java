package servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.FilmDao;
import model.Film;

/**
 * MVC Controller for edit film page ("/edit-film")
 * Handles both GET (display form) and POST (save changes)
 */
@WebServlet("/edit-film")
public class EditFilmController extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String filmIdParam = request.getParameter("id");
		boolean isCreateMode = "new".equals(filmIdParam);
		Film film = null;
		String errorMessage = null;
		int filmId = -1;

		if (!isCreateMode) {
			try {
				if (filmIdParam == null || filmIdParam.isEmpty()) {
					errorMessage = "Invalid Film ID";
				} else {
					filmId = Integer.parseInt(filmIdParam);
					FilmDao dao = new FilmDao();
					film = dao.getFilmByID(filmId);
					
					if (film == null) {
						errorMessage = "Film Not Found with ID: " + filmId;
					}
				}
			} catch (NumberFormatException e) {
				errorMessage = "Invalid Film ID";
			}
		}

		// Pass data to view
		request.setAttribute("film", film);
		request.setAttribute("isCreateMode", isCreateMode);
		request.setAttribute("errorMessage", errorMessage);
		request.setAttribute("filmId", filmId);
		
		// Forward to JSP view
		request.getRequestDispatcher("/edit-film.jsp").forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		// Handle delete
		if ("delete".equalsIgnoreCase(request.getParameter("action"))) {
			Map<String, String> payload = FormatSupport.parsePayload(request);
			int id = parseIntOrDefault(FormatSupport.getValue(request, payload, "id"), -1);
			
			if (id <= 0) {
				request.setAttribute("errorMessage", "Invalid id");
				request.getRequestDispatcher("/edit-film.jsp").forward(request, response);
				return;
			}

			FilmDao dao = new FilmDao();
			boolean deleted = dao.deleteFilm(id);
			
			if (!deleted) {
				request.setAttribute("errorMessage", "Failed to delete film");
				request.getRequestDispatcher("/edit-film.jsp").forward(request, response);
				return;
			}

			// Redirect to home on successful delete
			if ("home".equalsIgnoreCase(request.getParameter("redirect"))) {
				response.sendRedirect(request.getContextPath() + "/home");
				return;
			}
		}

		// Parse film from request
		Film film = parseFilmFromRequest(request);
		FilmDao dao = new FilmDao();

		// Update existing film
		if (film != null && film.getId() > 0) {
			if (!isFilmUpdateValid(film)) {
				request.setAttribute("errorMessage", "Missing or invalid film fields");
				request.setAttribute("film", film);
				request.setAttribute("isCreateMode", false);
				request.getRequestDispatcher("/edit-film.jsp").forward(request, response);
				return;
			}

			boolean updated = dao.updateFilm(film);
			if (!updated) {
				request.setAttribute("errorMessage", "Failed to update film");
				request.setAttribute("film", film);
				request.setAttribute("isCreateMode", false);
				request.getRequestDispatcher("/edit-film.jsp").forward(request, response);
				return;
			}

			if ("home".equalsIgnoreCase(request.getParameter("redirect"))) {
				response.sendRedirect(request.getContextPath() + "/home");
				return;
			}

			request.setAttribute("successMessage", "Film updated successfully");
			request.setAttribute("film", film);
			request.setAttribute("isCreateMode", false);
			request.getRequestDispatcher("/edit-film.jsp").forward(request, response);
			return;
		}

		// Create new film
		if (!isFilmInsertValid(film)) {
			request.setAttribute("errorMessage", "Missing or invalid film fields");
			request.setAttribute("isCreateMode", true);
			request.getRequestDispatcher("/edit-film.jsp").forward(request, response);
			return;
		}

		boolean inserted = dao.insertFilm(film);
		if (!inserted) {
			request.setAttribute("errorMessage", "Failed to insert film");
			request.setAttribute("isCreateMode", true);
			request.getRequestDispatcher("/edit-film.jsp").forward(request, response);
			return;
		}

		if ("home".equalsIgnoreCase(request.getParameter("redirect"))) {
			response.sendRedirect(request.getContextPath() + "/home");
			return;
		}

		request.setAttribute("successMessage", "Film created successfully");
		request.setAttribute("isCreateMode", true);
		request.getRequestDispatcher("/edit-film.jsp").forward(request, response);
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
}
