import React from "react";
import rehypeRaw from "rehype-raw";
import rehypeSanitize, { defaultSchema } from "rehype-sanitize";
import { downloadRecipeFromMarkdownLink } from "../api/generationApi";

const schema = {
  ...defaultSchema,
  attributes: {
    ...defaultSchema.attributes,
    a: [
      ...(defaultSchema.attributes.a || []),
      ["href", "target", "rel", "download"],
    ],
  },
};

const rehypePlugins = [[rehypeRaw], [rehypeSanitize, schema]];

function isSafeMarkdownUrl(url) {
  const value = String(url || "");
  const colon = value.indexOf(":");
  const slash = value.indexOf("/");
  const hash = value.indexOf("#");
  const query = value.indexOf("?");

  // Relative URLs are safe.
  if (
    colon === -1 ||
    (slash !== -1 && colon > slash) ||
    (hash !== -1 && colon > hash) ||
    (query !== -1 && colon > query)
  ) {
    return true;
  }

  const protocol = value.slice(0, colon).toLowerCase();
  return ["http", "https", "irc", "ircs", "mailto", "xmpp"].includes(protocol);
}

export function markdownUrlTransform(url) {
  const value = String(url || "");
  return isSafeMarkdownUrl(value) ? value : "";
}

export function MarkdownLink({ node, ...props }) {
  const rawHref = String(props?.href || "");
  let href = rawHref;
  try {
    href = decodeURIComponent(rawHref);
  } catch (e) {
    // Keep raw href when decode fails.
  }

  const isStoredRecipeDownloadLink = /\/api\/v1\/recipes\/download\/\d+(?:\?.*)?$/i.test(href);
  const isGuestRecipeDownloadLink = /\/api\/v1\/recipes\/download\/guest\/?(?:\?.*)?$/i.test(href);
  const isRecipeDownloadLink = isStoredRecipeDownloadLink || isGuestRecipeDownloadLink;

  if (isRecipeDownloadLink) {
    return React.createElement("a", {
      ...props,
      onClick: async (e) => {
        e.preventDefault();
        e.stopPropagation();
        try {
          await downloadRecipeFromMarkdownLink(href);
        } catch (err) {
          console.error("Recipe download failed:", err);
        }
      },
    });
  }

  return React.createElement("a", {
    ...props,
    target: "_blank",
    rel: "noopener noreferrer",
  });
}

export const markdownComponents = {
  a: MarkdownLink,
};

export { rehypePlugins, schema };
