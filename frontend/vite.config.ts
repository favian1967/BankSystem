import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The dev server runs on 5173, which the backend CORS config already whitelists.
// API calls are also proxied so you can use relative "/api" paths without CORS in dev.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.VITE_API_TARGET || "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
