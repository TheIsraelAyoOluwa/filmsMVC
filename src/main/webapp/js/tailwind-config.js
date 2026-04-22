// Shared Tailwind Configuration
const TAILWIND_CONFIG = {
	theme: {
		extend: {
			colors: {
				bg: '#0f0f0f',
				card: '#1c1b1b',
				cardLight: '#2a2a2a',
				border: '#404040',
				text: '#e5e5e5',
				textMuted: '#808080',
				textSecondary: '#b0b0b0',
				accent: '#E50914',
				accentHover: '#b3050d',
				warning: '#fbbf24',
				red: '#fca5a5'
			}
		}
	}
};

// Ensure config is visible whether this file loads before or after the Tailwind CDN script.
window.tailwind = window.tailwind || {};
window.tailwind.config = TAILWIND_CONFIG;

// Export for use in other modules if needed
if (typeof module !== 'undefined' && module.exports) {
	module.exports = TAILWIND_CONFIG;
}
