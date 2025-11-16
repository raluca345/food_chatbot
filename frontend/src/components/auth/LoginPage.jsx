import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./AuthPage.css";
import PasswordInput from "../commons/PasswordInput";
import { loginUser } from "../../api/homeApi";

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
      const data = await loginUser(formData);
      localStorage.setItem("token", data.token);
      navigate("/home");
    } catch (err) {
      console.error("Login error:", err);
      setErrorMessage(err.message || "Login failed");
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
            <PasswordInput value={formData.password} onChange={handleChange} />
          </div>
          <p className="reset-password-link">
            <a
              href="#"
              onClick={(e) => {
                e.preventDefault();
                navigate("/forgot");
              }}
            >
              Forgot Password?
            </a>
          </p>
          <div className="user-form-actions">
            <button
              type="button"
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
