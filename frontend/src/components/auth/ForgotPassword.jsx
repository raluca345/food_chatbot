import React, { useState, useEffect } from "react";
import "./AuthPage.css";
import "./ForgotPassword.css";
import { useNavigate } from "react-router";
import { getToken, sendPasswordResetEmail } from "../../api/homeApi";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [sending, setSending] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    const userEmail = email && email.trim();
    const msgBody =
      "Reset your password by clicking on the link below. Feel free to ignore if you didn't send this request.";
    const subject = "Reset Password";

    // basic email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!userEmail || !emailRegex.test(userEmail)) {
      setErrorMessage("Please enter a valid email address.");
      return;
    }

    setSending(true);
    try {
      await sendPasswordResetEmail(userEmail, msgBody, subject);
      setSuccessMessage(
        "We've sent password reset instructions to that email address!"
      );
      setEmail("");
    } catch (err) {
      console.error("Couldn't send email:", err);
      setErrorMessage(err?.userMessage || "Sending email failed");
    } finally {
      setSending(false);
    }
  };
  const navigate = useNavigate();

  const returnToPreviousPage = () => {
    navigate(-1);
  };

  const returnHome = () => {
    navigate("/home");
  };

  useEffect(() => {
    const t = getToken();
    if (t) navigate("/home");
  }, [navigate]);

  return (
    <div className={`forgot-password ${successMessage ? "forgot-password-success" : ""}`}>
      <div className="forgot-content">
        {!successMessage && (
          <>
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
              <div className="user-form-actions">
                <button
                  type="button"
                  onClick={returnToPreviousPage}
                  className="sign-up-cancel-btn"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="sign-up-btn"
                  disabled={sending}
                >
                  {sending ? "Sending..." : "Send"}
                </button>
              </div>
            </form>
          </>
        )}

        {errorMessage && (
          <div className="error-message" role="alert">
            {errorMessage}
          </div>
        )}

        {successMessage && (
          <div className="success-message" role="status">
            {successMessage}
          </div>
        )}

        {successMessage && (
          <div className="go-home">
            <button className="go-home-btn" onClick={returnHome}>
              Return Home
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
