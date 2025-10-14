import React from "react";
import { useNavigate } from "react-router-dom";

export default function LoginPage() {
  const navigate = useNavigate();
  return (
    <div className="tab-content">
      <h2>Log In</h2>
      <p>This is a placeholder login page.</p>
      <button onClick={() => navigate(-1)}>Back</button>
    </div>
  );
}
