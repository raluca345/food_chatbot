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
import ContextMenu from "../context-menu/ContextMenu";

export default function ConversationList({ isOpen }) {
  const [conversations, setConversations] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [savingId, setSavingId] = useState(null);

  const [openMenuId, setOpenMenuId] = useState(null);
  const [menuAnchor, setMenuAnchor] = useState(null);

  const [editingId, setEditingId] = useState(null);
  const [editingTitle, setEditingTitle] = useState("");
  const [originalTitle, setOriginalTitle] = useState("");

  const inputRef = useRef(null);
  const mountedRef = useRef(true);

  const navigate = useNavigate();
  const location = useLocation();

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
      const el = inputRef.current;
      el.focus();

      const range = document.createRange();
      range.selectNodeContents(el);

      const sel = window.getSelection();
      sel.removeAllRanges();
      sel.addRange(range);
    }
  }, [editingId]);

  const selectedMatch = location.pathname.match(/\/home\/chat\/(.+)/);
  const selectedId = selectedMatch ? selectedMatch[1] : null;

  const startRename = (id, currentTitle) => {
    setOpenMenuId(null);
    setEditingId(id);
    setEditingTitle(currentTitle ?? "");
    setOriginalTitle(currentTitle ?? "");
  };

  const cancelRename = () => {
    setEditingId(null);
    setEditingTitle("");
  };

  const saveRename = async (id) => {
    const newTitle = editingTitle.trim();
    if (!newTitle) return;

    setSavingId(id);
    try {
      await renameConversation(id, newTitle);
      setConversations((prev) =>
        prev.map((c) =>
          String(c.conversationId) === String(id)
            ? { ...c, title: newTitle }
            : c
        )
      );
    } catch (e) {
      setEditingTitle(originalTitle);
      cancelRename();
      console.error(e.userMessage);
    } finally {
      setSavingId(null);
      cancelRename();
    }
  };

  const handleDelete = async (conversationId) => {
    setSavingId(conversationId);
    try {
      await deleteConversation(conversationId);
      await fetchConversations();
      if (String(selectedId) === String(conversationId)) {
        navigate("/home/chat");
      }
    } catch (e) {
      console.error("Delete failed", e);
    } finally {
      setSavingId(null);
      setOpenMenuId(null);
    }
  };

  return (
    <>
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
                  <button
                    className="conv-link"
                    onClick={() => !isEditing && navigate(`/home/chat/${id}`)}
                  >
                    <span
                      ref={isEditing ? inputRef : null}
                      className="conv-title"
                      contentEditable={isEditing}
                      suppressContentEditableWarning
                      spellCheck={false}
                      onInput={(e) =>
                        setEditingTitle(e.currentTarget.textContent)
                      }
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          e.preventDefault();
                          saveRename(id);
                        }
                        if (e.key === "Escape") {
                          e.preventDefault();
                          cancelRename();
                        }
                      }}
                      onBlur={() => isEditing && saveRename(id)}
                    >
                      {isEditing ? editingTitle : c.title}
                    </span>
                  </button>

                  {!isEditing && (
                    <button
                      className="conv-ellipsis"
                      onClick={(e) => {
                        e.stopPropagation();
                        setMenuAnchor(e.currentTarget.getBoundingClientRect());
                        setOpenMenuId(openMenuId === id ? null : id);
                      }}
                    >
                      <FaEllipsis className="inline-icon menu" />
                    </button>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}

      <ContextMenu
        open={!!openMenuId}
        anchorRect={menuAnchor}
        onClose={() => setOpenMenuId(null)}
      >
        <button
          className="context-menu-item rename"
          onClick={() => {
            const convo = conversations.find(
              (c) => String(c.conversationId) === String(openMenuId)
            );
            if (convo) startRename(openMenuId, convo.title);
          }}
        >
          Rename
        </button>

        <button
          className="context-menu-item delete"
          onClick={() => handleDelete(openMenuId)}
          disabled={savingId === openMenuId}
        >
          {savingId === openMenuId ? "Deleting…" : "Delete"}
        </button>
      </ContextMenu>
    </>
  );
}
