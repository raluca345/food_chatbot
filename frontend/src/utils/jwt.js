import { Buffer } from "buffer";

export function decodeJwt(token) {
  try {
    if (!token || typeof token !== "string") return null;
    const parts = token.split(".");
    if (parts.length < 2) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(
      base64.length + ((4 - (base64.length % 4)) % 4),
      "="
    );
    const json = Buffer.from(padded, "base64").toString("utf-8");
    return JSON.parse(json);
  } catch (e) {
    console.warn("decodeJwt failed:", e);
    return null;
  }
}

export function getEmailFromToken(token) {
  const payload = decodeJwt(token);
  if (!payload) return null;
  return (
    payload.email ||
    payload.preferred_username ||
    payload.username ||
    payload.sub ||
    null
  );
}

export function getNameFromToken(token) {
  const payload = decodeJwt(token);
  if (!payload) return null;
  return payload.name || payload.preferred_username || payload.username || null;
}

export function getIdFromToken(token) {
  const payload = decodeJwt(token);
  if (!payload) return null;
  return payload.id || payload.userId || payload.user_id || null;
}
