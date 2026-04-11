import { useState, useEffect, useRef } from "react";
import "./ConversationList.css";
import Spinner from "../commons/Spinner";
import {
  deleteConversation,
  loadConversations,
  renameConversation,
} from "../../api/chatApi";
import { useNavigate, useLocation } from "react-router-dom";
import { FaEllipsis } from "react-icons/fa6";
import ContextMenu from "../context-menu/ContextMenu";

const PAGE_SIZE = 20;

export default function ConversationList({ isOpen }) {
  const [conversations, setConversations] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);

  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
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

  const mergeUniqueByConversationId = (current, incoming) => {
    const seen = new Set(current.map((c) => String(c.conversationId)));
    const merged = [...current];

    incoming.forEach((item) => {
      const id = String(item?.conversationId);
      if (!seen.has(id)) {
        seen.add(id);
        merged.push(item);
      }
    });

    return merged;
  };

  const fetchConversations = async (pageToFetch = 1, append = false) => {
    if (append) {
      setIsLoadingMore(true);
    } else {
      setIsLoading(true);
    }

    try {
      const data = await loadConversations({
        page: pageToFetch,
        pageSize: PAGE_SIZE,
      });
      const items = data.items;

      if (!mountedRef.current) return;

      setConversations((prev) =>
        append ? mergeUniqueByConversationId(prev, items) : items,
      );
      setPage(pageToFetch);
      setTotal(data.total);
    } catch (e) {
      console.error("Failed to load conversations", e);
    } finally {
      if (!mountedRef.current) return;
      if (append) {
        setIsLoadingMore(false);
      } else {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    mountedRef.current = true;
    fetchConversations(1, false);
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
    if (!newTitle) {
      cancelRename();
      return;
    }

    setSavingId(id);
    try {
      await renameConversation(id, newTitle);
      setConversations((prev) =>
        prev.map((c) =>
          String(c.conversationId) === String(id)
            ? { ...c, title: newTitle }
            : c,
        ),
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
      setConversations((prev) =>
        prev.filter((c) => String(c.conversationId) !== String(conversationId)),
      );
      setTotal((prevTotal) => Math.max(0, prevTotal - 1));
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

  const hasMore = conversations.length < total;

  const handleLoadMore = () => {
    if (isLoading || isLoadingMore || !hasMore) return;
    fetchConversations(page + 1, true);
  };

  return (
    <>
      {isLoading && <Spinner />}

      {isOpen && (
        <div className="conversation-list-wrap">
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
                          setMenuAnchor(
                            e.currentTarget.getBoundingClientRect(),
                          );
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

          {hasMore && (
            <div className="conv-load-more-wrap">
              <button
                className="conv-load-more"
                onClick={handleLoadMore}
                disabled={isLoading || isLoadingMore}
              >
                {isLoadingMore ? (
                  <>
                    <span
                      className="conv-mini-spinner"
                      aria-hidden="true"
                    ></span>
                    <span>Loading more</span>
                  </>
                ) : (
                  "Load more"
                )}
              </button>
            </div>
          )}
        </div>
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
              (c) => String(c.conversationId) === String(openMenuId),
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
