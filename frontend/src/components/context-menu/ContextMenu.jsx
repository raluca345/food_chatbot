import { useEffect } from "react";
import { createPortal } from "react-dom";
import "./ContextMenu.css";

export default function ContextMenu({
  anchorRect,
  open,
  onClose,
  children,
  width = 140,
  offset = 6,
}) {
  useEffect(() => {
    if (!open) return;

    function handlePointerDown() {
      onClose?.();
    }

    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, [open, onClose]);

  if (!open || !anchorRect) return null;

  const top = anchorRect.bottom + offset;
  const left = anchorRect.right - width;

  return createPortal(
    <div
      className="context-menu"
      style={{
        top,
        left,
        width,
      }}
      onPointerDown={(e) => e.stopPropagation()}
      role="menu"
    >
      {children}
    </div>,
    document.body
  );
}
