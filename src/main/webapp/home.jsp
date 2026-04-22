<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="model.Film" %>
<%
    if (request.getAttribute("films") == null) {
        response.sendRedirect(request.getContextPath() + "/home");
        return;
    }
%>
<%!
    private String escAttr(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private String escHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Film Library</title>
    <script src="<%= request.getContextPath() %>/js/tailwind-config.js"></script>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/theme-fallback.css">
</head>
<body class="bg-[#121212] min-h-screen text-white w-full">
    <div class="sticky top-0 z-20 bg-[#121212]/95 backdrop-blur border-b border-neutral-800">
        <div class="w-[90vw] mx-auto py-4 flex items-center justify-between gap-4">
            <h1 class="text-2xl font-bold">Dashboard</h1>
            <div class="space-y-1 min-w-40">
                <label class="text-xs text-red-300 uppercase font-medium">API Format</label>
                <select id="apiFormatSelect" class="w-full h-9 rounded border border-neutral-700 bg-[#121212] px-3 text-xs text-neutral-100 outline-none focus:border-red-400">
                    <option value="json">JSON</option>
                    <option value="xml">XML</option>
                    <option value="text">TEXT</option>
                </select>
            </div>
        </div>
    </div>

    <div class="w-[90vw] mx-auto py-8">
        <%
            ArrayList<Film> films = (ArrayList<Film>)request.getAttribute("films");
            if (films == null) films = new ArrayList<>();
            int totalFilms = films.size();
            int totalPages = (int) Math.ceil(totalFilms / 9.0);
            if (totalPages < 1) totalPages = 1;
        %>

        <div class="flex items-center justify-between mb-8 flex-wrap gap-5">
            <div>
                <h1 class="text-4xl font-bold mb-2">Film Library</h1>
                <p class="text-sm text-red-300">Explore our complete collection of cinematographic works</p>
            </div>
            <a href="<%= request.getContextPath() %>/edit-film?id=new" class="bg-red-600 hover:bg-red-700 text-white px-6 py-2 rounded-lg font-medium transition-colors">
                Add New Film
            </a>
        </div>

        <% if (films.isEmpty()) { %>
            <div class="bg-[#1c1b1b] rounded-lg p-12 text-center">
                <h3 class="text-2xl font-semibold text-amber-200 mb-2">No Films Yet</h3>
                <p class="text-neutral-400 mb-6">Start building your film collection by adding your first movie.</p>
                <a href="<%= request.getContextPath() %>/edit-film?id=new" class="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-[#E50914] text-white hover:bg-[#E50914]/80 transition-colors">
                    Add Your First Film
                </a>
            </div>
        <% } else { %>
            <div class="space-y-4">
                <div class="bg-card rounded-lg p-4 space-y-4">
                    <div class="space-y-2">
                        <label class="text-sm text-red-300 uppercase font-medium block">Search Films</label>
                        <input
                            type="text"
                            id="searchInput"
                            class="w-full h-10 rounded border border-neutral-700 bg-transparent px-3 text-sm text-neutral-100 outline-none focus:border-red-400"
                            placeholder="Search by title or year (e.g. A Little Princess, 1995)"
                            onkeyup="updateSearch()"
                        />
                    </div>
                    <p class="text-sm text-neutral-400">
                        Current response format: <span id="currentApiFormat">JSON</span>
                    </p>
                </div>

                <div id="resultsInfo" class="text-sm text-red-300">
                    Showing <span id="startResult">1</span>-<span id="endResult"><%= Math.min(9, totalFilms) %></span>
                    of <span id="totalCount"><%= totalFilms %></span> film<span id="filmCountSuffix"><%= totalFilms == 1 ? "" : "s" %></span>
                    <span id="filteredInfo" class="text-neutral-400"></span>
                </div>

                <div id="filmsGrid" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    <%
                        for (int i = 0; i < films.size(); i++) {
                            Film film = films.get(i);
                            boolean hideInitially = i >= 9;
                    %>
                        <div class="bg-card border border-neutral-800 rounded-lg p-4 flex flex-col transition-all hover:border-red-500 <%= hideInitially ? "hidden" : "" %>"
                            data-id="<%= film.getId() %>"
                            data-title="<%= escAttr(film.getTitle()) %>"
                            data-year="<%= film.getYear() %>"
                            data-director="<%= escAttr(film.getDirector()) %>"
                            data-stars="<%= escAttr(film.getStars()) %>"
                            data-review="<%= escAttr(film.getReview()) %>">
                            
                            <div class="text-xs text-neutral-500 uppercase font-semibold mb-2">ID #<%= film.getId() %></div>
                            <h3 class="text-lg font-semibold mb-2 text-white"><%= escHtml(film.getTitle()) %></h3>
                            
                            <div class="grid grid-cols-3 gap-2 text-xs mb-3">
                                <div>
                                    <div class="text-neutral-500 uppercase">Year</div>
                                    <div class="text-white"><%= film.getYear() %></div>
                                </div>
                                <div class="col-span-2">
                                    <div class="text-neutral-500 uppercase">Director</div>
                                    <div class="text-white truncate"><%= film.getDirector() != null ? escHtml(film.getDirector()) : "N/A" %></div>
                                </div>
                            </div>
                            
                            <p class="text-sm text-neutral-400 mb-4 line-clamp-3"><%= film.getReview() != null ? escHtml(film.getReview()) : "No review." %></p>
                            
                            <div class="flex gap-2 mt-auto">
                                <a href="<%= request.getContextPath() %>/film-detail?id=<%= film.getId() %>" class="flex-1 px-3 py-2 border border-neutral-700 bg-[#1c1b1b] text-center rounded-lg text-sm">View</a>
                                <a href="<%= request.getContextPath() %>/edit-film?id=<%= film.getId() %>" class="flex-1 px-3 py-2 border border-neutral-700 bg-[#1c1b1b] text-center rounded-lg text-sm">Edit</a>
                            </div>
                        </div>
                    <% } %>
                </div>

                <div id="paginationContainer" class="flex justify-center"></div>
            </div>
        <% } %>
    </div>

    <script>window.__ctx = "<%= request.getContextPath() %>";</script>
    <script src="<%= request.getContextPath() %>/js/api-format.js"></script>
    <script src="<%= request.getContextPath() %>/js/home.js"></script>
</body>
</html>