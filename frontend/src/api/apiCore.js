export const API_BASE =
  (import.meta && import.meta.env && import.meta.env.VITE_API_BASE_URL) ||
  "http://localhost:8080";

export function getToken() {
  const token = localStorage.getItem("token");
  if (!token) return null;

  const [, payload] = token.split(".");
  const data = JSON.parse(atob(payload));

  const now = Date.now() / 1000;

  if (data.exp < now) {
    localStorage.removeItem("token");
    return null;
  }

  return token;
}

export function getAuthHeader() {
  try {
    const token = getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  } catch (e) {
    return {};
  }
}

export async function handleErrorResponse(res, fallbackMessage) {
  const status = res.status;
  const text = await res.text().catch(() => "");
  let parsed = null;
  try {
    parsed = JSON.parse(text);
  } catch (e) {
    // not JSON
  }

  const serverMsg =
    (parsed && (parsed.message || parsed.error || parsed.detail)) ||
    text ||
    fallbackMessage;

  const err = new Error(serverMsg || fallbackMessage || "Request failed");
  err.status = status;
  err.server = parsed || text;

  const m = String(serverMsg || "").toLowerCase();
  let userMessage =
    fallbackMessage || "Something went wrong. Please try again.";

  const url = (res && res.url) || "";

  if (status === 401) {
    userMessage = "Failed to log in. Please try again.";
  } else if (
    m.includes("email") &&
    (m.includes("exist") ||
      m.includes("already") ||
      m.includes("duplicate") ||
      m.includes("registered"))
  ) {
    userMessage = "An account with that email already exists.";
  } else if (status === 409) {
    if (url.includes("/auth/register")) {
      userMessage = "An account with that email already exists.";
    } else {
      userMessage = "Conflict: the resource already exists.";
    }
  } else if (status === 404) {
    if (url.includes("/auth/password-reset/verify")) {
      userMessage = "No password reset link for that email address was found.";
    } else {
      userMessage = "Requested resource not found.";
    }
  } else if (status === 410) {
    if (url.includes("/auth/password-reset/verify")) {
      userMessage = "The password reset link has expired.";
    } else {
      userMessage = "Requested resource is no longer available.";
    }
  } else if (
    m.includes("username") &&
    (m.includes("exist") ||
      m.includes("already") ||
      m.includes("duplicate") ||
      m.includes("taken"))
  ) {
    userMessage = "That username is already taken.";
  } else if (
    m.includes("password") &&
    (m.includes("weak") || m.includes("invalid"))
  ) {
    userMessage =
      "Password is invalid or too weak. Please choose a stronger password.";
  } else if (status >= 500) {
    userMessage = "Server error. Please try again later.";
  } else if (serverMsg) {
    userMessage =
      serverMsg.length > 200 ? `${serverMsg.slice(0, 200)}...` : serverMsg;
  }

  err.userMessage = userMessage;
  return Promise.reject(err);
}
