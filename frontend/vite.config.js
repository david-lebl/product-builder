import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

export default defineConfig({
  plugins: [
    scalaJSPlugin({
      cwd: "..",
      projectID: "frontend",
    }),
  ],
  server: {
    proxy: {
      "/api": "http://localhost:8081",
    },
  },
  build: {
    outDir: "dist",
  },
});
