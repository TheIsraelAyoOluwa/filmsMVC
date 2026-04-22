// Edit Film Form Handler
/**
 * Initialize the edit film form
 */
function initializeEditFilmForm() {
	const form = document.getElementById("filmForm");
	const submitBtn = document.getElementById("submitBtn");
	const submitBtnText = document.getElementById("submitBtnText");

	if (!form) {
		console.error("Film form not found");
		return;
	}

	form.addEventListener("submit", function(e) {
		// Basic validation
		const title = document.getElementById("title").value.trim();
		const year = parseInt(document.getElementById("year").value);
		const director = document.getElementById("director").value.trim();

		if (!title) {
			e.preventDefault();
			alert("Title is required");
			return;
		}

		if (!director) {
			e.preventDefault();
			alert("Director is required");
			return;
		}

		if (isNaN(year) || year <= 1800) {
			e.preventDefault();
			alert("Please provide a valid release year (after 1800)");
			return;
		}

		// Disable button and show loading state
		submitBtn.disabled = true;
		submitBtnText.textContent = "Saving...";
	});
}

/**
 * Trim whitespace from form inputs
 */
function trimFormInputs() {
	const inputs = document.querySelectorAll("input[type='text'], textarea");
	inputs.forEach(input => {
		input.value = input.value.trim();
	});
}

/**
 * Pre-populate form with film data (if in edit mode)
 */
function populateFormWithFilmData(filmData) {
	if (!filmData) return;

	const fields = ["title", "year", "director", "stars", "review"];
	fields.forEach(field => {
		const element = document.getElementById(field);
		if (element && filmData[field]) {
			element.value = filmData[field];
		}
	});
}

/**
 * Validate individual field
 */
function validateField(fieldName, value) {
	switch (fieldName) {
		case "title":
			return value.trim().length > 0 ? null : "Title is required";
		case "director":
			return value.trim().length > 0 ? null : "Director is required";
		case "year":
			const year = parseInt(value);
			return !isNaN(year) && year > 1800 ? null : "Year must be after 1800";
		case "stars":
		case "review":
			return null; // Optional fields
		default:
			return null;
	}
}

/**
 * Add real-time validation to form fields
 */
function addRealTimeValidation() {
	const fields = ["title", "year", "director"];
	
	fields.forEach(fieldName => {
		const field = document.getElementById(fieldName);
		if (!field) return;

		field.addEventListener("blur", function() {
			const error = validateField(fieldName, this.value);
			if (error) {
				this.classList.add("border-red-500");
				console.warn(`Validation error for ${fieldName}: ${error}`);
			} else {
				this.classList.remove("border-red-500");
			}
		});
	});
}

/**
 * Reset form to initial state
 */
function resetForm() {
	const form = document.getElementById("filmForm");
	if (form) {
		form.reset();
	}
}

// Initialize when DOM is ready
document.addEventListener("DOMContentLoaded", function() {
	initializeEditFilmForm();
	addRealTimeValidation();
});
