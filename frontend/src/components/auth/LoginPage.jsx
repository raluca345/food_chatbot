import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./AuthPage.css";

export default function LoginPage() {
  const navigate = useNavigate();

  const returnToPreviousPage = () => {
    navigate(-1);
  };

  const [formData, setFormData] = useState({
    userName: "",
    password: "",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    // Handle form submission
    console.log("Form Data Submitted: ", formData);
  };

  return (
    <div className="user-form">
      <h2>Log In</h2>
      <form onSubmit={handleSubmit}>
        <div className="user-form-row">
          <label>Username:</label>
          <input
            className="user-form-input"
            type="text"
            name="userName"
            placeholder="Username or email"
            value={formData.userName}
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
          <button onClick={returnToPreviousPage} className="sign-up-cancel-btn">
            Cancel
          </button>
          <button type="submit" className="sign-up-btn">
            Sign Up
          </button>
        </div>
      </form>
    </div>
  );
}
