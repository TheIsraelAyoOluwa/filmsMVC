package servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.servlet.http.HttpServletRequest;

import model.Film;

public final class FormatSupport {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	public enum DataFormat {
		JSON, XML, TEXT
	}

	private FormatSupport() {
	}

	public static DataFormat resolveResponseFormat(HttpServletRequest request) {
		String formatParam = request.getParameter("format");
		if (formatParam != null) {
			String fp = formatParam.trim().toLowerCase();
			if ("xml".equals(fp)) {
				return DataFormat.XML;
			}
			if ("text".equals(fp) || "txt".equals(fp)) {
				return DataFormat.TEXT;
			}
		}

		String accept = request.getHeader("Accept");
		if (accept != null) {
			String a = accept.toLowerCase();
			if (a.contains("application/xml") || a.contains("text/xml")) {
				return DataFormat.XML;
			}
			if (a.contains("text/plain")) {
				return DataFormat.TEXT;
			}
		}

		return DataFormat.JSON;
	}

	public static String contentType(DataFormat format) {
		switch (format) {
		case XML:
			return "application/xml";
		case TEXT:
			return "text/plain";
		case JSON:
		default:
			return "application/json";
		}
	}

	public static Map<String, String> parsePayload(HttpServletRequest request) throws IOException {
		String contentType = request.getContentType();
		String body = readBody(request);
		if (body.isBlank()) {
			return new HashMap<String, String>();
		}

		if (contentType == null || contentType.isBlank()) {
			return parseJson(body);
		}

		String ct = contentType.toLowerCase();
		if (ct.contains("application/json")) {
			return parseJson(body);
		}
		if (ct.contains("application/xml") || ct.contains("text/xml")) {
			return parseXml(body);
		}
		if (ct.contains("text/plain")) {
			return parseText(body);
		}

		return new HashMap<String, String>();
	}

	public static String getValue(HttpServletRequest request, Map<String, String> payload, String key) {
		String param = request.getParameter(key);
		if (param != null) {
			return param;
		}
		return payload.get(key);
	}

	public static String filmAsJson(Film film) {
		return GSON.toJson(film);
	}

	public static String filmAsXml(Film film) {
		return marshalXml(film, Film.class, "<film/>");
	}

	public static String filmsAsXml(List<Film> films) {
		return marshalXml(new XmlFilmListResponse(films), XmlFilmListResponse.class, "<response/>");
	}

	public static String filmAsText(Film film) {
		if (film == null) {
			return "film=null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("id=").append(film.getId()).append("\n");
		sb.append("title=").append(nullSafe(film.getTitle())).append("\n");
		sb.append("year=").append(film.getYear()).append("\n");
		sb.append("director=").append(nullSafe(film.getDirector())).append("\n");
		sb.append("stars=").append(nullSafe(film.getStars())).append("\n");
		sb.append("review=").append(nullSafe(film.getReview()));
		return sb.toString();
	}

	public static String messageBody(DataFormat format, boolean success, String message) {
		switch (format) {
		case XML:
			return marshalXml(new XmlResponse(success, message), XmlResponse.class, "<response/>");
		case TEXT:
			return "success=" + success + "\nmessage=" + nullSafe(message);
		case JSON:
		default:
			Map<String, Object> response = new LinkedHashMap<String, Object>();
			response.put("success", Boolean.valueOf(success));
			response.put("message", message);
			return GSON.toJson(response);
		}
	}

	private static String readBody(HttpServletRequest request) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = request.getReader();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString().trim();
	}

	private static Map<String, String> parseJson(String body) {
		Map<String, String> result = new HashMap<String, String>();
		try {
			Film film = GSON.fromJson(body, Film.class);
			if (film == null) {
				return result;
			}
			putFilmValues(result, film);
		} catch (JsonSyntaxException ex) {
			return result;
		}
		return result;
	}

	private static Map<String, String> parseXml(String body) {
		Map<String, String> result = new HashMap<String, String>();
		try {
			Film film = unmarshalXml(body, Film.class);
			if (film == null) {
				return result;
			}
			putFilmValues(result, film);
		} catch (JAXBException ex) {
			return result;
		}
		return result;
	}

	private static Map<String, String> parseText(String body) {
		Map<String, String> result = new HashMap<String, String>();
		String[] tokens = body.split("[\\n;&]");
		for (String token : tokens) {
			String t = token.trim();
			if (t.isEmpty()) {
				continue;
			}
			int eq = t.indexOf('=');
			if (eq > 0 && eq < t.length() - 1) {
				String key = t.substring(0, eq).trim();
				String value = t.substring(eq + 1).trim();
				result.put(key, value);
			}
		}
		return result;
	}

	private static String nullSafe(String value) {
		return value == null ? "" : value;
	}

	private static void putFilmValues(Map<String, String> result, Film film) {
		result.put("id", String.valueOf(film.getId()));
		result.put("title", nullSafe(film.getTitle()));
		result.put("year", String.valueOf(film.getYear()));
		result.put("director", nullSafe(film.getDirector()));
		result.put("stars", nullSafe(film.getStars()));
		result.put("review", nullSafe(film.getReview()));
	}

	private static <T> String marshalXml(T value, Class<T> type, String fallback) {
		try {
			JAXBContext context = JAXBContext.newInstance(type);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			StringWriter writer = new StringWriter();
			marshaller.marshal(value, writer);
			return writer.toString();
		} catch (JAXBException ex) {
			return fallback;
		}
	}

	private static <T> T unmarshalXml(String body, Class<T> type) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(type);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		Object value = unmarshaller.unmarshal(new StringReader(body));
		return type.cast(value);
	}

	@XmlRootElement(name = "response")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class XmlResponse {
		@SuppressWarnings("unused")
		private boolean success;
		@SuppressWarnings("unused")
		private String message;

		@SuppressWarnings("unused")
		private XmlResponse() {
		}

		private XmlResponse(boolean success, String message) {
			this.success = success;
			this.message = message;
		}
	}

	@XmlRootElement(name = "response")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class XmlFilmListResponse {
		@SuppressWarnings("unused")
		private boolean success;
		@SuppressWarnings("unused")
		private int count;
		@jakarta.xml.bind.annotation.XmlElementWrapper(name = "data")
		@XmlElement(name = "film")
		private List<Film> films;

		@SuppressWarnings("unused")
		private XmlFilmListResponse() {
		}

		private XmlFilmListResponse(List<Film> films) {
			this.success = true;
			this.count = films == null ? 0 : films.size();
			this.films = films == null ? List.of() : films;
		}
	}
}
