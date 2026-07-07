import React from "react";
import { useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import Spinner from "../commons/Spinner";
import "./FoodChat.css";
import {
  rehypePlugins,
  markdownComponents,
  markdownUrlTransform,
} from "../../utils/sanitizeMarkdown";
import useChat from "../../hooks/useChat";

function FoodChat() {
  const { conversationId } = useParams();

  const {
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
  } = useChat(conversationId);

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
