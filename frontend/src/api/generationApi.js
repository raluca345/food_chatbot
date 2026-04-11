import { API_BASE, getAuthHeader, handleErrorResponse } from "./apiCore";

const guestRecipeRegistry = new Map();
let guestRecipeCounter = 0;

export async function generateImage(params) {
  const url = `${API_BASE}/api/v1/food-images`;
  const res = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...getAuthHeader(),
    },
    body: JSON.stringify(params),
  });
  if (!res.ok) return handleErrorResponse(res, "Failed to generate image");

  const contentType = res.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return res.json();
  }
  const imageUrl = await res.text();
  return { id: null, url: imageUrl };
}

export async function generateRecipe(params) {
  const url = `${API_BASE}/api/v1/recipes`;
  const res = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...getAuthHeader(),
    },
    body: JSON.stringify(params),
  });
  if (!res.ok) return handleErrorResponse(res, "Failed to generate recipe");

  const contentType = res.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    const data = await res.json();
    return {
      recipeMarkdown: data?.recipeMarkdown ?? "",
      fileId: data?.fileId ?? null,
      downloadMarkdown: data?.downloadMarkdown ?? "",
      fullText: data?.fullText ?? data?.recipeMarkdown ?? "",
    };
  }

  const fullText = await res.text();
  return {
    recipeMarkdown: fullText,
    fileId: null,
    downloadMarkdown: "",
    fullText,
  };
}

export function registerGuestRecipeDownload(recipeMarkdown) {
  const key = `guest-${Date.now()}-${guestRecipeCounter++}`;
  guestRecipeRegistry.set(key, recipeMarkdown);
  return `/api/v1/recipes/download/guest?guestKey=${encodeURIComponent(key)}`;
}

function parseFilenameFromDisposition(disposition, fallbackName) {
  let filename = fallbackName;
  try {
    const match = disposition.match(/filename\*?=(?:UTF-8''|\")?([^;\"\n]+)/i);
    if (match && match[1]) {
      filename = decodeURIComponent(match[1].replace(/"/g, ""));
    }
  } catch (e) {
    // fallback to provided name
  }
  return filename;
}

function triggerDownload(blob, filename) {
  const objectUrl = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = filename;
  link.style.display = "none";
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(objectUrl);
}

export async function downloadGeneratedRecipe({ fileId, recipeMarkdown }) {
  let res;
  let fallbackName = "recipe.txt";

  if (fileId) {
    fallbackName = `recipe-${fileId}.txt`;
    res = await fetch(`${API_BASE}/api/v1/recipes/download/${fileId}`, {
      method: "GET",
      headers: getAuthHeader(),
    });
    if (!res.ok) {
      return handleErrorResponse(res, "Failed to download recipe");
    }
  } else {
    const payload = JSON.stringify({ recipeMarkdown });
    const guestHeaders = {
      "Content-Type": "application/json",
      ...getAuthHeader(),
    };

    res = await fetch(`${API_BASE}/api/v1/recipes/download/guest`, {
      method: "POST",
      headers: guestHeaders,
      body: payload,
    });
    if (!res.ok) {
      return handleErrorResponse(res, "Failed to download recipe");
    }
  }

  const disposition = res.headers.get("content-disposition") || "";
  const filename = parseFilenameFromDisposition(disposition, fallbackName);
  const blob = await res.blob();
  triggerDownload(blob, filename);
  return true;
}

export async function downloadRecipeFromMarkdownLink(href) {
  const parsedUrl = new URL(String(href || ""), API_BASE);
  const isGuestDownloadPath = /\/api\/v1\/recipes\/download\/guest\/?$/i.test(
    parsedUrl.pathname,
  );
  if (isGuestDownloadPath) {
    const key = parsedUrl.searchParams.get("guestKey");
    const markdown = key ? guestRecipeRegistry.get(key) : null;
    if (!markdown) {
      return Promise.reject(
        new Error("Guest recipe download session expired."),
      );
    }
    return downloadGeneratedRecipe({ fileId: null, recipeMarkdown: markdown });
  }

  const url = parsedUrl.toString();
  const res = await fetch(url, {
    method: "GET",
    headers: getAuthHeader(),
  });
  if (!res.ok) {
    return handleErrorResponse(res, "Failed to download recipe");
  }

  const disposition = res.headers.get("content-disposition") || "";
  const filename = parseFilenameFromDisposition(disposition, "recipe.txt");
  const blob = await res.blob();
  triggerDownload(blob, filename);
  return true;
}
