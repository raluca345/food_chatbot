const API_BASE =
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

function _getAuthHeader() {
  try {
    const token = getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  } catch (e) {
    return {};
  }
}

async function _handleErrorResponse(res, fallbackMessage) {
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

  // derive a user-friendly message
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
    //conflict
    if (url.includes("/auth/register")) {
      userMessage = "An account with that email already exists.";
    } else {
      userMessage = "Conflict: the resource already exists.";
    }
  } else if (status === 404) {
    //not found
    if (url.includes("/auth/password-reset/verify")) {
      userMessage = "No password reset link for that email address was found.";
    } else {
      userMessage = "Requested resource not found.";
    }
  } else if (status === 410) {
    //gone
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
      serverMsg.length > 200 ? serverMsg.slice(0, 200) + "..." : serverMsg;
  }

  err.userMessage = userMessage;
  return Promise.reject(err);
}

export async function startConversation(message) {
  const res = await fetch(`${API_BASE}/api/v1/chat`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ..._getAuthHeader(),
    },
    body: JSON.stringify({ message }),
  });

  if (!res.ok) {
    return _handleErrorResponse(res, "Failed to start conversation");
  }

  // { conversationId, assistantMessage }
  return res.json();
}

export async function sendMessageToConversation(conversationId, message) {
  const res = await fetch(
    `${API_BASE}/api/v1/chat/${conversationId}/messages`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ..._getAuthHeader(),
      },
      body: JSON.stringify({ message }),
    }
  );

  if (!res.ok) {
    return _handleErrorResponse(res, "Failed to send message");
  }

  // { conversationId, assistantMessage }
  return res.json();
}

export async function loadConversation(conversationId) {
  const res = await fetch(`${API_BASE}/api/v1/chat/${conversationId}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      ..._getAuthHeader(),
    },
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "Failed to load conversation");
  }

  return res.json(); // {conversationId, {id, role, content, timestamp} }
}

export async function renameConversation(conversationId, title) {
  const safeTitle = String(title || "").trim();
  const res = await fetch(`${API_BASE}/api/v1/chat/${conversationId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      ..._getAuthHeader(),
    },
    body: JSON.stringify({ title: safeTitle }),
  });

  if (!res.ok)
    return _handleErrorResponse(res, "Failed to rename conversation");

  if (res.status === 200) return true;
  return res.json();
}

export async function loadConversations() {
  const res = await fetch(`${API_BASE}/api/v1/chat`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      ..._getAuthHeader(),
    },
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "Failed to load conversations");
  }

  return res.json(); // [ {conversationId, title, {id, role, content, timestamp} ... ]
}

