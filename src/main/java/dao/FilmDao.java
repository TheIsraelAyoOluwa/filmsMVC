package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import model.Film;

public class FilmDao {

	Film oneFilm = null;
	Connection conn = null;
	Statement stmt = null;
//	String user = "adelekei";
//	String password = "gacEiblad9";
//	String url = "jdbc:mysql://mudfoot.doc.stu.mmu.ac.uk:6306/" + user
//			+ "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
//	
	
	
    private final String dbUrl = "jdbc:mysql://35.184.162.65:3306/";
    private final String dbName = "mmu"; 
    private final String password = "Ayooluwa1."; 
    private final String user = "root"; 
    
    private final String url = dbUrl + dbName
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&enabledTLSProtocols=TLSv1.2"
            + "&serverTimezone=UTC";
    
    
	String preferredTable = System.getProperty("films.table", "films");

	public FilmDao() {
	}

	private void openConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			try {
				Class.forName("com.mysql.jdbc.Driver");
			} catch (ClassNotFoundException ex) {
				System.out.println(
						"[FilmDao] MySQL JDBC driver not found. Add mysql-connector-j jar to WEB-INF/lib and project build path.");
				ex.printStackTrace();
				return;
			}
		}

		try {
			conn = DriverManager.getConnection(url, user, password);
			stmt = conn.createStatement();
		} catch (SQLException se) {
			System.out.println("[FilmDao] DB connection failed: " + se.getMessage());
			se.printStackTrace();
		}
	}

	private void closeConnection() {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	private Film getNextFilm(ResultSet rs) {
		Film thisFilm = null;
		try {
			int id = rs.getInt("id");
			if (rs.wasNull()) {
				System.out.println("[FilmDao] Skipping row with null id.");
				return null;
			}

			thisFilm = new Film(id, rs.getString("title"), rs.getInt("year"), rs.getString("director"),
					rs.getString("stars"), rs.getString("review"));
		} catch (SQLException e) {
			System.out.println("[FilmDao] Failed to map film row: " + e.getMessage());
			e.printStackTrace();
		}
		return thisFilm;
	}

	private String resolveFilmsTable() {
		if (conn == null) {
			return preferredTable;
		}

		String sql = "select 1 from " + preferredTable + " limit 1";
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			if (rs != null) {
				System.out.println("[FilmDao] Using table: " + preferredTable);
				return preferredTable;
			}
		} catch (SQLException e) {
			throw new IllegalStateException("[FilmDao] Required table not accessible: " + preferredTable, e);
		}

		return preferredTable;
	}

	private int getNextFilmId(String table) {
		String sql = "select coalesce(max(id), 0) + 1 as next_id from " + table;
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				return rs.getInt("next_id");
			}
		} catch (SQLException se) {
			System.out.println("[FilmDao] Failed to generate next id: " + se.getMessage());
			se.printStackTrace();
		}
		return -1;
	}

	public ArrayList<Film> getAllFilms() {
		ArrayList<Film> allFilms = new ArrayList<Film>();
		openConnection();

		if (conn == null) {
			System.out.println("[FilmDao] No DB connection in getAllFilms().");
			return allFilms;
		}

		String table = resolveFilmsTable();
		String sql = "select * from " + table + " order by id desc";
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs1 = ps.executeQuery()) {
			while (rs1.next()) {
				oneFilm = getNextFilm(rs1);
				if (oneFilm != null) {
					allFilms.add(oneFilm);
				}
			}
		} catch (SQLException se) {
			System.out.println("[FilmDao] Query failed in getAllFilms(): " + se.getMessage());
			se.printStackTrace();
		} finally {
			closeConnection();
		}

		return allFilms;
	}

	public Film getFilmByID(int id) {
		openConnection();
		oneFilm = null;

		if (conn == null) {
			System.out.println("[FilmDao] No DB connection in getFilmByID().");
			return null;
		}

		String table = resolveFilmsTable();
		try (PreparedStatement ps = conn.prepareStatement("select * from " + table + " where id = ?")) {
			ps.setInt(1, id);
			try (ResultSet rs1 = ps.executeQuery()) {
				if (rs1.next()) {
					oneFilm = getNextFilm(rs1);
				}
			}
		} catch (SQLException se) {
			System.out.println("[FilmDao] Query failed in getFilmByID(): " + se.getMessage());
			se.printStackTrace();
		} finally {
			closeConnection();
		}

		return oneFilm;
	}

	public ArrayList<Film> getFilmsByTitle(String title) {
		ArrayList<Film> films = new ArrayList<Film>();
		openConnection();

		if (conn == null) {
			System.out.println("[FilmDao] No DB connection in getFilmsByTitle().");
			return films;
		}

		String table = resolveFilmsTable();
		String sql = "select * from " + table + " where lower(title) like lower(?) order by id desc";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, "%" + title + "%");
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Film film = getNextFilm(rs);
					if (film != null) {
						films.add(film);
					}
				}
			}
		} catch (SQLException se) {
			System.out.println("[FilmDao] Query failed in getFilmsByTitle(): " + se.getMessage());
			se.printStackTrace();
		} finally {
			closeConnection();
		}

		return films;
	}

	public ArrayList<Film> searchFilms(String title, int year, String genre) {
		ArrayList<Film> films = new ArrayList<Film>();
		openConnection();

		if (conn == null) {
			System.out.println("[FilmDao] No DB connection in searchFilms().");
			return films;
		}

		String table = resolveFilmsTable();
		StringBuilder sql = new StringBuilder("select * from " + table + " where 1=1");
		ArrayList<Object> params = new ArrayList<Object>();

		if (title != null && !title.trim().isEmpty()) {
			sql.append(" and lower(title) like lower(?)");
			params.add("%" + title.trim() + "%");
		}

		if (year > 0) {
			sql.append(" and year = ?");
			params.add(Integer.valueOf(year));
		}

		if (genre != null && !genre.trim().isEmpty()) {
			if (hasColumn(table, "genre")) {
				sql.append(" and lower(genre) like lower(?)");
				params.add("%" + genre.trim() + "%");
			} else {
				System.out.println("[FilmDao] Genre filter ignored: 'genre' column does not exist in table " + table);
			}
		}

		sql.append(" order by id desc");

		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			for (int i = 0; i < params.size(); i++) {
				Object param = params.get(i);
				if (param instanceof Integer) {
					ps.setInt(i + 1, ((Integer) param).intValue());
				} else {
					ps.setString(i + 1, String.valueOf(param));
				}
			}

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Film film = getNextFilm(rs);
					if (film != null) {
						films.add(film);
					}
				}
			}
		} catch (SQLException se) {
			System.out.println("[FilmDao] Query failed in searchFilms(): " + se.getMessage());
			se.printStackTrace();
		} finally {
			closeConnection();
		}

		return films;
	}

	private boolean hasColumn(String table, String column) {
		String sql = "select 1 from information_schema.columns where table_schema = ? and table_name = ? and column_name = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, user);
			ps.setString(2, table);
			ps.setString(3, column);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException se) {
			System.out.println("[FilmDao] Could not verify column '" + column + "' on table '" + table + "': "
					+ se.getMessage());
			return false;
		}
	}

	public boolean insertFilm(Film film) {
		openConnection();

		if (conn == null || film == null) {
			System.out.println("[FilmDao] Insert skipped: conn or film is null.");
			return false;
		}

		String table = resolveFilmsTable();
		if (film.getId() <= 0) {
			int nextId = getNextFilmId(table);
			if (nextId <= 0) {
				System.out.println("[FilmDao] Insert skipped: could not determine next id.");
				return false;
			}
			film.setId(nextId);
		}

		String sql = "insert into " + table + " (id, title, year, director, stars, review) values (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, film.getId());
			ps.setString(2, film.getTitle());
			ps.setInt(3, film.getYear());
			ps.setString(4, film.getDirector());
			ps.setString(5, film.getStars());
			ps.setString(6, film.getReview());
			return ps.executeUpdate() > 0;
		} catch (SQLException se) {
			System.out.println("[FilmDao] Insert failed: " + se.getMessage());
			se.printStackTrace();
			return false;
		} finally {
			closeConnection();
		}
	}

	public boolean updateFilm(Film film) {
		openConnection();

		if (conn == null || film == null) {
			System.out.println("[FilmDao] Update skipped: conn or film is null.");
			return false;
		}

		String table = resolveFilmsTable();
		String sql = "update " + table + " set title = ?, year = ?, director = ?, stars = ?, review = ? where id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, film.getTitle());
			ps.setInt(2, film.getYear());
			ps.setString(3, film.getDirector());
			ps.setString(4, film.getStars());
			ps.setString(5, film.getReview());
			ps.setInt(6, film.getId());
			return ps.executeUpdate() > 0;
		} catch (SQLException se) {
			System.out.println("[FilmDao] Update failed: " + se.getMessage());
			se.printStackTrace();
			return false;
		} finally {
			closeConnection();
		}
	}

	public boolean deleteFilm(int id) {
		openConnection();

		if (conn == null) {
			System.out.println("[FilmDao] Delete skipped: conn is null.");
			return false;
		}

		String table = resolveFilmsTable();
		String sql = "delete from " + table + " where id = ?";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, id);
			return ps.executeUpdate() > 0;
		} catch (SQLException se) {
			System.out.println("[FilmDao] Delete failed: " + se.getMessage());
			se.printStackTrace();
			return false;
		} finally {
			closeConnection();
		}
	}
}
