import React from "react";
import "./PaginationControls.css";

export default function PaginationControls({
  page = 1,
  pageSize = 10,
  total = null,
  onPageChange,
  prevLabel = "Previous",
  nextLabel = "Next",
  disabled = false,
}) {
  const totalPages =
    typeof total === "number" ? Math.max(1, Math.ceil(total / pageSize)) : null;

  const canPrev = page > 1 && !disabled;
  const canNext = !disabled && (totalPages ? page < totalPages : true);
  const pageButtons = () => {
    if (!totalPages) return null;
    const buttons = [];
    const maxButtons = 7;
    if (totalPages <= maxButtons) {
      for (let i = 1; i <= totalPages; i++) buttons.push(i);
    } else {
      const windowSize = 3;
      const start = Math.max(2, page - windowSize);
      const end = Math.min(totalPages - 1, page + windowSize);
      buttons.push(1);
      if (start > 2) buttons.push("...");
      for (let i = start; i <= end; i++) buttons.push(i);
      if (end < totalPages - 1) buttons.push("...");
      buttons.push(totalPages);
    }
    return buttons.map((p, idx) => {
      if (p === "...") {
        return (
          <span key={`dots-${idx}`} className="pc-ellipsis">
            ...
          </span>
        );
      }
      const isActive = p === page;
      return (
        <button
          key={`pg-${p}`}
          className={`btn pc-page-btn ${isActive ? "active" : ""}`}
          onClick={() => onPageChange(p)}
          disabled={disabled || isActive}
          aria-current={isActive ? "page" : undefined}
        >
          {p}
        </button>
      );
    });
  };

  return (
    <div className="pc-pagination">
      <button
        className="btn btn-secondary pc-btn"
        onClick={() => onPageChange(Math.max(1, page - 1))}
        disabled={!canPrev}
        aria-label="Previous page"
      >
        {prevLabel}
      </button>

      <div className="pc-pages" aria-live="polite">
        {pageButtons()}
      </div>

      <button
        className="btn btn-secondary pc-btn"
        onClick={() => onPageChange(page + 1)}
        disabled={!canNext}
        aria-label="Next page"
      >
        {nextLabel}
      </button>
    </div>
  );
}
