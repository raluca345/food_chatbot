import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import "./AuthPage.css";

const SignUpPage = () => {
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
  });
  const [errorMessage, setErrorMessage] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage("");

    try {
      const response = await fetch(
        "http://localhost:8080/api/v1/auth/register",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(formData),
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Registration failed");
      }

      const data = await response.json();
      localStorage.setItem("token", data.token);

      navigate("/home");
    } catch (error) {
      console.error("Registration error:", error);
      setErrorMessage(error.message);
    }
  };

  const returnToHome = () => {
    navigate("/home");
  };

  return (
    <>
      {errorMessage && <div className="error-message">{errorMessage}</div>}
      <div className="user-form">
        <h2>Sign Up</h2>
        <form onSubmit={handleSubmit}>
          <div className="user-form-row">
            <label>Username:</label>
            <input
              className="user-form-input"
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
            />
          </div>
          <div className="user-form-row">
            <label>Email:</label>
            <input
              className="user-form-input"
              type="email"
              name="email"
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
          <p>
            Already have an account?{" "}
            <Link to="/login" className="login-link">
              Log In
            </Link>
          </p>
          <div className="user-form-actions">
            <button onClick={returnToHome} className="sign-up-cancel-btn">
              Cancel
            </button>
            <button type="submit" className="sign-up-btn">
              Sign Up
            </button>
          </div>
        </form>
      </div>
    </>
  );
};

export default SignUpPage;
