import React, { useEffect, useRef } from "react";
import "./ConfirmModal.css";

export default function ConfirmModal({
  open,
  title = "Confirm",
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  onConfirm,
  onCancel,
  confirmDisabled = false,
  variant = "danger",
}) {
  const overlayRef = useRef(null);

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === "Escape" && open) onCancel?.();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, onCancel]);

  if (!open) return null;

  const handleOverlayClick = (e) => {
    if (e.target === overlayRef.current) {
      onCancel?.();
    }
  };

  return (
    <div
      className="cm-modal-overlay"
      role="dialog"
      aria-modal="true"
      onMouseDown={handleOverlayClick}
      ref={overlayRef}
    >
      <div className="cm-modal" role="document">
        {title ? <h3 className="cm-title">{title}</h3> : null}
        {message ? <p className="cm-message">{message}</p> : null}
        <div className="cm-actions">
          <button className="cm-btn cm-btn-secondary" onClick={onCancel}>
            {cancelLabel}
          </button>
          <button
            className={`cm-btn ${variant === "danger" ? "cm-btn-danger" : ""}`}
            onClick={onConfirm}
            disabled={confirmDisabled}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
