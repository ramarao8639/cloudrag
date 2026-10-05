import ReactMarkdown from "react-markdown";
import type { Message } from "../../types";

type Props = {
  messages: Message[];
  question: string;
  loading: boolean;
  uploading: boolean;
  onQuestionChange: (value: string) => void;
  onAsk: () => void;
  onUploadClick: () => void;
};

export default function ChatArea({ messages, question, loading, uploading, onQuestionChange, onAsk, onUploadClick }: Props) {
  return (
    <main className="chat-area">
      <header className="chat-header"><div><h1>Knowledge Assistant</h1><span>Conversation-scoped RAG</span></div><div className="online-status"><span></span>Local AI</div></header>

      <div className="messages">
        {messages.length === 0 ? (
          <div className="welcome">
            <div className="welcome-icon">✦</div><h2>What can I help you find?</h2>
            <p>Upload documents to this conversation and ask questions about them.</p>
            <div className="suggestions">
              <button onClick={() => onQuestionChange("Summarize the uploaded documents")}>Summarize my documents</button>
              <button onClick={() => onQuestionChange("What are the key technologies mentioned?")}>Find key technologies</button>
              <button onClick={() => onQuestionChange("What are the main requirements?")}>Find requirements</button>
            </div>
          </div>
        ) : messages.map((message) => (
          <div className={message.role === "user" ? "message-row user-row" : "message-row"} key={message.id}>
            <div className={message.role === "user" ? "avatar user-avatar" : "avatar ai-avatar"}>{message.role === "user" ? "You" : "✦"}</div>
            <div className="message-content">
              <div className="message-role">{message.role === "user" ? "You" : "CloudRAG"}</div>
              <div className="message-text">{message.role === "assistant" ? <ReactMarkdown>{message.content}</ReactMarkdown> : message.content}</div>
              {message.sources && message.sources.length > 0 && (
                <div className="message-sources">
                  <div className="sources-heading">Sources</div>
                  {message.sources.map((source, index) => (
                    <div className="source" key={source.id}>
                      <span className="source-number">{index + 1}</span>
                      <div className="source-details"><strong>{source.fileName || "Knowledge source"}</strong><span>{source.pageNumber ? `Page ${source.pageNumber}` : "Document"}</span></div>
                      {source.distance !== undefined && <span className="relevance">Similarity {Math.max(0, Math.round((1 - source.distance) * 100))}%</span>}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}

        {loading && <div className="message-row"><div className="avatar ai-avatar">✦</div><div className="message-content"><div className="message-role">CloudRAG</div><div className="typing"><span></span><span></span><span></span></div></div></div>}
      </div>

      <div className="composer-wrapper">
        <div className="composer">
          <button className="attach-button" onClick={onUploadClick} title="Upload PDF" disabled={uploading}>📎</button>
          <textarea value={question} onChange={(event) => onQuestionChange(event.target.value)} placeholder={uploading ? "Indexing document..." : "Ask anything about your documents..."} disabled={loading || uploading} onKeyDown={(event) => { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); onAsk(); } }} />
          <button className="send-button" onClick={onAsk} disabled={loading || uploading || !question.trim()}>↑</button>
        </div>
        <div className="composer-hint">Enter to send · Shift + Enter for new line</div>
      </div>
    </main>
  );
}
