/**
 * Backend API base URL (no trailing slash).
 * - Local dev: leave unset so Vite proxy forwards /api, /auth, etc. to localhost:8080.
 * - Vercel/prod: set VITE_API_BASE_URL to your Render backend, e.g. https://nexaedi.onrender.com
 */
export const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string)?.trim() || '';
