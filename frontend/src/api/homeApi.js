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

export async function generateMessage(prompt) {
  const url = `${API_BASE}/api/v1/messages?prompt=${encodeURIComponent(
    prompt
  )}`;
  const res = await fetch(url, {
    method: "POST",
    headers: { ..._getAuthHeader(), "Content-Type": "application/json" },
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Failed to generate message");
  }
  return res.text();
}

export async function generateImage(params) {
  const query = new URLSearchParams(params);
  const url = `${API_BASE}/api/v1/food-images?${query.toString()}`;
  const res = await fetch(url, { method: "POST", headers: _getAuthHeader() });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Failed to generate image");
  }
  return res.text();
}

export async function generateRecipe(params) {
  const query = new URLSearchParams(params);
  const url = `${API_BASE}/api/v1/recipes?${query.toString()}`;
  const res = await fetch(url, { method: "POST", headers: _getAuthHeader() });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Failed to generate recipe");
  }
  return res.text();
}

export async function getUserRecipeHistory({ page = 1, pageSize = 10 } = {}) {
  const params = new URLSearchParams();
  if (page) params.set("page", String(page));
  if (pageSize) params.set("pageSize", String(pageSize));
  const url = `${API_BASE}/api/v1/users/me/recipes/history?${params.toString()}`;
  const res = await fetch(url, { method: "GET", headers: _getAuthHeader() });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(
      text || "Failed to fetch recipe history for logged in user"
    );
  }

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
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Failed to delete recipe from history");
  }
  return true;
}

export async function downloadRecipeFromHistory(id) {
  const url = `${API_BASE}/api/v1/users/me/recipes/history/${id}/download`;
  const res = await fetch(url, { method: "GET", headers: _getAuthHeader() });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Failed to download recipe from history");
  }

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

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Failed to retrieve user images");
  }

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

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Registration failed");
  }

  return res.json();
}

export async function loginUser(credentials) {
  const url = `${API_BASE}/api/v1/auth/login`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Login failed");
  }

  return res.json();
}

export async function downloadImage(id) {
  const url = `${API_BASE}/api/v1/users/me/images/${id}/download`;

  const res = await fetch(url, {
    method: "GET",
    headers: _getAuthHeader(),
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Failed to download image");
  }

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
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Failed to delete image");
  }
  return true;
}
