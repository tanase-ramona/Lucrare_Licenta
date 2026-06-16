import React from "react";
import ReactDOM from "react-dom/client";
// @ts-ignore
import "./index.css";
import { BrowserRouter } from "react-router-dom";
import App from "./App.js";
import { AuthProvider } from "./auth/AuthContext.tsx";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);