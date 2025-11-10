import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./AuthPage.css";

export default function LoginPage() {
  const [errorMessage, setErrorMessage] = useState("");
  const navigate = useNavigate();

  const returnToPreviousPage = () => {
    navigate(-1);
  };

  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage("");

    try {
      const response = await fetch("http://localhost:8080/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        const errorText = await response.text();
        setErrorMessage(errorText || "Login failed");
        return;
      }

      const data = await response.json();
      localStorage.setItem("token", data.token);
      navigate("/home");
    } catch (err) {
      console.error("Login error:", err);
      setErrorMessage(err.message);
    }
  };

  return (
    <>
      {errorMessage && <div className="error-message">{errorMessage}</div>}
      <div className="user-form">
        <h2>Log In</h2>
        <form onSubmit={handleSubmit}>
          <div className="user-form-row">
            <label>Username:</label>
            <input
              className="user-form-input"
              type="text"
              name="email"
              placeholder="Username or email"
              value={formData.email}
              onChange={handleChange}
            />
          </div>
          <div className="user-form-row">
            <label>Password:</label>
            <input
              className="user-form-input"
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
            />
          </div>
          <div className="user-form-actions">
            <button
              onClick={returnToPreviousPage}
              className="sign-up-cancel-btn"
            >
              Cancel
            </button>
            <button type="submit" className="sign-up-btn">
              Log In
            </button>
          </div>
        </form>
      </div>
    </>
  );
}
