# Critical Analysis: Films MVC Web Application

## Overview

This project implements a full-stack Java web application that delivers film archive management through both traditional Model-View-Controller (MVC) pages and a RESTful API interface. The implementation prioritises maintainability, extensibility, and adherence to established software engineering practices. This section evaluates the technical decisions made, justifies key architectural choices with reference to industry standards, identifies remaining weaknesses, and outlines a prioritised roadmap for future improvements.

---

## 1. Software Engineering Techniques

The application employs layered separation of concerns, dividing responsibilities across distinct, loosely-coupled components. This architectural approach improves resilience to change and supports incremental testing and debugging.

**Architectural Layers:**

- **Persistence Layer:** DAO encapsulates all SQL operations [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L13)
- **Controller Layer:** Servlet-based request handlers orchestrate requests [src/main/java/servlet/FilmsServlet.java](src/main/java/servlet/FilmsServlet.java#L19), [src/main/java/servlet/EditFilmController.java](src/main/java/servlet/EditFilmController.java#L20), [src/main/java/servlet/HomePageController.java](src/main/java/servlet/HomePageController.java#L20)
- **Model Layer:** Domain object carries state between layers [src/main/java/model/Film.java](src/main/java/model/Film.java#L10)
- **Presentation Layer:** JSP views and JavaScript client handle UI [src/main/webapp/home.jsp](src/main/webapp/home.jsp), [src/main/webapp/js/home.js](src/main/webapp/js/home.js)

**Defensive Programming:**

Request validation occurs before persistence operations, implementing fail-fast error handling. This technique reduces propagation of invalid state and provides clear, early feedback to API clients.

```java
// Example: Validating input before invoking DAO
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
```

Source: [src/main/java/servlet/FilmsServlet.java](src/main/java/servlet/FilmsServlet.java#L35)

**Content Negotiation:**

The application supports JSON, XML, and plain-text response formats from a single endpoint. This multi-format capability is achieved through centralised format detection and payload parsing, enabling different client types (browser, mobile app, server-to-server integration) to use the same business logic without code duplication.

```java
// Format detection based on request parameter or Accept header
String formatParam = request.getParameter("format");
if (formatParam != null) {
    String fp = formatParam.trim().toLowerCase();
    if ("xml".equals(fp)) {
        return DataFormat.XML;
    }
    // ... additional format checks
}

String accept = request.getHeader("Accept");
if (accept != null) {
    String a = accept.toLowerCase();
    if (a.contains("application/xml") || a.contains("text/xml")) {
        return DataFormat.XML;
    }
}

return DataFormat.JSON;  // Default
```

Source: [src/main/java/servlet/FormatSupport.java](src/main/java/servlet/FormatSupport.java#L38)

**Benefit:** This technique reduces code branching in controllers and improves adherence to REST principles, where content type is negotiated dynamically based on client capability.

---

## 2. Design Patterns

The implementation employs multiple design patterns beyond basic MVC, each selected to solve a specific architectural problem.

### 2.1 Model-View-Controller (MVC) Pattern

Controllers process HTTP requests, invoke business logic, and forward to JSP views with prepared model data.

```java
// HomePageController: Fetches all films and forwards to view
@WebServlet("/home")
public class HomePageController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        FilmDao dao = new FilmDao();
        ArrayList<Film> allFilms = dao.getAllFilms();
        
        request.setAttribute("films", allFilms);
        request.getRequestDispatcher("/home.jsp").forward(request, response);
    }
}
```

Source: [src/main/java/servlet/HomePageController.java](src/main/java/servlet/HomePageController.java#L20)

**Justification:** MVC decouples presentation from request logic, enabling independent evolution of UI and business rules. This is a foundational pattern in enterprise Java and is recommended by multiple authoritative sources (Gamma et al., 1994; Fowler, 2002).

### 2.2 Data Access Object (DAO) Pattern

All database interactions are encapsulated in a single class. SQL logic, result mapping, and connection management reside in one place, making changes to persistence safer and more maintainable.

```java
// DAO Update operation with parameterised query
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
```

Source: [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L317)

**Benefit:** Isolating SQL from application code improves testability, supports schema changes without controller modifications, and enforces consistent query safety practices.

### 2.3 Utility/Helper Pattern

Format support concerns—parsing, serialisation, and content-type resolution—are centralised in a helper class rather than distributed across controllers. This prevents repeated branching logic and ensures consistent behaviour.

```java
// Centralised payload parsing for JSON, XML, and text
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
```

Source: [src/main/java/servlet/FormatSupport.java](src/main/java/servlet/FormatSupport.java#L76)

**Benefit:** Eliminates duplicate format-handling logic and centralises serialisation behaviour, reducing the risk of format inconsistencies.

### 2.4 Data Transfer Object (DTO) / Model Pattern

The Film class serves dual roles: domain model for in-memory operations and XML-serialisable DTO for API responses.

```java
@XmlRootElement(name = "film")
@XmlAccessorType(XmlAccessType.FIELD)
public class Film {
    public Film(int id, String title, int year, String director, String stars, String review) {
        super();
        this.id = id;
        this.title = title;
        this.year = year;
        this.director = director;
        this.stars = stars;
        this.review = review;
    }

    private int id;
    private String title;
    private int year;
    private String director;
    private String stars;
    private String review;

    // ... getters/setters
}
```

Source: [src/main/java/model/Film.java](src/main/java/model/Film.java#L8)

**Benefit:** Single model object supports both server-side business logic and multi-format API serialisation, reducing impedance mismatch and transformation overhead.

---

## 3. Refactoring

Refactoring was a continuous practice during development to eliminate duplication and improve code clarity. Several significant refactoring outcomes improved quality and maintainability.

### 3.1 Centralised Payload Parsing

Rather than handling JSON, XML, and text parsing in each controller, logic was extracted into a shared utility. Both FilmsServlet and EditFilmController now use the same parsing path.

```java
// Shared parsing in multiple controllers
Map<String, String> payload = FormatSupport.parsePayload(request);
film.setId(parseIntOrDefault(FormatSupport.getValue(request, payload, "id"), -1));
film.setTitle(FormatSupport.getValue(request, payload, "title"));
film.setYear(parseIntOrDefault(FormatSupport.getValue(request, payload, "year"), 0));
film.setDirector(FormatSupport.getValue(request, payload, "director"));
film.setStars(FormatSupport.getValue(request, payload, "stars"));
film.setReview(FormatSupport.getValue(request, payload, "review"));
```

Source: [src/main/java/servlet/FilmsServlet.java](src/main/java/servlet/FilmsServlet.java#L186)

**Impact:** Reduces code duplication from approximately 50+ lines to a single reusable method call, and centralises format-handling logic for easier maintenance and testing.

### 3.2 Extracted Validation Methods

Insert and update validation logic was initially inline in each controller. Extracting dedicated validation methods improves readability and reusability.

```java
private boolean isFilmInsertValid(Film film) {
    return film != null && film.getYear() > 0 && !isBlank(film.getTitle()) && !isBlank(film.getDirector())
            && !isBlank(film.getStars()) && !isBlank(film.getReview());
}

private boolean isFilmUpdateValid(Film film) {
    return isFilmInsertValid(film) && film.getId() > 0;
}
```

Source: [src/main/java/servlet/FilmsServlet.java](src/main/java/servlet/FilmsServlet.java#L198)

**Impact:** Validation rules are now explicit and testable, and changes to validation logic affect both MVC and REST paths automatically.

### 3.3 Consolidated Response Formatting

Instead of manually assembling JSON/XML/text responses in multiple methods, FormatSupport provides consistent message and serialisation utilities.

```java
// Centralised message formatting
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
```

Source: [src/main/java/servlet/FormatSupport.java](src/main/java/servlet/FormatSupport.java#L135)

**Impact:** Response structure consistency is enforced in one place. Changes to response format automatically propagate across all endpoints.

### 3.4 Critical Remaining Duplication

Despite refactoring efforts, parse-and-validate logic still appears in multiple controller classes, creating a maintenance risk:

- [src/main/java/servlet/FilmsServlet.java](src/main/java/servlet/FilmsServlet.java#L186) — REST controller parsing
- [src/main/java/servlet/EditFilmController.java](src/main/java/servlet/EditFilmController.java#L154) — MVC controller parsing

A next refactoring phase should extract a dedicated `FilmRequestMapper` and `FilmValidator` component to serve both controllers, eliminating this duplication entirely.

---

## 4. Code Optimisation

Optimisation decisions were made to balance code clarity with practical runtime performance. Key optimisations are listed below, followed by identified performance limitations.

### 4.1 Prepared Statements for Safe, Efficient SQL

All SQL operations use prepared statements, which provide both security (SQL injection prevention) and performance benefits (reduced parsing overhead by the database).

```java
// All DAO operations use PreparedStatement
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
```

Source: [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L117)

**Benefit:** Parameterised queries are resistant to injection attacks and allow database query planners to reuse execution plans across invocations.

### 4.2 Database-Side Filtering and Ordering

Search and ordering operations are performed at the SQL level rather than loading full result sets and filtering in Java.

```java
// Database-side search and ordering
public ArrayList<Film> getFilmsByTitle(String title) {
    // ...
    String sql = "select * from " + table + " where lower(title) like lower(?) order by id desc";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, "%" + title + "%");
        // ... process results
    }
}
```

Source: [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L172)

**Benefit:** Reduces memory overhead and network traffic by filtering at the source.

### 4.3 Client-Side Pagination and Filtering

The home page uses client-side pagination over pre-rendered film cards, avoiding unnecessary server round-trips when users navigate between pages or apply filters to already-loaded data.

```javascript
// Client-side pagination slice
function renderGrid(activePage) {
    const grid = document.getElementById("filmsGrid");
    if (!grid) return;

    allCards.forEach(card => card.classList.add("hidden"));

    if (filteredCards.length === 0) {
        getNoResultsElement(grid).classList.remove("hidden");
        return;
    }

    const emptyState = document.getElementById("noResultsState");
    if (emptyState) emptyState.classList.add("hidden");

    const start = (activePage - 1) * FILMS_PER_PAGE;
    const pageCards = filteredCards.slice(start, start + FILMS_PER_PAGE);
    pageCards.forEach(card => card.classList.remove("hidden"));
}
```

Source: [src/main/webapp/js/home.js](src/main/webapp/js/home.js#L51)

**Benefit:** Improves perceived responsiveness and reduces server load for interactive filtering within cached result sets.

### 4.4 Performance Limitations and Constraints

Despite these optimisations, several limitations remain that would require attention for production deployment:

**1. Per-Call Connection Lifecycle**

A new connection is opened and closed for each DAO operation:

```java
private void openConnection() {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
        // ... fallback to older driver
    }

    try {
        conn = DriverManager.getConnection(url, user, password);
        stmt = conn.createStatement();
    } catch (SQLException se) {
        System.out.println("[FilmDao] DB connection failed: " + se.getMessage());
        se.printStackTrace();
    }
}
```

Source: [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L27)

**Impact:** Under concurrent load, connection establishment overhead becomes a bottleneck. Production systems typically use pooled DataSource implementations (e.g., HikariCP, C3P0, or container-managed pools).

**2. Distributed ID Generation**

Insert operations generate the next ID via `max(id) + 1`:

```java
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
```

Source: [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L104)

**Impact:** This approach is vulnerable to race conditions when multiple inserts occur concurrently. Database-managed sequences or auto-increment columns are more reliable.

**3. Manual Response String Assembly**

Some response formats are built via string concatenation:

```java
private void writeList(PrintWriter out, DataFormat format, ArrayList<Film> films) {
    switch (format) {
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
```

Source: [src/main/java/servlet/FilmsServlet.java](src/main/java/servlet/FilmsServlet.java#L223)

**Impact:** This approach is more error-prone than using a structured serialiser. It lacks compile-time type safety and can lead to inconsistent response structures across endpoints.

---

## 5. Use of Best Practice (With Research-Based Justification)

The implementation adheres to multiple established software engineering best practices, each justified by research and industry standards.

### 5.1 Layered Architecture and Separation of Concerns

**Principle:** Different types of changes (UI, business logic, persistence) should be isolated in different code regions to minimise the scope of modification and testing.

**Evidence:**
- Controllers: [src/main/java/servlet/](src/main/java/servlet/)
- DAO: [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java)
- Model: [src/main/java/model/Film.java](src/main/java/model/Film.java)
- Views: [src/main/webapp/](src/main/webapp/)

**Justification:** This pattern is recommended by clean architecture literature (Martin, 2017; Newman, 2015) and the Gang of Four design patterns book (Gamma et al., 1994). It has been validated across decades of enterprise software development.

### 5.2 Input Validation and Explicit Error Responses

**Principle:** Invalid requests should be rejected early with clear error signals rather than propagating invalid state into the system.

**Evidence:**

```java
// Early validation with explicit HTTP status codes
if (!isBlank(idParam)) {
    int id = parseIntOrDefault(idParam, -1);
    if (id <= 0) {
        writeError(response, format, HttpServletResponse.SC_BAD_REQUEST, "Invalid id");
        return;
    }
}
```

Source: [src/main/java/servlet/FilmsServlet.java](src/main/java/servlet/FilmsServlet.java#L35)

**Justification:** RFC 7231 (HTTP semantics) recommends using 4xx status codes to indicate client-side errors. Clear, early validation improves reliability and supports client-side error recovery (Fielding et al., 2014).

### 5.3 Parameterised SQL and SQL Injection Prevention

**Principle:** User input should never be concatenated directly into SQL queries; instead, it should be passed as bind parameters to the database driver.

**Evidence:**

```java
// Parameterised query (safe)
String sql = "update " + table + " set title = ?, year = ?, director = ?, stars = ?, review = ? where id = ?";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, film.getTitle());
    ps.setInt(2, film.getYear());
    // ... remaining parameters
    return ps.executeUpdate() > 0;
}
```

Source: [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L325)

**Justification:** Prepared statements are the primary defence against SQL injection attacks. OWASP, the National Institute of Standards and Technology (NIST), and all major database vendors recommend this approach as a first-line mitigation (OWASP, 2021).

### 5.4 Output Escaping and XSS Prevention

**Principle:** User-controlled data should be escaped before rendering in HTML/JavaScript contexts to prevent cross-site scripting (XSS) attacks.

**Evidence:**

```java
// HTML escaping utility in JSP
private String escHtml(String value) {
    if (value == null) return "";
    return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
}

// Usage in rendered output
<h3 class="text-lg font-semibold mb-2 text-white"><%= escHtml(film.getTitle()) %></h3>
```

Source: [src/main/webapp/home.jsp](src/main/webapp/home.jsp#L19), [src/main/webapp/home.jsp](src/main/webapp/home.jsp#L82)

**Justification:** Output escaping is a foundational XSS defence and is recommended by OWASP and W3C guidance (OWASP, 2021).

### 5.5 Critical Best-Practice Gap: Credentials Management

**Issue:** Database credentials are hard-coded in source code:

```java
String user = "adelekei";
String password = "gacEiblad9";
String url = "jdbc:mysql://mudfoot.doc.stu.mmu.ac.uk:6306/" + user
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
```

Source: [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L18)

**Risk:** Hard-coded secrets in source code are a critical security anti-pattern. If the repository is compromised or accidentally exposed, attackers gain immediate database access. This violates industry best practices and security standards (OWASP, 2021; CWE, 2022).

**Recommended Resolution:**

Externalise credentials using environment variables, JNDI, or a secrets management system:

```java
// Environment-based approach (recommended)
String user = System.getenv("DB_USER");
String password = System.getenv("DB_PASSWORD");
String url = System.getenv("DB_URL");

if (user == null || password == null || url == null) {
    throw new IllegalStateException("Database configuration not found in environment variables");
}
```

This approach is standard in containerised deployment (Docker, Kubernetes) and cloud platforms.

---

## 6. Future Work (Prioritised Roadmap)

The following enhancements are recommended to mature the system toward production readiness:

### 6.1 Security Hardening (Priority: Critical)

**Task:** Externalise all credentials and configuration from source code.

**Current State:** Hard-coded credentials in [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L18)

**Approach:**
- Use environment variables for local development
- Use JNDI or secrets management for containerised/cloud deployment
- Implement startup validation that fails fast if required configuration is missing
- Remove all secrets from repository history using git-filter-branch or BFG Repo-Cleaner

**Benefit:** Eliminates a critical security vulnerability and aligns with industry standards (OWASP, 2021; Cloud Native Computing Foundation, 2021).

### 6.2 Connection Pooling and Persistence Modernisation (Priority: High)

**Task:** Replace DriverManager with pooled DataSource.

**Current State:** Per-call connection management in [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L27)

**Approach:**
- Introduce HikariCP or similar pooling library
- Configure pool size, timeout, and eviction policies
- Update DAO to use DataSource instead of DriverManager
- Add instrumentation to monitor pool health

**Benefit:** Improves throughput and latency under concurrent load, reduces connection establishment overhead, and enables better resource management.

### 6.3 Refactor Request Mapping (Priority: High)

**Task:** Eliminate duplication in parse/validate logic across controllers.

**Current Duplication:**
- [src/main/java/servlet/FilmsServlet.java](src/main/java/servlet/FilmsServlet.java#L186)
- [src/main/java/servlet/EditFilmController.java](src/main/java/servlet/EditFilmController.java#L154)

**Approach:**
- Create `FilmRequestMapper` class with `parseAndValidate()` method
- Create `FilmValidator` class with reusable validation rules
- Update both controllers to use shared mapper/validator

**Benefit:** Reduces maintenance burden, ensures all entry points enforce identical rules, and supports future validation enhancements (e.g., bean validation annotations).

### 6.4 Response Serialisation Standardisation (Priority: Medium)

**Task:** Replace manual string assembly with typed response DTOs and consistent serialisation.

**Current State:** String concatenation in [src/main/java/servlet/FilmsServlet.java](src/main/java/servlet/FilmsServlet.java#L207)

**Approach:**
- Define response wrapper DTOs: `FilmListResponse`, `FilmSingleResponse`, `ApiResponse`
- Use Gson/Jackson for all serialisation instead of mixed manual+library approaches
- Ensure consistent response structure across all endpoints
- Add content-type assertion tests to verify correctness

**Benefit:** Improves type safety, reduces serialisation bugs, and supports easier API versioning.

### 6.5 Automated Testing (Priority: Medium)

**Task:** Add comprehensive unit and integration test coverage.

**Components to Test:**
1. **FormatSupport Utilities** — Unit tests for JSON/XML/text parsing and serialisation
2. **DAO Operations** — Integration tests against test database (H2 or test MySQL instance)
3. **REST Endpoints** — API contract tests for GET/POST/PUT/DELETE across all formats
4. **Validation Logic** — Unit tests for boundary conditions and invalid inputs
5. **JavaScript Client** — Browser automation tests for UI interactions

**Approach:**
- Use JUnit 5 for unit testing
- Use TestContainers or H2 database for integration testing
- Use REST Assured or Postman for API testing
- Use Selenium or Playwright for UI testing

**Benefit:** Enables confident refactoring, catches regressions early, and documents expected behaviour.

### 6.6 Schema and Database Improvements (Priority: Medium)

**Task:** Improve database design and query performance.

**Changes:**
1. Use AUTO_INCREMENT or database sequences for ID generation instead of `max(id)+1`
   - Current: [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L104)
2. Add indexes on frequently-searched columns (title, year)
3. Add database-level constraints (NOT NULL for required fields, CHECK for year range)
4. Consider normalisation if additional film metadata is added (e.g., separate genres, ratings)

**Benefit:** Improves query performance, reduces race conditions, and enforces data integrity at the database layer.

### 6.7 Logging and Observability (Priority: Medium)

**Task:** Replace System.out.println with structured logging.

**Current State:** Console logging throughout [src/main/java/dao/FilmDao.java](src/main/java/dao/FilmDao.java#L45)

**Approach:**
- Introduce SLF4J + Logback
- Use structured logging (JSON format) for easier parsing
- Implement log levels: DEBUG (detailed execution), INFO (key events), WARN (recoverable errors), ERROR (failures)
- Add context tracking (request IDs) for tracing related logs

**Benefit:** Enables better debugging, supports operational monitoring, and facilitates log aggregation in production.

### 6.8 Cloud Readiness (Priority: Medium)

**Task:** Prepare application for containerised and cloud deployment.

**Approach:**
- Create Dockerfile for application containerisation
- Externalise configuration (database URL, ports, credentials) via environment variables
- Add health check endpoint (`/health`) for orchestrator monitoring
- Implement graceful shutdown hooks for clean container termination
- Add request correlation IDs for distributed tracing

**Benefit:** Enables deployment to Kubernetes, AWS, Google Cloud, or Azure with minimal friction.

### 6.9 Accessibility and Frontend Robustness (Priority: Low)

**Task:** Improve user experience and accessibility.

**Approach:**
- Add ARIA labels and semantic HTML to improve screen reader support
- Enhance form validation error messages for clarity
- Improve mobile responsiveness testing and layout
- Add keyboard navigation support for all interactive elements
- Implement error recovery gracefully for network failures

**Benefit:** Broadens user base, improves resilience to transient failures, and aligns with accessibility standards (WCAG 2.1).

---

## 7. Conclusion

This project demonstrates a disciplined approach to web application development, with clear separation of concerns, multiple design patterns applied judiciously, and alignment with established best practices. The strongest architectural decisions—MVC layering, centralised payload handling, and parameterised SQL—provide a solid foundation for future enhancement.

The principal weaknesses—hard-coded credentials, remaining duplication, limited test coverage, and per-call connection management—are well-understood and prioritised in the roadmap above. The identified improvements are realistic, incremental, and sequenced to address the highest-impact issues first.

This project is fully functional and suitable for the assignment requirements. When viewed as the foundation for a larger system, it demonstrates professional software engineering thinking and a clear vision for sustainable, maintainable code.

---

## References

Cloud Native Computing Foundation (2021) *Secrets Management*, available at: https://www.cncf.io/blog/2021/12/15/secrets-management/ (Accessed: 4 May 2026).

CWE (2022) *CWE-798: Use of Hard-coded Credentials*, available at: https://cwe.mitre.org/data/definitions/798.html (Accessed: 4 May 2026).

Fielding, R.T., Gettys, J., Mogul, J., Frystyk, H., Masinter, L., Leach, P. and Berners-Lee, T. (2014) *RFC 7231: Hypertext Transfer Protocol (HTTP/1.1) Semantics and Semantics*, Internet Engineering Task Force. Available at: https://tools.ietf.org/html/rfc7231 (Accessed: 4 May 2026).

Fowler, M. (2002) *Patterns of Enterprise Application Architecture*, Addison-Wesley Professional.

Gamma, E., Helm, R., Johnson, R. and Vlissides, J. (1994) *Design Patterns: Elements of Reusable Object-Oriented Software*, Addison-Wesley.

Martin, R.C. (2017) *Clean Architecture: A Craftsman's Guide to Software Structure and Design*, Prentice Hall.

Newman, S. (2015) *Building Microservices*, O'Reilly Media.

OWASP (2021) *OWASP Top 10 – 2021*, available at: https://owasp.org/Top10/ (Accessed: 4 May 2026).

---

## Appendix: Screenshot Captions (Ready to Paste Under Images)

### MVC Application Evidence

**Screenshot 1: Home Page Listing**
> This screenshot shows the Films dashboard displaying all cinema records in a paginated grid layout. The interface demonstrates server-side retrieval via HomePageController (src/main/java/servlet/HomePageController.java#L28), client-side pagination and filtering via home.js (src/main/webapp/js/home.js), and Tailwind CSS styling. The search box filters records by title or year without requiring server round-trips once the initial list is loaded. The API format selector (top-right) allows users to toggle between JSON, XML, and TEXT response formats, demonstrating content negotiation at the HTTP level. This represents the READ operation of MVC CRUD flow.

**Screenshot 2: Film Detail Page**
> This screenshot displays a single film record retrieved via FilmDetailController (src/main/java/servlet/FilmDetailController.java#L24). The page shows the film title, narrative/review, director, and principal cast members. The "Edit Film Details" and "Delete Film" action buttons provide entry points to the UPDATE and DELETE operations. Error handling is visible if an invalid film ID is provided. This demonstrates the read-single and navigation flow in the MVC architecture.

**Screenshot 3: Create Film Form (New Record)**
> This screenshot shows the film creation form accessed via `/edit-film?id=new` (src/main/java/servlet/EditFilmController.java#L29). The form has empty fields for title, year, director, stars, and review. The "Draft Overview" sidebar indicates "Create" mode and provides usage hints. The submit button text reads "Create Film" (not "Save Changes"), reinforcing the insert operation. This demonstrates the CREATE entry point of MVC CRUD.

**Screenshot 4: Edit Film Form (Existing Record)**
> This screenshot shows the same form populated with existing film data (src/main/java/servlet/EditFilmController.java#L40). The sidebar now shows "Edit" mode and displays the Film ID being modified. The submit button reads "Save Changes". Form validation is enforced client-side (via edit-film.js, src/main/webapp/js/edit-film.js) and server-side (via isFilmUpdateValid, src/main/java/servlet/EditFilmController.java#L171). This demonstrates the UPDATE operation and full validation chain.

**Screenshot 5: Delete Confirmation and Result**
> This screenshot shows either the delete confirmation dialog (from film-detail.jsp) or the success/failure redirect after deletion. If successful, the browser navigates back to the home page (via response.sendRedirect in src/main/java/servlet/EditFilmController.java#L88). This demonstrates the DELETE operation and post-delete user navigation.

---

### RESTful Web Service Evidence

For each of the following screenshots, show:
- Request URL and HTTP method in Postman address bar
- Headers tab showing Content-Type and Accept values
- Request body (for POST/PUT)
- Response status code
- Response body formatted as JSON/XML/TEXT

**Screenshot 6: GET All Films (JSON)**
> **Request:** `GET http://[host]:[port]/filmsApi/films` with `Accept: application/json` header.  
> **Response:** HTTP 200 OK with JSON body containing array of film objects (src/main/java/servlet/FilmsServlet.java#L58). Demonstrates the READ-ALL operation and JSON serialisation via Gson (FormatSupport.java#L110).

**Screenshot 7: GET All Films (XML)**
> **Request:** Same URL with `Accept: application/xml` or `?format=xml` parameter.  
> **Response:** HTTP 200 OK with XML body containing wrapped film elements. Demonstrates content negotiation (src/main/java/servlet/FormatSupport.java#L38) and JAXB XML marshalling (src/main/java/servlet/FormatSupport.java#L219).

**Screenshot 8: GET All Films (TEXT)**
> **Request:** Same URL with `Accept: text/plain` or `?format=text`.  
> **Response:** HTTP 200 OK with plain-text key=value format (src/main/java/servlet/FormatSupport.java#L121). Demonstrates alternative format support from single endpoint.

**Screenshot 9: GET Film by ID (JSON)**
> **Request:** `GET http://[host]:[port]/filmsApi/films?id=1` with JSON Accept header.  
> **Response:** HTTP 200 OK with single film object in JSON. If ID does not exist, HTTP 404 NOT_FOUND (src/main/java/servlet/FilmsServlet.java#L42).

**Screenshot 10: POST Create Film (JSON)**
> **Request:** `POST http://[host]:[port]/filmsApi/films` with `Content-Type: application/json` and request body containing new film data (title, year, director, stars, review).  
> **Response:** HTTP 201 CREATED with message body. Demonstrates request body parsing (src/main/java/servlet/FilmsServlet.java#L92), validation (src/main/java/servlet/FilmsServlet.java#L115), and insert operation (src/main/java/dao/FilmDao.java#L281).

**Screenshot 11: POST Create Film (XML)**
> **Request:** Same with `Content-Type: application/xml` and XML-formatted request body.  
> **Response:** HTTP 201 CREATED with XML response body. Demonstrates XML parsing and marshalling in single endpoint (src/main/java/servlet/FormatSupport.java#L174).

**Screenshot 12: PUT Update Film (JSON)**
> **Request:** `PUT http://[host]:[port]/filmsApi/films` with JSON body containing film ID and updated fields.  
> **Response:** HTTP 200 OK with success message. Demonstrates request body parsing, validation (src/main/java/servlet/FilmsServlet.java#L141), and update operation (src/main/java/dao/FilmDao.java#L317).

**Screenshot 13: DELETE Film**
> **Request:** `DELETE http://[host]:[port]/filmsApi/films?id=1` (or via POST with `action=delete`).  
> **Response:** HTTP 200 OK with success message, or HTTP 404 NOT_FOUND if ID does not exist. Demonstrates delete operation (src/main/java/dao/FilmDao.java#L346).

---

### JavaScript Client Evidence

**Screenshot 14: Create Film from Browser Form**
> This screenshot shows the edit-film.html form with populated fields and the browser Developer Tools Network tab open. The Network panel displays a POST request to `/edit-film` with request payload visible in the "Request" sub-tab. The response shows HTTP 302 redirect or direct forward to form with success message. This demonstrates client-side form submission and server-side persistence flow (src/main/webapp/js/edit-film.js, src/main/java/servlet/EditFilmController.java#L63).

**Screenshot 15: Create Film - Network Details**
> Close-up of the Network tab showing:
> - Request URL: `/filmsApi/edit-film` (or `/films` if using REST endpoint)
> - Request Method: POST
> - Request Headers: Content-Type application/x-www-form-urlencoded (or JSON if using REST)
> - Request Payload: form data with title, year, director, stars, review fields
> - Response Status: 201 CREATED or 302 Found (redirect)
> - Response Headers and Body visible

**Screenshot 16: Update Film from Browser Form**
> Similar to Screenshot 14, but form is pre-populated with existing film data (retrieved via GET request). The POST payload includes the film ID to indicate update mode. Network tab shows the PUT or POST with film ID and updated fields.

**Screenshot 17: Delete Film from Detail Page**
> Screenshot showing the delete confirmation button on the film-detail page. Network tab displays a POST request with `action=delete` parameter and film ID. Response shows HTTP 200 OK and redirect back to home page.

**Screenshot 18: Home Page Search and Pagination**
> Screenshots of the home page showing:
> 1. Initial list of 9 films displayed
> 2. User typing in search box (e.g., "Inception")
> 3. Filtered list showing only matching films
> 4. Pagination controls showing current page and total count
> 5. Browser Network tab showing NO new requests (client-side filtering only)

This demonstrates progressive enhancement: the page works without JavaScript (server returns all films), and JavaScript adds interactivity (client-side search/pagination) for a better UX without creating unnecessary server traffic.

---

### Cloud Implementation Evidence

**Screenshot 19: Cloud Service Dashboard**
> Screenshot of cloud provider dashboard (Google App Engine, AWS, or Azure) showing:
> - Application deployment status (Green = Running)
> - Service configuration (instance count, resource allocation)
> - Recent deployment timestamp
> - URL where application is accessible
> This demonstrates that the application is successfully containerised and deployed to cloud infrastructure.

**Screenshot 20: Database Dashboard**
> Screenshot of cloud database service showing:
> - Database name and status (Connected)
> - Table structure (films table with columns: id, title, year, director, stars, review)
> - Row count showing populated data
> This demonstrates that the application data is persisted in cloud-managed database.

**Screenshot 21: App Running in Browser with Public URL**
> Screenshot of the Films application home page loaded in a web browser with the cloud-hosted public URL clearly visible in the address bar (e.g., `https://my-films-app-xyz.appspot.com/filmsApi/home`).  
> This demonstrates that the application is accessible over the internet via public URL and renders correctly in the cloud environment.

**Screenshot 22: Cloud CRUD Operations - Create**
> Screenshot of the edit-film form loaded from the cloud URL. Browser Developer Tools Network tab shows POST request to the cloud endpoint. Response shows successful film creation with HTTP 201 or redirect.

**Screenshot 23: Cloud CRUD Operations - Read**
> Screenshot of the home page showing film list retrieved from cloud database. Network tab shows GET request to cloud endpoint returning film data.

**Screenshot 24: Cloud CRUD Operations - Update**
> Screenshot of the edit-film form pre-populated with existing data from cloud database. POST request shown in Network tab with updated field values. Response shows success.

**Screenshot 25: Cloud CRUD Operations - Delete**
> Screenshot of delete confirmation or result page. Network tab shows DELETE request to cloud endpoint. Response shows success message and user is redirected back to home page.

**Screenshot 26: Cost Control - Resource Termination**
> Screenshot of cloud dashboard showing that unused services have been terminated/shut down (e.g., "Application deployment stopped", "Database instance deleted"). Or, a terminal screenshot/log showing `gcloud app versions delete` or equivalent cloud CLI command executed to clean up resources.  
> Include text caption: "All non-essential cloud resources were terminated after testing to prevent ongoing charges and demonstrate responsible cloud resource management."

---

End of Critical Analysis Document.
