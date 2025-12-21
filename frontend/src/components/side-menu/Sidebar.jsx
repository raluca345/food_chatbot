import React from "react";
import { FaBars, FaImages } from "react-icons/fa";
import { GiCook } from "react-icons/gi";
import "./Sidebar.css";
import ConversationList from "./ConversationList";
import { useNavigate } from "react-router";

export default function Sidebar({ isOpen, onToggle }) {
  const navigate = useNavigate();

  const renderLabel = (text, onClick) => (
    <button
      type="button"
      className="sidebar-label"
      aria-hidden={!isOpen}
      onClick={onClick}
      tabIndex={isOpen ? 0 : -1}
    >
      {text}
    </button>
  );

  return (
    <div className={`sidebar ${isOpen ? "open" : "closed"}`}>
      <FaBars
        className={`icon-inline sidebar-toggle ${isOpen ? "open" : ""}`}
        onClick={onToggle}
        aria-label={isOpen ? "Close sidebar" : "Open sidebar"}
        aria-expanded={isOpen}
      />
      <div className="sidebar-group" role="group" aria-label="Sidebar actions">
        <span>
          <FaImages
            className="icon-inline sidebar-icon"
            title="Gallery"
            onClick={() => navigate("/me/gallery")}
          />
          {renderLabel("Gallery", () => navigate("/me/gallery"))}
        </span>
        <span>
          <GiCook
            className="icon-inline sidebar-icon"
            title="Recipe history"
            onClick={() => navigate("/me/recipe-history")}
          />
          {renderLabel("Recipe history", () => navigate("/me/recipe-history"))}
        </span>
        <span>
          <ConversationList isOpen={isOpen} onToggle={onToggle} />
        </span>
      </div>

      <div
        className={`sidebar-divider ${isOpen ? "visible" : ""}`}
        aria-hidden={!isOpen}
      />
    </div>
  );
}
