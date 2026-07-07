import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  startConversation,
  sendMessageToConversation,
  loadConversation,
} from "../api/chatApi";
import { registerGuestRecipeDownload } from "../api/generationApi";
import { getIdFromToken, getNameFromToken } from "../utils/jwt";

const MESSAGE_PAGE_SIZE = 20;
const HISTORY_LOAD_THRESHOLD_PX = 120;

export default function useChat(conversationId) {
  const navigate = useNavigate();
  const location = useLocation();

  const [prompt, setPrompt] = useState("");
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [messagePage, setMessagePage] = useState(1);
  const [messageTotal, setMessageTotal] = useState(0);
  const [lastMessageId, setLastMessageId] = useState(null);

  const outputRef = useRef(null);
  const loadingHistoryRef = useRef(false);
  const prependSnapshotRef = useRef(null);
  const shouldScrollToBottomRef = useRef(false);

  const userName = useMemo(() => {
    const token = localStorage.getItem("token");
    return token ? getNameFromToken(token) : null;
  }, []);

  const userId = useMemo(() => {
    const token = localStorage.getItem("token");
    return token ? getIdFromToken(token) : null;
  }, []);

  const mapPagedMessageBatch = useCallback(
    (items) =>
      [...items].reverse().map((m) => ({
        id: m.id,
        role: String(m.role || "").toLowerCase(),
        content: m.content,
      })),
    []
  );

  const loadMessagePage = useCallback(
    async ({ page, prepend }) => {
      if (!conversationId) return;
      if (loadingHistoryRef.current) return;

      const container = outputRef.current;
      if (prepend && container) {
        prependSnapshotRef.current = {
          scrollTop: container.scrollTop,
          scrollHeight: container.scrollHeight,
        };
      }

      loadingHistoryRef.current = true;
      setLoadingHistory(true);
      try {
        const response = await loadConversation(conversationId, {
          page,
          pageSize: MESSAGE_PAGE_SIZE,
        });
        const normalizedBatch = mapPagedMessageBatch(response?.messages ?? []);

        setMessages((prev) =>
          prepend ? [...normalizedBatch, ...prev] : normalizedBatch
        );
        setMessagePage(page);
        setMessageTotal(Number(response?.total) || 0);

        if (!prepend) {
          shouldScrollToBottomRef.current = true;
        }
      } catch (e) {
        console.error("Failed to load conversation messages", e);
      } finally {
        loadingHistoryRef.current = false;
        setLoadingHistory(false);
      }
    },
    [conversationId, mapPagedMessageBatch]
  );

  useEffect(() => {
    if (!conversationId) {
      setMessages([]);
      setLastMessageId(null);
      setLoading(false);
      setLoadingHistory(false);
      setMessagePage(1);
      setMessageTotal(0);
      loadingHistoryRef.current = false;
      prependSnapshotRef.current = null;
      shouldScrollToBottomRef.current = false;
      return;
    }

    setMessages([]);
    setLastMessageId(null);
    setLoadingHistory(false);
    setMessagePage(1);
    setMessageTotal(0);
    loadingHistoryRef.current = false;
    prependSnapshotRef.current = null;
    shouldScrollToBottomRef.current = false;

    loadMessagePage({ page: 1, prepend: false });
  }, [conversationId, location.pathname, loadMessagePage]);

  useLayoutEffect(() => {
    const container = outputRef.current;
    if (!container) return;

    if (prependSnapshotRef.current) {
      const { scrollTop, scrollHeight } = prependSnapshotRef.current;
      const grownBy = container.scrollHeight - scrollHeight;
      container.scrollTop = scrollTop + grownBy;
      prependSnapshotRef.current = null;
      return;
    }

    if (shouldScrollToBottomRef.current) {
      container.scrollTop = container.scrollHeight;
      shouldScrollToBottomRef.current = false;
    }
  }, [messages]);

  const hasMoreHistory = messages.length < messageTotal;

  const handleOutputScroll = () => {
    const container = outputRef.current;
    if (!container) return;
    if (!conversationId || !hasMoreHistory || loadingHistoryRef.current) return;

    if (container.scrollTop <= HISTORY_LOAD_THRESHOLD_PX) {
      loadMessagePage({ page: messagePage + 1, prepend: true });
    }
  };

  const stripDownloadLine = (text) =>
    String(text || "").replace(
      /\n\nYou can download this recipe here:\s*\[Download recipe]\([^)]+\)\s*$/i,
      ""
    );

  const looksLikeRecipe = (text) => {
    const normalized = String(text || "").toLowerCase();
    return normalized.includes("ingredients") && normalized.includes("instructions");
  };

  const sendMessage = async () => {
    if (!prompt.trim() || loading) return;

    const userMsg = {
      id: Date.now(),
      role: "user",
      content: prompt,
    };

    setMessages((m) => [...m, userMsg]);
    shouldScrollToBottomRef.current = true;
    setPrompt("");
    setLoading(true);

    try {
      let response;

      if (!conversationId) {
        response = await startConversation(prompt);
        if (response && response.conversationId) {
          navigate(`/home/chat/${response.conversationId}`, { replace: true });
        }
      } else {
        response = await sendMessageToConversation(conversationId, prompt);
      }

      let assistantContent = response.assistantMessage;
      if (
        !userId &&
        looksLikeRecipe(assistantContent) &&
        !/\[Download recipe]\([^)]+\)/i.test(assistantContent)
      ) {
        const recipeMarkdown = stripDownloadLine(assistantContent);
        const guestHref = registerGuestRecipeDownload(recipeMarkdown);
        assistantContent = `${recipeMarkdown}\n\nYou can download this recipe here: [Download recipe](${guestHref})`;
      }

      const assistantMsg = {
        id: Date.now() + 1,
        role: "assistant",
        content: assistantContent,
      };

      setMessages((m) => [...m, assistantMsg]);
      shouldScrollToBottomRef.current = true;
      setLastMessageId(assistantMsg.id);
    } catch (err) {
      console.error(err);

      const errorMsg = {
        id: Date.now() + 1,
        role: "assistant",
        content:
          err?.userMessage || err?.message || "Error: Unable to fetch response",
      };

      setMessages((m) => [...m, errorMsg]);
      shouldScrollToBottomRef.current = true;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!lastMessageId) return;
    const t = setTimeout(() => setLastMessageId(null), 1200);
    return () => clearTimeout(t);
  }, [lastMessageId]);

  return {
    prompt,
    setPrompt,
    messages,
    loading,
    loadingHistory,
    lastMessageId,
    outputRef,
    hasMoreHistory,
    handleOutputScroll,
    sendMessage,
    userName,
  };
}
