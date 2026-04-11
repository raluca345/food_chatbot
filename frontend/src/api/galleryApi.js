import { API_BASE, getAuthHeader, handleErrorResponse } from "./apiCore";

export async function getUserImages({ page = 1, pageSize = 18 } = {}) {
  const params = new URLSearchParams();
  if (page) params.set("page", String(page));
  if (pageSize) params.set("pageSize", String(pageSize));
  const url = `${API_BASE}/api/v1/users/me/images?${params.toString()}`;
  const res = await fetch(url, {
    method: "GET",
    headers: getAuthHeader(),
  });
  if (!res.ok) {
    return handleErrorResponse(res, "Failed to retrieve user images");
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

export async function downloadImage(id) {
  const url = `${API_BASE}/api/v1/users/me/images/${id}/download`;

  const res = await fetch(url, {
    method: "GET",
    headers: getAuthHeader(),
  });
  if (!res.ok) return handleErrorResponse(res, "Failed to download image");

  let filename = "image.png";
  const disposition = res.headers.get("content-disposition");
  console.log("Disposition: ", disposition);

  if (disposition) {
    // Matches simple Content-Disposition filename forms, e.g.:
    // filename=image.png or filename="image.png"
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
  const res = await fetch(url, { method: "DELETE", headers: getAuthHeader() });
  if (!res.ok) return handleErrorResponse(res, "Failed to delete image");
  return true;
}
