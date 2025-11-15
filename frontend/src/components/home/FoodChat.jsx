import React, { useState, useRef, useEffect, useMemo } from "react";
import Spinner from "../commons/Spinner";
import "./FoodChat.css";
import ReactMarkdown from "react-markdown";
import {
  rehypePlugins,
  markdownComponents,
} from "../../utils/sanitizeMarkdown";
import { generateMessage } from "../../api/homeApi";
import { getNameFromToken } from "../../utils/jwt";

function FoodChat() {
  const [prompt, setPrompt] = useState("");
  const [chatResponse, setChatResponse] = useState("");
  const [messages, setMessages] = useState([]); // {id, role: 'user'|'assistant', content:''}
  const [lastMessageId, setLastMessageId] = useState(null);

  const [loading, setLoading] = useState(false);
  const outputRef = useRef(null);

  const userName = useMemo(() => {
    const token = localStorage.getItem("token");
    if (!token) return null;
    return getNameFromToken(token);
  }, []);

  useEffect(() => {
    if (outputRef.current) {
      outputRef.current.scrollTo({
        top: outputRef.current.scrollHeight,
        behavior: "smooth",
      });
    }
  }, [messages]);

  const askFoodAi = async () => {
    if (!prompt.trim()) return;

    const userId = Date.now();
    const userMsg = { id: userId, role: "user", content: prompt };
    setMessages((m) => [...m, userMsg]);
    setLoading(true);

    try {
      const data = await generateMessage(prompt);
      const assistantId = Date.now() + 1;
      const assistantMsg = {
        id: assistantId,
        role: "assistant",
        content: data,
      };
      setMessages((m) => [...m, assistantMsg]);
      setLastMessageId(assistantId);
      setChatResponse(data);
    } catch (err) {
      const errTxt = err?.message || "Error: Unable to fetch response";
      setChatResponse(errTxt);
      const assistantId = Date.now() + 1;
      setMessages((m) => [
        ...m,
        { id: assistantId, role: "assistant", content: errTxt },
      ]);
      setLastMessageId(assistantId);
    }

    setLoading(false);
    setPrompt("");
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
              askFoodAi();
            }
          }}
        />
        <div className="chat-actions">
          <button className="primary" onClick={askFoodAi}>
            Send
          </button>
        </div>
      </div>
      {loading && <Spinner />}
      <div className="output">
        <div className="recipe-text" ref={outputRef}>
          {messages.length === 0 ? (
            <p>Start the conversation by asking a question.</p>
          ) : (
            messages.map((msg) => (
              <div
                key={msg.id}
                className={
                  (msg.id === lastMessageId ? "new-message " : "") +
                  (msg.role === "assistant"
                    ? "message assistant"
                    : "message user")
                }
              >
                <div className="message-meta">
                  {msg.role === "assistant" ? "Assistant" : "You"}
                </div>
                <div className="message-body">
                  <ReactMarkdown
                    rehypePlugins={rehypePlugins}
                    components={markdownComponents}
                  >
                    {msg.content}
                  </ReactMarkdown>
                </div>
                {msg.role === "assistant" && (
                  <div className="reply-btn-wrap">
                    <button
                      onClick={() => {
                        const quoted = msg.content
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
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

export default FoodChat;
