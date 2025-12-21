import React from "react";
import { useNavigate, useLocation, matchPath } from "react-router-dom";
import "./Tabs.css";

export default function Tabs() {
  const navigate = useNavigate();
  const location = useLocation();

  const getActiveTab = () => {
    const p = location.pathname;
    if (p === "/sign-up") return "sign-up";

    if (
      p === "/" ||
      p === "/home" ||
      p === "/home/chat" ||
      !!matchPath("/home/chat/:conversationId", p)
    )
      return "food-chat";

    if (p === "/home/image-generator") return "image-generator";

    if (p === "/home/recipe-generator") return "recipe-generator";

    return null;
  };

  const activeTab = getActiveTab();

  return (
    <nav className="tab-nav" role="tablist" aria-label="Primary">
      <button
        role="tab"
        aria-selected={activeTab === "image-generator"}
        className={activeTab === "image-generator" ? "active" : ""}
        onClick={() => navigate("/home/image-generator")}
      >
        Generate Food Image
      </button>

      <button
        role="tab"
        aria-selected={activeTab === "food-chat"}
        className={activeTab === "food-chat" ? "active" : ""}
        onClick={() => navigate("/home/chat")}
      >
        Talk About Food
      </button>

      <button
        role="tab"
        aria-selected={activeTab === "recipe-generator"}
        className={activeTab === "recipe-generator" ? "active" : ""}
        onClick={() => navigate("/home/recipe-generator")}
      >
        Generate a Recipe
      </button>
    </nav>
  );
}
