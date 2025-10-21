import React from "react";
import rehypeRaw from "rehype-raw";
import rehypeSanitize, { defaultSchema } from "rehype-sanitize";

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

export function MarkdownLink({ node, ...props }) {
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
