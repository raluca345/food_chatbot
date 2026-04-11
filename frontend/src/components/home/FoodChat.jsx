import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import ReactMarkdown from "react-markdown";

import Spinner from "../commons/Spinner";
import "./FoodChat.css";

import {
  rehypePlugins,
  markdownComponents,
  markdownUrlTransform,
} from "../../utils/sanitizeMarkdown";

import {
  startConversation,
  sendMessageToConversation,
  loadConversation,
} from "../../api/chatApi";
import { registerGuestRecipeDownload } from "../../api/generationApi";

import { getIdFromToken, getNameFromToken } from "../../utils/jwt";

const MESSAGE_PAGE_SIZE = 20;
const HISTORY_LOAD_THRESHOLD_PX = 120;

function FoodChat() {
  const { conversationId } = useParams();
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

  const stripDownloadLine = (text) =>
    String(text || "").replace(
      /\n\nYou can download this recipe here:\s*\[Download recipe]\([^)]+\)\s*$/i,
      ""
    );

  const looksLikeRecipe = (text) => {
    const normalized = String(text || "").toLowerCase();
    return normalized.includes("ingredients") && normalized.includes("instructions");
  };

  useEffect(() => {
    if (!lastMessageId) return;
    const t = setTimeout(() => setLastMessageId(null), 1200);
    return () => clearTimeout(t);
  }, [lastMessageId]);

  return (
    <div>
      <h2>Hello, {userName || "User"}</h2>
      <p>Ask me anything about food!</p>

      <div className="chat-controls">
        <textarea
          className="chat-input"
          placeholder="Ask about food, recipes, ingredients..."
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              sendMessage();
            }
          }}
        />

        <div className="chat-actions">
          <button className="primary" onClick={sendMessage} disabled={loading}>
            Send
          </button>
        </div>
      </div>

      {loading && <Spinner />}

      <div className="output">
        <div className="recipe-text" ref={outputRef} onScroll={handleOutputScroll}>
          {loadingHistory && (
            <div className="history-loading">Loading earlier messages...</div>
          )}
          {messages.map((msg) => {
            let display = msg && msg.content ? msg.content : "";
            if (typeof display === "string") {
              try {
                const parsed = JSON.parse(display);
                if (parsed && typeof parsed === "object" && parsed.message) {
                  display = parsed.message;
                }
              } catch (e) {
                // leave display as-is when not JSON
              }
            }

            return (
              <div
                key={msg.id}
                className={[
                  "message",
                  msg.role,
                  msg.id === lastMessageId ? "new-message" : "",
                ].join(" ")}
              >
                <div className="message-meta">
                  {msg.role === "assistant" ? "Assistant" : "You"}
                </div>

                <div className="message-body">
                  <ReactMarkdown
                    rehypePlugins={rehypePlugins}
                    components={markdownComponents}
                    urlTransform={markdownUrlTransform}
                  >
                    {display}
                  </ReactMarkdown>
                </div>

                <div className="reply-btn-wrap">
                  <button
                    onClick={() => {
                      const quoted = String(display)
                        .replace(/\r/g, "")
                        .split("\n")
                        .map((l) => (l.trim() ? `> ${l}` : ">"))
                        .join("\n");
                      const withSpacing = quoted + "\n\n";
                      setPrompt((p) =>
                        p ? p + "\n" + withSpacing : withSpacing
                      );
                    }}
                  >
                    Reply
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default FoodChat;
