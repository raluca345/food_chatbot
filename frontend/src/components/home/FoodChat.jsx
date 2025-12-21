import React, { useState, useRef, useEffect, useMemo } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import ReactMarkdown from "react-markdown";

import Spinner from "../commons/Spinner";
import "./FoodChat.css";

import {
  rehypePlugins,
  markdownComponents,
} from "../../utils/sanitizeMarkdown";

import {
  startConversation,
  sendMessageToConversation,
  loadConversation,
} from "../../api/homeApi";

import { getIdFromToken, getNameFromToken } from "../../utils/jwt";

function FoodChat() {
  const { conversationId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [prompt, setPrompt] = useState("");
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [lastMessageId, setLastMessageId] = useState(null);

  const outputRef = useRef(null);

  const userName = useMemo(() => {
    const token = localStorage.getItem("token");
    return token ? getNameFromToken(token) : null;
  }, []);

  const userId = useMemo(() => {
    const token = localStorage.getItem("token");
    return token ? getIdFromToken(token) : null;
  }, []);

  useEffect(() => {
    if (!conversationId) return;

    async function load() {
      try {
        const convo = await loadConversation(conversationId);
        setMessages(
          convo.messages.map((m) => ({
            id: m.id,
            role: m.role.toLowerCase(),
            content: m.content,
          }))
        );
      } catch (e) {
        console.error("Failed to load conversation", e);
      }
    }

    load();
  }, [conversationId]);

  useEffect(() => {
    if (!conversationId) {
      setMessages([]);
      setLastMessageId(null);
      setLoading(false);
    }
  }, [conversationId, location.pathname]);

  useEffect(() => {
    if (!outputRef.current) return;
    outputRef.current.scrollTo({
      top: outputRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [messages]);

  const sendMessage = async () => {
    if (!prompt.trim() || loading) return;

    const userMsg = {
      id: userId,
      role: "user",
      content: prompt,
    };

    setMessages((m) => [...m, userMsg]);
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

      const assistantMsg = {
        id: Date.now() + 1,
        role: "assistant",
        content: response.assistantMessage,
      };

      setMessages((m) => [...m, assistantMsg]);
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
    } finally {
      setLoading(false);
    }
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
        <div className="recipe-text" ref={outputRef}>
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
