import type { Chat } from "../../types";

type Props = {
  chats: Chat[];
  conversationId: string;
  username: string | null;
  onNewChat: () => void;
  onSelectChat: (id: string) => void;
  onLogout: () => void;
};

export default function Sidebar({ chats, conversationId, username, onNewChat, onSelectChat, onLogout }: Props) {
  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="brand-icon">✦</div>
        <div><strong>CloudRAG</strong><span>Knowledge Assistant</span></div>
      </div>

      <button className="new-chat" onClick={onNewChat}><span>＋</span>New Chat</button>

      <div className="sidebar-section">
        <div className="sidebar-label">Conversations</div>
        {chats.length === 0 ? <div className="empty-sidebar">No conversations yet</div> : chats.map((chat) => (
          <button className={chat.id === conversationId ? "chat-item active" : "chat-item"} key={chat.id} onClick={() => onSelectChat(chat.id)}>
            <span>◌</span>{chat.title}
          </button>
        ))}
      </div>

      <div className="sidebar-bottom">
        <div className="sidebar-label">Signed in as</div>
        <div className="conversation-id">{username || "User"}</div>
        <div className="conversation-id">{conversationId.substring(0, 8)}</div>
        <button className="logout-button" onClick={onLogout}>Logout</button>
        <div className="sidebar-footer">Local AI · Ollama</div>
      </div>
    </aside>
  );
}
