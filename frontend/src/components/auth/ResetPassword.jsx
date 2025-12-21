import React, { useState, useEffect, useMemo } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import PasswordInput from "../commons/PasswordInput";
import "./ResetPassword.css";
import { getToken, changePassword } from "../../api/homeApi";

export default function ResetPassword() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const token = searchParams.get("token");

  useEffect(() => {
    if (getToken()) navigate("/home");
  }, [navigate]);

  const [form, setForm] = useState({
    newPassword: "",
    confirmPassword: "",
  });

  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({
    newPassword: "",
    confirmPassword: "",
  });

  const [globalError, setGlobalError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const MIN_LENGTH = 8;

  const updateField = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));

    // clear specific field errors while typing
    setFieldErrors((err) => ({ ...err, [name]: "" }));
    setGlobalError("");
  };

  // redirect after success
  useEffect(() => {
    if (!successMessage) return;
    const timer = setTimeout(() => navigate("/login"), 2500);
    return () => clearTimeout(timer);
  }, [successMessage, navigate]);

  // validation helper
  const validateForm = () => {
    const errors = {};

    if (!form.newPassword || form.newPassword.length < MIN_LENGTH) {
      errors.newPassword = `Password must be at least ${MIN_LENGTH} characters long.`;
    }

    if (form.confirmPassword !== form.newPassword) {
      errors.confirmPassword = "Passwords do not match.";
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGlobalError("");
    setSuccessMessage("");

    if (!validateForm()) return;

    setSubmitting(true);

    try {
      const ok = await changePassword(token, form.newPassword);
      if (ok) {
        setSuccessMessage("Password changed successfully!");
      }
    } catch (err) {
      if (err.status === 400) {
        setFieldErrors((prev) => ({
          ...prev,
          newPassword: "New password cannot be the same as the old password.",
        }));
      } else {
        setGlobalError(
          err.userMessage || err.message || "Failed to change password."
        );
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="reset-password">
      <div className="reset-content">
        <h2>Reset Password</h2>

        {!successMessage && (
          <form onSubmit={handleSubmit} noValidate>
            <div className="user-form-row">
              <label>New password:</label>
              <PasswordInput
                name="newPassword"
                value={form.newPassword}
                onChange={updateField}
                minLength={MIN_LENGTH}
                externalError={fieldErrors.newPassword}
              />
            </div>

            <div className="user-form-row">
              <label>Confirm password:</label>
              <PasswordInput
                name="confirmPassword"
                value={form.confirmPassword}
                onChange={updateField}
                minLength={MIN_LENGTH}
                externalError={fieldErrors.confirmPassword}
              />
            </div>

            {globalError && (
              <div className="error-message" role="alert">
                {globalError}
              </div>
            )}

            <div className="user-form-actions">
              <button
                type="button"
                onClick={() => navigate(-1)}
                className="sign-up-cancel-btn"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="sign-up-btn"
              >
                Send
              </button>
            </div>
          </form>
        )}

        {successMessage && (
          <>
            <div className="success-message">{successMessage}</div>
            <button className="go-home-btn" onClick={() => navigate("/home")}>
              Return Home
            </button>
          </>
        )}
      </div>
    </div>
  );
}
