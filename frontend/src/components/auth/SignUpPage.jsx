import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./SignUpPage.css";

const SignUpPage = () => {
  const [formData, setFormData] = useState({
    userName: "",
    email: "",
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

  const navigate = useNavigate();

  const returnToHome = () => {
    navigate("/home");
  };

  return (
    <div className="user-form">
      <h2>Sign Up</h2>
      <form onSubmit={handleSubmit}>
        <div className="user-form-row">
          <label>Username:</label>
          <input
            className="user-form-input"
            type="text"
            name="userName"
            value={formData.userName}
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
          Already have an account? <a href="">Log In</a>
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
  );
};

export default SignUpPage;
