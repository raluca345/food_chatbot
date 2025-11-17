import React, { useState, useRef, useEffect } from "react";
import "./RecipeHistory.css";
import { FaAngleDown } from "react-icons/fa";
import { LuDownload } from "react-icons/lu";
import { FaTrashCan } from "react-icons/fa6";
import ReactMarkdown from "react-markdown";
import {
  rehypePlugins,
  markdownComponents,
} from "../../utils/sanitizeMarkdown";
import {
  getUserRecipeHistory,
  deleteRecipeHistoryEntry,
  downloadRecipeFromHistory,
} from "../../api/homeApi";
import Spinner from "../commons/Spinner";
import ConfirmModal from "../commons/ConfirmModal";
import PaginationControls from "../commons/PaginationControls";

export default function RecipeHistory() {
  const [historyEntries, setHistoryEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [expandedIndex, setExpandedIndex] = useState(null);
  const [deletingIds, setDeletingIds] = useState(new Set());
  const [downloadingIds, setDownloadingIds] = useState(new Set());
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 6;
  const [total, setTotal] = useState(null);

  const panelRefs = useRef({});

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError(null);
    getUserRecipeHistory({ page, pageSize: PAGE_SIZE })
      .then((res) => {
        if (!mounted) return;
        setHistoryEntries(Array.isArray(res.items) ? res.items : []);
        setTotal(typeof res.total === "number" ? res.total : null);
        setExpandedIndex(null);
      })
      .catch((err) => {
        if (!mounted) return;
        console.error("Failed to load history:", err);
        setError(err?.userMessage || "Failed to load recipe history. Please try again later.");
      })
      .finally(() => {
        if (!mounted) return;
        setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [page]);

  const handleToggle = (index) => {
    setExpandedIndex((prev) => (prev === index ? null : index));
  };

  const handleDelete = async (entryId, index) => {
    const prev = historyEntries;
    const next = prev.filter((e) => e.id !== entryId);
    setHistoryEntries(next);
    setDeletingIds((s) => new Set(s).add(entryId));
    try {
      await deleteRecipeHistoryEntry(entryId);
      setDeletingIds((s) => {
        const n = new Set(s);
        n.delete(entryId);
        return n;
      });
      if (next.length === 0 && page > 1) {
        setPage((p) => p - 1);
      }
      } catch (err) {
      setHistoryEntries(prev);
      setDeletingIds((s) => {
        const n = new Set(s);
        n.delete(entryId);
        return n;
      });
        console.error("Failed to delete history entry:", err);
        setError(err?.userMessage || "Failed to delete entry. Please try again.");
    }
  };

  const [confirmDelete, setConfirmDelete] = useState(null);

  const openDeleteModal = (entry) => {
    setConfirmDelete({ id: entry.id, title: entry.title });
  };

  const closeDeleteModal = () => setConfirmDelete(null);

  const confirmAndDelete = async () => {
    if (!confirmDelete) return;
    const id = confirmDelete.id;
    closeDeleteModal();
    await handleDelete(id);
  };

  const handleDownload = (entry) => {
    setDownloadingIds((s) => new Set(s).add(entry.id));
    downloadRecipeFromHistory(entry.id)
      .then(({ blob, filename }) => {
        try {
          const url = URL.createObjectURL(blob);
          const a = document.createElement("a");
          a.href = url;
          a.download =
            filename ||
            (entry.title || "recipe").replace(/[^a-z0-9-_\.]/gi, "_") + ".txt";
          document.body.appendChild(a);
          a.click();
          a.remove();
          URL.revokeObjectURL(url);
        } catch (err) {
          console.error("Download failed", err);
          setError(err?.userMessage || "Failed to download recipe. Please try again.");
        }
      })
      .catch((err) => {
        console.error("Download request failed", err);
        setError(err?.userMessage || "Failed to download recipe. Please try again.");
      })
      .finally(() => {
        setDownloadingIds((s) => {
          const n = new Set(s);
          n.delete(entry.id);
          return n;
        });
      });
  };

  useEffect(() => {
    Object.keys(panelRefs.current).forEach((k) => {
      const idx = Number(k);
      const panel = panelRefs.current[k];
      if (!panel) return;
      if (expandedIndex === idx) {
        panel.style.maxHeight = panel.scrollHeight + "px";
        const onEnd = () => {
          panel.style.maxHeight = "none";
          panel.removeEventListener("transitionend", onEnd);
        };
        panel.addEventListener("transitionend", onEnd);
      } else {
        panel.style.maxHeight = panel.scrollHeight + "px";
        requestAnimationFrame(() => {
          panel.style.maxHeight = "0px";
        });
      }
    });
  }, [expandedIndex, historyEntries]);

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
          <li className="history-entry" key={entry.id ?? index}>
            <button
              className="accordion"
              onClick={() => handleToggle(index)}
              aria-expanded={expandedIndex === index}
            >
              <FaAngleDown
                className={`inline-icon arrow ${
                  expandedIndex === index ? "expanded" : ""
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
              ref={(el) => (panelRefs.current[index] = el)}
              className={`panel ${expandedIndex === index ? "open" : ""}`}
              aria-hidden={expandedIndex !== index}
            >
              <ReactMarkdown
                rehypePlugins={rehypePlugins}
                components={markdownComponents}
              >
                {entry.content}
              </ReactMarkdown>
            </div>
          </li>
        ))}
      </ul>
      <PaginationControls
        page={page}
        pageSize={PAGE_SIZE}
        total={total}
        onPageChange={(p) => setPage(p)}
        disabled={loading}
      />
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