export async function deleteConversation(conversationId) {
  const res = await fetch(`${API_BASE}/api/v1/chat/${conversationId}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      ..._getAuthHeader(),
    },
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "Failed to load conversations");
  }

  return true;
}

export async function generateImage(params) {
  const query = new URLSearchParams(params);
  const url = `${API_BASE}/api/v1/food-images?${query.toString()}`;
  const res = await fetch(url, { method: "POST", headers: _getAuthHeader() });
  if (!res.ok) return _handleErrorResponse(res, "Failed to generate image");
  return res.text();
}

export async function generateRecipe(params) {
  const query = new URLSearchParams(params);
  const url = `${API_BASE}/api/v1/recipes?${query.toString()}`;
  const res = await fetch(url, { method: "POST", headers: _getAuthHeader() });
  if (!res.ok) return _handleErrorResponse(res, "Failed to generate recipe");
  return res.text();
}

export async function getUserRecipeHistory({ page = 1, pageSize = 10 } = {}) {
  const params = new URLSearchParams();
  if (page) params.set("page", String(page));
  if (pageSize) params.set("pageSize", String(pageSize));
  const url = `${API_BASE}/api/v1/users/me/recipes/history?${params.toString()}`;
  const res = await fetch(url, { method: "GET", headers: _getAuthHeader() });
  if (!res.ok)
    return _handleErrorResponse(
      res,
      "Failed to fetch recipe history for logged in user"
    );

  const data = await res.json().catch(() => null);
  if (Array.isArray(data)) {
    return { items: data, total: data.length };
  }
  if (data && Array.isArray(data.items)) {
    return {
      items: data.items,
      total: Number(data.total) || data.items.length,
    };
  }
  return { items: [], total: 0 };
}

export async function deleteRecipeHistoryEntry(id) {
  const url = `${API_BASE}/api/v1/users/me/recipes/history/${id}`;
  const res = await fetch(url, { method: "DELETE", headers: _getAuthHeader() });
  if (!res.ok)
    return _handleErrorResponse(res, "Failed to delete recipe from history");
  return true;
}

export async function downloadRecipeFromHistory(id) {
  const url = `${API_BASE}/api/v1/users/me/recipes/history/${id}/download`;
  const res = await fetch(url, { method: "GET", headers: _getAuthHeader() });
  if (!res.ok)
    return _handleErrorResponse(res, "Failed to download recipe from history");

  const disposition = res.headers.get("content-disposition") || "";
  let filename = `recipe-${id}.txt`;
  try {
    const match = disposition.match(/filename\*?=(?:UTF-8''|\")?([^;\"\n]+)/i);
    if (match && match[1]) {
      filename = decodeURIComponent(match[1].replace(/"/g, ""));
    }
  } catch (e) {}

  const blob = await res.blob();
  return { blob, filename };
}

export async function getUserImages({ page = 1, pageSize = 18 } = {}) {
  const params = new URLSearchParams();
  if (page) params.set("page", String(page));
  if (pageSize) params.set("pageSize", String(pageSize));
  const url = `${API_BASE}/api/v1/users/me/images?${params.toString()}`;
  const res = await fetch(url, {
    method: "GET",
    headers: _getAuthHeader(),
  });
  if (!res.ok)
    return _handleErrorResponse(res, "Failed to retrieve user images");

  const data = await res.json().catch(() => null);

  if (Array.isArray(data)) {
    return { items: data, total: data.length };
  }
  if (data && Array.isArray(data.items)) {
    return {
      items: data.items,
      total: Number(data.total) || data.items.length,
    };
  }
  return { items: [], total: 0 };
}

export async function registerUser(data) {
  const url = `${API_BASE}/api/v1/auth/register`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!res.ok) return _handleErrorResponse(res, "Registration failed");

  return res.json();
}

export async function loginUser(credentials) {
  const url = `${API_BASE}/api/v1/auth/login`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });
  if (!res.ok) return _handleErrorResponse(res, "Login failed");

  return res.json();
}

export async function downloadImage(id) {
  const url = `${API_BASE}/api/v1/users/me/images/${id}/download`;

  const res = await fetch(url, {
    method: "GET",
    headers: _getAuthHeader(),
  });
  if (!res.ok) return _handleErrorResponse(res, "Failed to download image");

  let filename = "image.png";
  const disposition = res.headers.get("content-disposition");
  console.log("Disposition: ", disposition);

  if (disposition) {
    const match = disposition.match(/filename="?([^"]+)"?$/);

    if (match) {
      filename = match[1];
    }
  }

  const blob = await res.blob();
  return { blob, filename };
}

export async function deleteImage(id) {
  const url = `${API_BASE}/api/v1/users/me/images/${id}`;
  const res = await fetch(url, { method: "DELETE", headers: _getAuthHeader() });
  if (!res.ok) return _handleErrorResponse(res, "Failed to delete image");
  return true;
}

export async function sendPasswordResetEmail(userEmail, msgBody, subject) {
  const url = `${API_BASE}/api/v1/auth/password-reset/request`;
  const body = {
    recipient: userEmail,
    msgBody: msgBody,
    subject: subject,
  };
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) return _handleErrorResponse(res, "Failed to send email");
  return true;
}

export async function changePassword(token, newPassword) {
  const url = `${API_BASE}/api/v1/auth/password-reset/confirm`;
  const body = {
    token: token,
    password: newPassword,
  };
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) return _handleErrorResponse(res, "Failed to change password");
  return true;
}
