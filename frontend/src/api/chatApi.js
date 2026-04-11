import {
  API_BASE,
  getAuthHeader,
  getToken,
  handleErrorResponse,
} from "./apiCore";

export async function startConversation(message) {
  const token = getToken();
  const isLoggedIn = Boolean(token);
  const endpoint = isLoggedIn ? "/api/v1/chat" : "/api/v1/chat/guest";

  const res = await fetch(`${API_BASE}${endpoint}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(isLoggedIn ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ message }),
  });

  if (!res.ok) {
    return handleErrorResponse(res, "Failed to start conversation");
  }

  return res.json();
}

export async function sendMessageToConversation(conversationId, message) {
  const res = await fetch(
    `${API_BASE}/api/v1/chat/${conversationId}/messages`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...getAuthHeader(),
      },
      body: JSON.stringify({ message }),
    },
  );

  if (!res.ok) {
    return handleErrorResponse(res, "Failed to send message");
  }

  return res.json();
}

export async function loadConversation(
  conversationId,
  { page = 1, pageSize = 4 } = {},
) {
  const params = new URLSearchParams();
  if (page) params.set("page", String(page));
  if (pageSize) params.set("pageSize", String(pageSize));
  const query = params.toString();

  const res = await fetch(
    `${API_BASE}/api/v1/chat/${conversationId}${query ? `?${query}` : ""}`,
    {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        ...getAuthHeader(),
      },
    },
  );

  if (!res.ok) {
    return handleErrorResponse(res, "Failed to load conversation");
  }

  return res.json();
}

export async function renameConversation(conversationId, title) {
  const safeTitle = String(title || "").trim();
  const res = await fetch(`${API_BASE}/api/v1/chat/${conversationId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      ...getAuthHeader(),
    },
    body: JSON.stringify({ title: safeTitle }),
  });

  if (!res.ok) {
    return handleErrorResponse(res, "Failed to rename conversation");
  }

  if (res.status === 200) return true;
  return res.json();
}

export async function loadConversations({ page = 1, pageSize = 20 } = {}) {
  const params = new URLSearchParams();
  if (page) params.set("page", String(page));
  if (pageSize) params.set("pageSize", String(pageSize));

  const query = params.toString();
  const url = `${API_BASE}/api/v1/chat${query ? `?${query}` : ""}`;

  const res = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      ...getAuthHeader(),
    },
  });

  if (!res.ok) {
    return handleErrorResponse(res, "Failed to load conversations");
  }

  return res.json();
}

export async function deleteConversation(conversationId) {
  const res = await fetch(`${API_BASE}/api/v1/chat/${conversationId}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      ...getAuthHeader(),
    },
  });

  if (!res.ok) {
    return handleErrorResponse(res, "Failed to delete conversation");
  }

  return true;
}
