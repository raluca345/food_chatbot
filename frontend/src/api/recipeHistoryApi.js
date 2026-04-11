import { API_BASE, getAuthHeader, handleErrorResponse } from "./apiCore";

export async function getUserRecipeHistory({ page = 1, pageSize = 10 } = {}) {
  const params = new URLSearchParams();
  if (page) params.set("page", String(page));
  if (pageSize) params.set("pageSize", String(pageSize));
  const url = `${API_BASE}/api/v1/users/me/recipes/history?${params.toString()}`;
  const res = await fetch(url, { method: "GET", headers: getAuthHeader() });
  if (!res.ok) {
    return handleErrorResponse(res, "Failed to fetch recipe history");
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
  const res = await fetch(url, { method: "DELETE", headers: getAuthHeader() });
  if (!res.ok) {
    return handleErrorResponse(res, "Failed to delete recipe from history");
  }
  return true;
}

export async function downloadRecipeFromHistory(id) {
  const url = `${API_BASE}/api/v1/users/me/recipes/history/${id}/download`;
  const res = await fetch(url, { method: "GET", headers: getAuthHeader() });
  if (!res.ok) {
    return handleErrorResponse(res, "Failed to download recipe from history");
  }

  const disposition = res.headers.get("content-disposition") || "";
  let filename = `recipe-${id}.txt`;
  try {
    // Matches filename and filename* variants in Content-Disposition, e.g.:
    // filename=recipe.txt, filename="recipe.txt", filename*=UTF-8''my%20recipe.txt
    const match = disposition.match(/filename\*?=(?:UTF-8''|\")?([^;\"\n]+)/i);
    if (match && match[1]) {
      filename = decodeURIComponent(match[1].replace(/"/g, ""));
    }
  } catch (e) {}

  const blob = await res.blob();
  return { blob, filename };
}
