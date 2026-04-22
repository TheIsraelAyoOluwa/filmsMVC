package servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.FilmDao;
import model.Film;

/**
 * MVC Controller for film detail page ("/film-detail")
 * Fetches single film and forwards to JSP view
 */
@WebServlet("/film-detail")
public class FilmDetailController extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String filmIdParam = request.getParameter("id");
		Film film = null;
		String errorMessage = null;

		try {
			if (filmIdParam == null || filmIdParam.isEmpty()) {
				errorMessage = "Invalid Film ID";
			} else {
				int filmId = Integer.parseInt(filmIdParam);
				FilmDao dao = new FilmDao();
				film = dao.getFilmByID(filmId);
				
				if (film == null) {
					errorMessage = "Film Not Found with ID: " + filmId;
				}
			}
		} catch (NumberFormatException e) {
			errorMessage = "Invalid Film ID";
		}

		// Pass data to view
		request.setAttribute("film", film);
		request.setAttribute("errorMessage", errorMessage);
		
		// Forward to JSP view
		request.getRequestDispatcher("/film-detail.jsp").forward(request, response);
	}
}
