package servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.FilmDao;
import model.Film;

/**
 * MVC Controller for home page ("/")
 * Fetches all films and forwards to JSP view
 */
@WebServlet("/home")
public class HomePageController extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		FilmDao dao = new FilmDao();
		ArrayList<Film> allFilms = dao.getAllFilms();
		
		// Pass data to view
		request.setAttribute("films", allFilms);
		
		// Forward to JSP view
		request.getRequestDispatcher("/home.jsp").forward(request, response);
	}
}
