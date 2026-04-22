<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Film" %>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<title>Film Details</title>
	<script src="<%= request.getContextPath() %>/js/tailwind-config.js"></script>
	<script src="https://cdn.tailwindcss.com"></script>
	<link rel="stylesheet" href="<%= request.getContextPath() %>/css/theme-fallback.css">
</head>
<body class="bg-[#121212] min-h-screen text-white w-full">
	<!-- Top sticky header with API format selector -->
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
			String errorMessage = (String)request.getAttribute("errorMessage");
			Film film = (Film)request.getAttribute("film");

			if (errorMessage != null) {
		%>
			<div class="py-8">
				<h1 class="text-3xl font-bold uppercase mb-4 text-red-300"><%= errorMessage %></h1>
				<p class="text-sm text-textMuted mb-6">The film you're looking for could not be found or loaded.</p>
				<a href="<%= request.getContextPath() %>/" class="inline-flex items-center gap-2 px-4 py-2 border border-neutral-700 bg-[#1c1b1b] text-neutral-200 hover:bg-[#2a2a2a] rounded-lg transition-colors text-sm font-medium">
					← Back to Films
				</a>
			</div>
		<%
			} else if (film != null) {
				String[] castMembers = film.getStars() != null ? film.getStars().split(",") : new String[0];
		%>
			<!-- Back Button -->
			<a href="<%= request.getContextPath() %>/" class="inline-flex items-center gap-2 mb-6 px-4 py-2 border border-neutral-700 bg-[#1c1b1b] text-neutral-200 hover:bg-[#2a2a2a] rounded-lg transition-colors text-sm font-medium">
				← Back to All Films
			</a>

			<!-- Film Title -->
			<div class="mb-8">
				<h1 class="text-4xl font-bold uppercase"><%= film.getTitle() %></h1>
			</div>

			<!-- Main Content -->
			<div class="flex items-start gap-8 w-full">
				<!-- Left Content (75%) -->
				<div class="flex flex-col gap-8 w-3/4">
					<!-- Narrative Section -->
					<div class="bg-card rounded-lg p-6">
						<div class="flex flex-col gap-4">
							<h2 class="uppercase font-semibold text-warning text-sm">The Narrative</h2>
							<p class="text-sm text-textSecondary leading-relaxed font-mono">
								<%= film.getReview() != null ? film.getReview() : "No review available" %>
							</p>
						</div>
					</div>

					<!-- Director and Cast Section -->
					<div class="flex gap-8">
						<!-- Director -->
						<div class="bg-card rounded-lg p-6 w-1/2 flex flex-col gap-4">
							<h2 class="uppercase font-semibold text-red-300 text-sm">Director and Creative</h2>
							<div class="flex gap-4">
								<div class="w-12 h-12 rounded-lg bg-gray-600 flex-shrink-0"></div>
								<div class="flex flex-col">
									<p class="text-sm font-mono text-text">
										<%= film.getDirector() != null ? film.getDirector() : "Unknown Director" %>
									</p>
									<p class="text-xs text-red-300">Creative Director</p>
								</div>
							</div>
						</div>

						<!-- Principal Cast -->
						<div class="bg-card rounded-lg p-6 w-1/2 flex flex-col gap-4">
							<h2 class="uppercase font-semibold text-warning text-sm">Principal Cast</h2>
							<div class="space-y-1">
								<%
									String stars = film.getStars();
									if (stars != null && !stars.isEmpty()) {
										String[] actors = stars.split(",");
										for (int i = 0; i < actors.length; i++) {
											String actor = actors[i].trim();
											if (!actor.isEmpty()) {
								%>
									<p class="text-xs font-mono text-red-300">
										<%= actor %><%= i < actors.length - 1 ? "," : "" %>
									</p>
								<%
											}
										}
									} else {
								%>
									<p class="text-xs text-textMuted">No cast information available</p>
								<%
									}
								%>
							</div>
						</div>
					</div>
				</div>

				<!-- Right Sidebar (25%) -->
				<div class="bg-cardLight rounded-lg p-6 w-1/4 space-y-6">
					<div>
						<h2 class="uppercase font-semibold text-warning text-sm mb-4">Film Details</h2>
						<p class="text-xs text-textMuted">
							Update metadata or remove this entry from the library archives permanently.
						</p>
					</div>

					<!-- Action Buttons -->
					<div class="flex flex-col gap-3">
						<a href="<%= request.getContextPath() %>/edit-film?id=<%= film.getId() %>" class="flex items-center justify-center gap-2 px-4 py-2 bg-[#E50914] hover:bg-[#E50914]/80 text-white rounded-lg font-medium transition-colors text-sm h-10">
							<span>✏️</span>
							<span>Edit Film Details</span>
						</a>
						
						<form method="post" action="<%= request.getContextPath() %>/edit-film" style="display: inline;">
							<input type="hidden" name="action" value="delete" />
							<input type="hidden" name="redirect" value="home" />
							<input type="hidden" name="id" value="<%= film.getId() %>" />
							<button type="submit" class="w-full flex items-center justify-center gap-2 px-4 py-2 bg-red-900/30 hover:bg-red-900/50 border border-red-700 text-red-300 rounded-lg font-medium transition-colors text-sm h-10" onclick="return confirm('Are you sure you want to delete this film?');">
								<span>🗑️</span>
								<span>Delete Film</span>
							</button>
						</form>
					</div>

					<!-- Film Metadata -->
					<div class="space-y-3 pt-4 border-t border-border">
						<div class="flex justify-between items-center">
							<p class="text-xs text-red-300">Film ID</p>
							<p class="text-xs font-mono text-textMuted"><%= film.getId() %></p>
						</div>
						
						<div class="flex justify-between items-center">
							<p class="text-xs text-red-300">Release Year</p>
							<p class="text-xs font-mono text-textMuted"><%= film.getYear() %></p>
						</div>
					</div>
				</div>
			</div>
		<%
			} else {
		%>
			<div class="py-8">
				<h1 class="text-3xl font-bold uppercase mb-4">Loading Film...</h1>
				<p class="text-sm text-textMuted">Please wait while we fetch the film details.</p>
			</div>
		<%
			}
		%>
	</div>
	<script src="<%= request.getContextPath() %>/js/api-format.js"></script>
</body>
</html>
