import React from "react";
import { Link } from "react-router-dom";
import "./NotFound.css";

export default function NotFound() {
  return (
    <div className="nf-items">
      <h2 className="header">404</h2>
      <p>Oops, not what you're looking for.</p>
      <p>
        <Link to="/home" className="return-link">
          Return Home
        </Link>
      </p>
    </div>
  );
}
