<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Film" %>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<title>Edit Film</title>
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
			String successMessage = (String)request.getAttribute("successMessage");
			Film film = (Film)request.getAttribute("film");
			Boolean isCreateMode = (Boolean)request.getAttribute("isCreateMode");
			Integer filmId = (Integer)request.getAttribute("filmId");
			
			if (isCreateMode == null) isCreateMode = false;
			if (filmId == null) filmId = -1;

			if (errorMessage != null) {
		%>
			<div class="py-8">
				<h1 class="text-3xl font-bold uppercase mb-4 text-red-300"><%= errorMessage %></h1>
				<p class="text-sm text-textMuted mb-6">Unable to load the film details. Please try again.</p>
				<a href="<%= request.getContextPath() %>/" class="inline-flex items-center gap-2 px-4 py-2 border border-neutral-700 bg-[#1c1b1b] text-neutral-200 hover:bg-[#2a2a2a] rounded-lg transition-colors text-sm font-medium">
					← Back
				</a>
			</div>
		<%
			} else {
				String title = isCreateMode ? "" : (film != null && film.getTitle() != null ? film.getTitle() : "");
				int year = isCreateMode ? java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) : (film != null ? film.getYear() : 0);
				String director = isCreateMode ? "" : (film != null && film.getDirector() != null ? film.getDirector() : "");
				String stars = isCreateMode ? "" : (film != null && film.getStars() != null ? film.getStars() : "");
				String review = isCreateMode ? "" : (film != null && film.getReview() != null ? film.getReview() : "");
		%>

			<!-- Back Button -->
			<a href="javascript:history.back()" class="inline-flex items-center gap-2 mb-4 px-4 py-2 border border-neutral-700 bg-[#1c1b1b] text-neutral-200 hover:bg-[#2a2a2a] rounded-lg transition-colors text-sm font-medium">
				← Back
			</a>

			<!-- Page Title -->
			<div class="mb-6">
				<h1 class="text-2xl font-bold mb-2"><%= isCreateMode ? "Add Film" : "Edit Film Detail" %></h1>
				<p class="text-sm text-red-300">
					<%= isCreateMode 
						? "Add a new film to the visual archive." 
						: "Update the visual archive metadata for the cinematic record." %>
				</p>
			</div>

			<!-- Main Form Container -->
			<div class="grid grid-cols-1 lg:grid-cols-[320px_1fr] gap-6 items-start">
				<!-- Sidebar -->
				<aside class="bg-card rounded-lg p-6 space-y-5 lg:sticky lg:top-24">
					<div class="space-y-2">
						<h3 class="text-warning font-semibold"><%= isCreateMode ? "Draft Overview" : "Edit Overview" %></h3>
						<p class="text-xs text-textMuted">
							<%= isCreateMode 
								? "Fill in film details, then create the record in your archive." 
								: "Update the selected film and save changes to keep your archive current." %>
						</p>
					</div>

					<div class="space-y-2 border-t border-border pt-4">
						<div class="flex items-center justify-between">
							<p class="text-xs text-red-300">Mode</p>
							<p class="text-xs text-text"><%= isCreateMode ? "Create" : "Update" %></p>
						</div>

						<% if (!isCreateMode) { %>
							<div class="flex items-center justify-between">
								<p class="text-xs text-red-300">Film ID</p>
								<p class="text-xs text-text"><%= filmId %></p>
							</div>
						<% } %>

						<div class="flex items-center justify-between">
							<p class="text-xs text-red-300">API Format</p>
							<p id="currentApiFormat" class="text-xs text-text uppercase">JSON</p>
						</div>
					</div>

					<div class="space-y-2 border-t border-border pt-4">
						<p class="text-xs text-red-300 uppercase font-semibold block">Quick Tips</p>
						<p class="text-xs text-textMuted block">Use comma-separated names for stars.</p>
						<p class="text-xs text-textMuted block">Keep the review concise for better card previews.</p>
					</div>
				</aside>

				<!-- Form Section -->
				<div class="bg-cardLight rounded-lg p-6 w-full max-w-4xl">
					<% if (successMessage != null) { %>
						<div class="mb-4 p-3 bg-green-900/20 border border-green-700 rounded-lg">
							<p class="text-green-300 text-sm"><%= successMessage %></p>
						</div>
					<% } %>
					
					<form id="filmForm" method="post" action="<%= request.getContextPath() %>/edit-film" class="space-y-6">
						<input type="hidden" name="redirect" value="home" />
						<% if (!isCreateMode) { %>
							<input type="hidden" name="id" value="<%= filmId %>" />
						<% } %>

						<!-- Title and Year Row -->
						<div class="grid grid-cols-1 md:grid-cols-2 gap-6">
							<!-- Title -->
							<div class="flex flex-col space-y-2">
								<label class="text-xs text-red-300 uppercase font-medium">Title *</label>
								<input
									type="text"
									name="title"
									id="title"
									placeholder="Enter the film title"
									value="<%= title %>"
									required
									class="w-full h-10 px-3 rounded-lg border border-border bg-transparent text-text placeholder:text-textMuted outline-none focus:border-accent transition-colors text-sm"
								/>
							</div>

							<!-- Year -->
							<div class="flex flex-col space-y-2">
								<label class="text-xs text-red-300 uppercase font-medium">Year *</label>
								<input
									type="number"
									name="year"
									id="year"
									placeholder="Enter release year"
									value="<%= year %>"
									min="1800"
									required
									class="w-full h-10 px-3 rounded-lg border border-border bg-transparent text-text placeholder:text-textMuted outline-none focus:border-accent transition-colors text-sm"
								/>
							</div>
						</div>

						<!-- Director -->
						<div class="flex flex-col space-y-2">
							<label class="text-xs text-red-300 uppercase font-medium">Director *</label>
							<input
								type="text"
								name="director"
								id="director"
								placeholder="Enter the film director"
								value="<%= director %>"
								required
								class="w-full h-10 px-3 rounded-lg border border-border bg-transparent text-text placeholder:text-textMuted outline-none focus:border-accent transition-colors text-sm"
							/>
						</div>

						<!-- Stars -->
						<div class="flex flex-col space-y-2">
							<label class="text-xs text-red-300 uppercase font-medium">Stars</label>
							<textarea
								name="stars"
								id="stars"
								placeholder="Enter stars separated by commas"
								rows="3"
								class="w-full px-3 py-2 rounded-lg border border-border bg-transparent text-text placeholder:text-textMuted outline-none focus:border-accent transition-colors text-sm resize-none"
							><%= stars %></textarea>
						</div>

						<!-- Review -->
						<div class="flex flex-col space-y-2">
							<label class="text-xs text-red-300 uppercase font-medium">Review</label>
							<textarea
								name="review"
								id="review"
								placeholder="Enter the film review"
								rows="6"
								class="w-full px-3 py-2 rounded-lg border border-border bg-transparent text-text placeholder:text-textMuted outline-none focus:border-accent transition-colors text-sm resize-none"
							><%= review %></textarea>
						</div>

						<!-- Action Buttons -->
						<div class="flex items-center justify-end gap-3 pt-4">
							<button
								type="button"
								onclick="history.back()"
								class="px-5 py-2 border border-neutral-700 bg-[#1c1b1b] text-neutral-200 hover:bg-[#2a2a2a] rounded-lg transition-colors text-sm font-medium"
							>
								Cancel
							</button>
							<button
								type="submit"
								id="submitBtn"
								class="px-5 py-2 bg-[#E50914] hover:bg-[#E50914]/80 text-white rounded-lg text-sm font-medium transition-colors flex items-center gap-2"
							>
								<span>💾</span>
								<span id="submitBtnText"><%= isCreateMode ? "Create Film" : "Save Changes" %></span>
							</button>
						</div>
					</form>
				</div>
			</div>

			<script src="<%= request.getContextPath() %>/js/edit-film.js"></script>
			<script src="<%= request.getContextPath() %>/js/api-format.js"></script>

		<% } %>
	</div>
</body>
</html>
