import React, { useState } from "react";
import "./AuthPage.css";
import "./ForgotPassword.css";
import { useNavigate } from "react-router";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");

  const handleSubmit = () => {};
  const navigate = useNavigate();

  const returnToPreviousPage = () => {
    navigate(-1);
  };

  return (
    <div className="forgot-password">
      <div className="forgot-content">
        <h2>Enter your email address:</h2>
        <form onSubmit={handleSubmit}>
          <div className="user-form-row">
            <input
              className="user-form-input"
              type="text"
              name="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
        </form>
        <div className="user-form-actions">
          <button
            type="button"
            onClick={returnToPreviousPage}
            className="sign-up-cancel-btn"
          >
            Cancel
          </button>
          <button type="submit" className="sign-up-btn">
            Send
          </button>
        </div>
      </div>
    </div>
  );
}
