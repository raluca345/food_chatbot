import React from "react";
import "./RecipeHistory.css";
import { FaAngleDown } from "react-icons/fa";
import { LuDownload } from "react-icons/lu";
import { FaTrashCan } from "react-icons/fa6";
import ReactMarkdown from "react-markdown";
import {
  rehypePlugins,
  markdownComponents,
  markdownUrlTransform,
} from "../../utils/sanitizeMarkdown";
import Spinner from "../commons/Spinner";
import ConfirmModal from "../commons/ConfirmModal";
import PaginationControls from "../commons/PaginationControls";
import useRecipeHistory from "../../hooks/useRecipeHistory";

export default function RecipeHistory() {
  const {
    historyEntries,
    loading,
    error,
    expandedId,
    deletingIds,
    downloadingIds,
    page,
    total,
    PAGE_SIZE,
    confirmDelete,
    panelRefs,
    setPage,
    handleToggle,
    handleDelete,
    handleDownload,
    openDeleteModal,
    closeDeleteModal,
    confirmAndDelete,
  } = useRecipeHistory();

  return (
    <>
      <ul className="history-list">
        {loading && (
          <li>
            <Spinner />
          </li>
        )}
        {error && <li className="error">{error}</li>}
        {historyEntries.length === 0 && !loading && !error && (
          <li className="history-empty">
            You haven't generated any recipes yet.
          </li>
        )}
        {historyEntries.map((entry, index) => (
          <li className="history-entry" key={entry.id}>
            <button
              className="accordion"
              onClick={() => handleToggle(entry.id)}
              aria-expanded={expandedId === entry.id}
            >
              <FaAngleDown
                className={`inline-icon arrow ${
                  expandedId === entry.id ? "expanded" : ""
                }`}
              />
              <span className="actions">
                <button
                  className="action-btn"
                  aria-label={`Download ${entry.title}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDownload(entry);
                  }}
                  disabled={downloadingIds.has(entry.id)}
                >
                  {downloadingIds.has(entry.id) ? (
                    <span className="inline-spinner" aria-hidden="true"></span>
                  ) : (
                    <LuDownload className="inline-icon download" />
                  )}
                </button>
                <button
                  className="action-btn"
                  aria-label={`Delete ${entry.title}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    if (!deletingIds.has(entry.id)) openDeleteModal(entry);
                  }}
                  disabled={deletingIds.has(entry.id)}
                >
                  <FaTrashCan className="inline-icon delete" />
                </button>
              </span>
              {entry.title}
            </button>

            <div
              ref={(el) => (panelRefs.current[entry.id] = el)}
              className={`panel ${expandedId === entry.id ? "open" : ""}`}
              aria-hidden={expandedId !== entry.id}
            >
              <ReactMarkdown
                rehypePlugins={rehypePlugins}
                components={markdownComponents}
                urlTransform={markdownUrlTransform}
              >
                {entry.content}
              </ReactMarkdown>
            </div>
          </li>
        ))}
      </ul>
      {!loading && !error && historyEntries.length > 0 && (
        <PaginationControls
          page={page}
          pageSize={PAGE_SIZE}
          total={total}
          onPageChange={(p) => setPage(p)}
          disabled={loading}
        />
      )}
      <ConfirmModal
        open={!!confirmDelete}
        title="Confirm delete"
        message={
          confirmDelete
            ? `Are you sure you want to delete "${confirmDelete.title}"? This action cannot be undone.`
            : ""
        }
        confirmLabel={
          confirmDelete && deletingIds.has(confirmDelete.id)
            ? "Deleting..."
            : "Delete"
        }
        cancelLabel="Cancel"
        onConfirm={confirmAndDelete}
        onCancel={closeDeleteModal}
        confirmDisabled={
          confirmDelete ? deletingIds.has(confirmDelete.id) : false
        }
        variant="danger"
      />
    </>
  );
}
