import React from "react";
import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <div className="tab-content">
      <h2>Page not found</h2>
      <p>The page you are looking for doesn't exist.</p>
      <p>
        <Link to="/home">Return to Home</Link>
      </p>
    </div>
  );
}
