import React, { useState, useEffect, useRef } from "react";
import { FaUser } from "react-icons/fa";
import { getEmailFromToken } from "../../utils/jwt";
import "./UserDropdown.css";

export default function UserDropdown({ token, onLogout }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  const email = token ? getEmailFromToken(token) : null;

  useEffect(() => {
    const onDocClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener("click", onDocClick);
    return () => document.removeEventListener("click", onDocClick);
  }, []);

  return (
    <div className="user-menu" ref={ref}>
      <FaUser
        className="icon-inline user-icon"
        onClick={() => setOpen((s) => !s)}
        size={24}
        aria-haspopup="true"
        aria-expanded={open}
      />

      {open && (
        <div className="dropdown-menu" role="menu">
          <div className="dropdown-header">
            {email ? `Logged in as ${email}` : "Logged in"}
          </div>
          <button
            onClick={() => {
              setOpen(false);
              onLogout();
            }}
          >
            Log Out
          </button>
        </div>
      )}
    </div>
  );
}
