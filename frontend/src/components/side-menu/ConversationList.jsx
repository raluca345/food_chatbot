import { useState, useEffect, useRef } from "react";
import "./ConversationList.css";
import Spinner from "../commons/Spinner";
import {
  deleteConversation,
  loadConversations,
  renameConversation,
} from "../../api/homeApi";
import { useNavigate, useLocation } from "react-router-dom";
import { FaEllipsis } from "react-icons/fa6";

export default function ConversationList({ isOpen }) {
  const [conversations, setConversations] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [savingId, setSavingId] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();
  const [openMenuId, setOpenMenuId] = useState(null);

  const [editingId, setEditingId] = useState(null);
  const [editingTitle, setEditingTitle] = useState("");
  const inputRef = useRef(null);
  const mountedRef = useRef(true);
  const rootRef = useRef(null);
  const fetchConversations = async () => {
    setIsLoading(true);
    try {
      const data = await loadConversations();
      const items = Array.isArray(data) ? data : data?.items ?? [];
      if (mountedRef.current) setConversations(items);
    } catch (e) {
      console.error("Failed to load conversations", e);
    } finally {
      if (mountedRef.current) setIsLoading(false);
    }
  };

  useEffect(() => {
    mountedRef.current = true;
    fetchConversations();
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (editingId && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [editingId]);

  const selectedMatch = location.pathname.match(/\/home\/chat\/(.+)/);
  const selectedId = selectedMatch ? selectedMatch[1] : null;

  const startRename = (id, currentTitle) => {
    setOpenMenuId(null);
    setEditingId(id);
    setEditingTitle(currentTitle ?? "");
    // ensure input gets focus even if DOM updates are batched
    setTimeout(() => {
      try {
        if (inputRef.current) {
          inputRef.current.focus();
          inputRef.current.select();
        }
      } catch (err) {
        /* no-op */
      }
    }, 40);
  };

  const cancelRename = () => {
    setEditingId(null);
    setEditingTitle("");
  };

  const saveRename = async (id) => {
    const newTitle = String(editingTitle || "").trim();
    if (!newTitle) {
      return;
    }
    setSavingId(id);
    try {
      const res = await renameConversation(id, newTitle);
      setConversations((prev) =>
        prev.map((c) =>
          String(c.conversationId) === String(id)
            ? { ...c, title: newTitle }
            : c
        )
      );
      // refresh full list from server to pick up any ordering or metadata changes
      fetchConversations();
    } catch (e) {
      console.error("Rename failed", e);
    } finally {
      setSavingId(null);
      cancelRename();
    }
  };

  const handleDelete = async (conversationId) => {
    if (!conversationId) return;
    setSavingId(conversationId);
    try {
      await deleteConversation(conversationId);
      // refresh from server to ensure list consistency after delete
      await fetchConversations();
      if (String(selectedId) === String(conversationId)) {
        navigate(`/home/chat`);
      }
    } catch (e) {
      console.error("Failed to delete conversation.", e);
    } finally {
      setSavingId(null);
      setOpenMenuId(null);
    }
  };

  useEffect(() => {
    if (!selectedId) return;
    const found = conversations.some(
      (c) => String(c.conversationId) === String(selectedId)
    );
    if (!found) {
      fetchConversations();
    }
  }, [selectedId, conversations]);

  useEffect(() => {
    function handleDocPointer(e) {
      if (!openMenuId) return;
      if (rootRef.current && !rootRef.current.contains(e.target)) {
        setOpenMenuId(null);
      }
    }

    document.addEventListener("pointerdown", handleDocPointer);
    return () => document.removeEventListener("pointerdown", handleDocPointer);
  }, [openMenuId]);

  return (
    <>
      <div ref={rootRef}>
      {isLoading && <Spinner />}
      {isOpen && (
        <ul className="conversation-list">
          {conversations.map((c) => {
            const id = c.conversationId;
            const isSelected = String(id) === String(selectedId);
            const isEditing = String(editingId) === String(id);
            return (
              <li key={id}>
                <div
                  className={[
                    "conv-item",
                    isSelected ? "selected" : "",
                    isEditing ? "editing" : "",
                  ].join(" ")}
                >
                  {isEditing ? (
                    <input
                      ref={inputRef}
                      className="conv-rename-input"
                      value={editingTitle}
                      onChange={(e) => setEditingTitle(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          e.preventDefault();
                          saveRename(id);
                        } else if (e.key === "Escape") {
                          cancelRename();
                        }
                      }}
                      aria-label="Edit conversation title"
                      disabled={savingId === id}
                    />
                  ) : (
                    <button
                      type="button"
                      className="conv-link"
                      onClick={() => navigate(`/home/chat/${id}`)}
                      title={c.title}
                    >
                      <span className="conv-title" title={c.title}>
                        {c.title}
                      </span>
                    </button>
                  )}

                  <div className="conv-actions">
                    {!isEditing && (
                      <button
                        className="conv-ellipsis"
                        aria-haspopup="true"
                        aria-expanded={openMenuId === id}
                        onClick={(e) => {
                          e.stopPropagation();
                          setOpenMenuId(openMenuId === id ? null : id);
                        }}
                        title="Options"
                      >
                        <FaEllipsis className="inline-icon menu" />
                      </button>
                    )}

                    {openMenuId === id && !isEditing && (
                      <div
                        className="conv-menu"
                        role="menu"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <button
                          className="conv-menu-item rename"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={(e) => {
                            e.stopPropagation();
                            startRename(id, c.title);
                          }}
                        >
                          Rename
                        </button>
                        <button
                          className="conv-menu-item delete"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDelete(id);
                          }}
                          disabled={savingId === id}
                        >
                          {savingId === id ? "Deleting…" : "Delete"}
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}
      </div>
    </>
  );
}
