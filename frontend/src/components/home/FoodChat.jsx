import React, { useState, useRef, useEffect } from "react";
import Spinner from "../commons/Spinner";
import ReactMarkdown from "react-markdown";

function FoodChat() {
  const [prompt, setPrompt] = useState("");
  const [chatResponse, setChatResponse] = useState("");
  const [messages, setMessages] = useState([]); // {id, role: 'user'|'assistant', content:''}
  const [lastMessageId, setLastMessageId] = useState(null);

  const [loading, setLoading] = useState(false);
  const outputRef = useRef(null);

  // auto-scroll to bottom when messages change
  useEffect(() => {
    if (outputRef.current) {
      // scroll to bottom smoothly
      outputRef.current.scrollTo({
        top: outputRef.current.scrollHeight,
        behavior: "smooth",
      });
    }
  }, [messages]);

  const askFoodAi = async () => {
    if (!prompt.trim()) return;

    // append user's message to history
    const userId = Date.now();
    const userMsg = { id: userId, role: "user", content: prompt };
    setMessages((m) => [...m, userMsg]);
    setLoading(true);

    const response = await fetch(
      "http://localhost:8080/api/v1/messages?prompt=" +
        encodeURIComponent(prompt),
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
      }
    );

    if (response.ok) {
      const data = await response.text();
      const assistantId = Date.now() + 1;
      const assistantMsg = {
        id: assistantId,
        role: "assistant",
        content: data,
      };
      setMessages((m) => [...m, assistantMsg]);
      setLastMessageId(assistantId);
      setChatResponse(data);
    } else {
      const err = "Error: Unable to fetch response";
      setChatResponse(err);
      const assistantId = Date.now() + 1;
      setMessages((m) => [
        ...m,
        { id: assistantId, role: "assistant", content: err },
      ]);
      setLastMessageId(assistantId);
    }

    setLoading(false);
    setPrompt("");
  };

  // clear highlight after a short delay
  useEffect(() => {
    if (!lastMessageId) return;
    const t = setTimeout(() => setLastMessageId(null), 1200);
    return () => clearTimeout(t);
  }, [lastMessageId]);

  return (
    <div>
      <h2>Hello, User</h2>
      <p>Ask me anything about food!</p>
      <div className="chat-controls">
        <textarea
          className="chat-input"
          placeholder="Ask about food, recipes, ingredients..."
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
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
                className={msg.id === lastMessageId ? "new-message" : ""}
                style={{
                  marginBottom: 12,
                  padding: 8,
                  background: msg.role === "assistant" ? "#fff" : "#f8f8f8",
                  borderRadius: 6,
                }}
              >
                <div style={{ fontSize: 12, color: "#666", marginBottom: 6 }}>
                  {msg.role === "assistant" ? "Assistant" : "You"}
                </div>
                <div>
                  <ReactMarkdown>{msg.content}</ReactMarkdown>
                </div>
                {msg.role === "assistant" && (
                  <div style={{ marginTop: 8, textAlign: "right" }}>
                    <button
                      onClick={() => {
                        // prepend each line with '> ' to create a Markdown quote
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
