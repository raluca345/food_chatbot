import React from "react";
import { FaBars, FaImages } from "react-icons/fa";
import { GiCook } from "react-icons/gi";
import "./Sidebar.css";
import { useNavigate } from "react-router";

export default function Sidebar({ isOpen, onToggle }) {
  const handleExpand = () => {};
  const navigate = useNavigate();

  const renderLabel = (text) => (
    <span className="sidebar-label" aria-hidden={!isOpen}>
      {text}
    </span>
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
            title="Images"
            onClick={() => navigate("/home/gallery")}
          />
          {renderLabel("Images")}
        </span>
        <span>
          <GiCook
            className="icon-inline sidebar-icon"
            title="Recipes history"
            onClick={() => navigate("/home/recipes-history")}
          />
          {renderLabel("Recipes history")}
        </span>
      </div>

      <div
        className={`sidebar-divider ${isOpen ? "visible" : ""}`}
        aria-hidden={!isOpen}
      />
    </div>
  );
}
