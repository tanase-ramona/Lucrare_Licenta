import React from "react";
import Navbar from "./Navbar";
import "./PageLayout.css";

type Props = { children: React.ReactNode };

export default function PageLayout({ children }: Props) {
  return (
    <div className="app-layout">
      <Navbar />
      <main className="app-main">{children}</main>
    </div>
  );
}
