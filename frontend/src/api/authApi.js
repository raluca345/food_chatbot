import { API_BASE, handleErrorResponse } from "./apiCore";

export async function registerUser(data) {
  const url = `${API_BASE}/api/v1/auth/register`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!res.ok) return handleErrorResponse(res, "Registration failed");

  return res.json();
}

export async function loginUser(credentials) {
  const url = `${API_BASE}/api/v1/auth/login`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });
  if (!res.ok) return handleErrorResponse(res, "Login failed");

  return res.json();
}

export async function sendPasswordResetEmail(userEmail, msgBody, subject) {
  const url = `${API_BASE}/api/v1/auth/password-reset/request`;
  const body = {
    recipient: userEmail,
    msgBody,
    subject,
  };
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) return handleErrorResponse(res, "Failed to send email");
  return true;
}

export async function changePassword(token, newPassword) {
  const url = `${API_BASE}/api/v1/auth/password-reset/confirm`;
  const body = {
    token,
    password: newPassword,
  };
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) return handleErrorResponse(res, "Failed to change password");
  return true;
}
