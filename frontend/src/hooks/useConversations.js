import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  deleteConversation,
  loadConversations,
  renameConversation,
} from "../api/chatApi";

const PAGE_SIZE = 20;

export default function useConversations() {
  const [conversations, setConversations] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [savingId, setSavingId] = useState(null);
  const [openMenuId, setOpenMenuId] = useState(null);
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [originalTitle, setOriginalTitle] = useState("");

  const inputRef = useRef(null);
  const editingTitleRef = useRef("");
  const mountedRef = useRef(true);

  const navigate = useNavigate();
  const location = useLocation();

  const selectedMatch = location.pathname.match(/\/home\/chat\/(.+)/);
  const selectedId = selectedMatch ? selectedMatch[1] : null;

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

  const fetchConversations = useCallback(async (pageToFetch = 1, append = false) => {
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
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    fetchConversations(1, false);
    return () => {
      mountedRef.current = false;
    };
  }, [fetchConversations]);

  useEffect(() => {
    if (editingId && inputRef.current) {
      const el = inputRef.current;
      el.textContent = originalTitle;
      el.focus();

      const range = document.createRange();
      range.selectNodeContents(el);
      range.collapse(false);

      const sel = window.getSelection();
      sel.removeAllRanges();
      sel.addRange(range);
    }
  }, [editingId, originalTitle]);

  const startRename = (id, currentTitle) => {
    setOpenMenuId(null);
    setEditingId(id);
    editingTitleRef.current = currentTitle ?? "";
    setOriginalTitle(currentTitle ?? "");
  };

  const cancelRename = () => {
    setEditingId(null);
  };

  const saveRename = async (id) => {
    const newTitle = (inputRef.current?.textContent ?? "").trim();
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

  return {
    conversations,
    isLoading,
    isLoadingMore,
    savingId,
    openMenuId,
    menuAnchor,
    editingId,
    selectedId,
    inputRef,
    editingTitleRef,
    hasMore,
    setOpenMenuId,
    setMenuAnchor,
    startRename,
    cancelRename,
    saveRename,
    handleDelete,
    handleLoadMore,
  };
}
