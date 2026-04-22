// Film Library - client-side pagination over server-rendered cards
const FILMS_PER_PAGE = 9;

let allCards = [];
let filteredCards = [];
let currentPage = 1;
let searchQuery = "";

function getPageNumbers(activePage, totalPages) {
	if (totalPages <= 5) return Array.from({ length: totalPages }, (_, i) => i + 1);
	const pages = [1];
	const start = Math.max(2, activePage - 1);
	const end = Math.min(totalPages - 1, activePage + 1);
	for (let p = start; p <= end; p++) pages.push(p);
	if (!pages.includes(totalPages)) pages.push(totalPages);
	return pages;
}

function getNoResultsElement(grid) {
	let el = document.getElementById("noResultsState");
	if (el) return el;

	el = document.createElement("div");
	el.id = "noResultsState";
	el.className = "bg-[#1c1b1b] rounded-lg p-8 text-center space-y-3 col-span-full hidden";
	el.innerHTML =
		'<h3 class="text-xl font-semibold text-amber-200">No Matching Films</h3>' +
		'<p class="text-neutral-400 block">Try a different title or year.</p>' +
		'<button class="inline-flex items-center gap-2 px-4 py-2 border border-neutral-700 bg-transparent hover:bg-[#2a2a2a] rounded-lg text-sm font-medium" onclick="clearSearch()">Clear Search</button>';
	grid.appendChild(el);
	return el;
}

function filterCards() {
	if (!searchQuery) return allCards.slice();
	return allCards.filter(card => {
		const title = (card.getAttribute("data-title") || "").toLowerCase();
		const year = (card.getAttribute("data-year") || "").toLowerCase();
		return title.includes(searchQuery) || year.includes(searchQuery);
	});
}

function updateSearch() {
	const input = document.getElementById("searchInput");
	searchQuery = input ? input.value.toLowerCase().trim() : "";
	currentPage = 1;
	filteredCards = filterCards();
	render();
}

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

function renderResultsInfo(activePage) {
	const startResultEl = document.getElementById("startResult");
	const endResultEl = document.getElementById("endResult");
	const totalCountEl = document.getElementById("totalCount");
	const countSuffixEl = document.getElementById("filmCountSuffix");
	const filteredInfoEl = document.getElementById("filteredInfo");

	const startResult = filteredCards.length === 0 ? 0 : (activePage - 1) * FILMS_PER_PAGE + 1;
	const endResult = Math.min(activePage * FILMS_PER_PAGE, filteredCards.length);
	const hasActiveFilters = searchQuery.length > 0;

	if (startResultEl) startResultEl.textContent = String(startResult);
	if (endResultEl) endResultEl.textContent = String(endResult);
	if (totalCountEl) totalCountEl.textContent = String(filteredCards.length);
	if (countSuffixEl) countSuffixEl.textContent = filteredCards.length === 1 ? "" : "s";
	if (filteredInfoEl) {
		filteredInfoEl.textContent = hasActiveFilters ? ` (filtered from ${allCards.length})` : "";
	}
}

function renderPagination(activePage, totalPages) {
	const paginationContainer = document.getElementById("paginationContainer");
	if (!paginationContainer) return;

	if (totalPages <= 1) {
		paginationContainer.innerHTML = "";
		return;
	}

	const pageNumbers = getPageNumbers(activePage, totalPages);
	paginationContainer.innerHTML = `
		<div class="flex flex-wrap items-center justify-center gap-2 pt-3 pb-10">
			<button class="h-10 px-4 border border-neutral-700 bg-[#1c1b1b] text-neutral-200 hover:bg-[#2a2a2a] rounded-lg disabled:opacity-50 disabled:cursor-not-allowed text-sm font-medium transition-colors inline-flex items-center gap-2" onclick="goToPage(${activePage - 1})" ${activePage === 1 ? "disabled" : ""}>
				<span>Prev</span>
			</button>
			${pageNumbers
				.map((page, index) => {
					const prevPage = pageNumbers[index - 1];
					const shouldShowGap = prevPage && page - prevPage > 1;
					return `
						${shouldShowGap ? '<span class="text-neutral-500 px-1 text-sm">...</span>' : ""}
						<button class="h-10 min-w-10 px-3 rounded-lg text-sm font-medium transition-colors ${
							activePage === page
								? "bg-[#E50914] text-white hover:bg-[#E50914]/80"
								: "border border-neutral-700 bg-[#1c1b1b] text-neutral-200 hover:bg-[#2a2a2a]"
						}" onclick="goToPage(${page})">${page}</button>
					`;
				})
				.join("")}
			<button class="h-10 px-4 border border-neutral-700 bg-[#1c1b1b] text-neutral-200 hover:bg-[#2a2a2a] rounded-lg disabled:opacity-50 disabled:cursor-not-allowed text-sm font-medium transition-colors inline-flex items-center gap-2" onclick="goToPage(${activePage + 1})" ${activePage === totalPages ? "disabled" : ""}>
				<span>Next</span>
			</button>
		</div>
	`;
}

function render() {
	const totalPages = Math.max(1, Math.ceil(filteredCards.length / FILMS_PER_PAGE));
	const activePage = Math.min(Math.max(currentPage, 1), totalPages);
	currentPage = activePage;

	renderGrid(activePage);
	renderResultsInfo(activePage);
	renderPagination(activePage, totalPages);
}

function goToPage(page) {
	const totalPages = Math.max(1, Math.ceil(filteredCards.length / FILMS_PER_PAGE));
	if (page < 1 || page > totalPages || page === currentPage) return;
	currentPage = page;
	render();
	const target = document.getElementById("resultsInfo") || document.body;
	target.scrollIntoView({ behavior: "smooth" });
}

function clearSearch() {
	const input = document.getElementById("searchInput");
	if (input) input.value = "";
	updateSearch();
}

function initFromRenderedGrid() {
	const grid = document.getElementById("filmsGrid");
	if (!grid) return;

	allCards = Array.from(grid.querySelectorAll("[data-id]"));
	const input = document.getElementById("searchInput");
	searchQuery = input ? input.value.toLowerCase().trim() : "";
	filteredCards = filterCards();
	currentPage = 1;
	render();
}

window.updateSearch = updateSearch;
window.goToPage = goToPage;
window.clearSearch = clearSearch;
window.initFromRenderedGrid = initFromRenderedGrid;

if (document.readyState === "loading") {
	document.addEventListener("DOMContentLoaded", initFromRenderedGrid);
} else {
	initFromRenderedGrid();
}