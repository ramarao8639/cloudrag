import { useCallback, useEffect, useRef, useState } from "react";
import "./App.css";
import AuthScreen from "./components/auth/AuthScreen";
import ChatArea from "./components/chat/ChatArea";
import DocumentPanel from "./components/documents/DocumentPanel";
import Sidebar from "./components/sidebar/Sidebar";
import { useAuth } from "./hooks/useAuth";
import { apiFetch } from "./services/api";
import type { Chat, DocumentFile, Message, RagResponse } from "./types";

function createConversationId() { return crypto.randomUUID(); }

function App() {
  const { token, username, loading: authLoading, error: authError, authenticate, logout } = useAuth();
  const [conversationId, setConversationId] = useState(() => localStorage.getItem("cloudrag_conversation_id") || createConversationId());
  const [selectedFileId, setSelectedFileId] = useState<string | null>(null);
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState<Message[]>([]);
  const [documents, setDocuments] = useState<DocumentFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [chats, setChats] = useState<Chat[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => { localStorage.setItem("cloudrag_conversation_id", conversationId); }, [conversationId]);

  const loadMessages = useCallback(async () => {
    const response = await apiFetch(`/api/conversations/${conversationId}/messages`);
    if (!response.ok) throw new Error("Failed to load messages");
    const data = await response.json();
    setMessages(data.map((message: { id: string; role: "USER" | "ASSISTANT"; content: string }) => ({ id: message.id, role: message.role === "USER" ? "user" : "assistant", content: message.content })));
  }, [conversationId]);

  const loadDocuments = useCallback(async () => {
    const response = await apiFetch(`/api/conversations/${conversationId}/documents`);
    if (!response.ok) throw new Error("Failed to load documents");
    const data = await response.json();
    setDocuments(data.map((document: { fileId: string; fileName: string; chunks: number }) => ({ fileId: document.fileId, fileName: document.fileName, chunks: document.chunks })));
  }, [conversationId]);

  const loadConversations = useCallback(async () => {
    const response = await apiFetch("/api/conversations");
    if (!response.ok) throw new Error("Failed to load conversations");
    const data = await response.json();
    setChats(data.map((conversation: { id: string; title: string }) => ({ id: conversation.id, title: conversation.title })));
  }, []);

  useEffect(() => {
    if (!token) return;
    // Data loading is intentionally triggered when the authenticated conversation changes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    Promise.all([loadMessages(), loadDocuments(), loadConversations()]).catch((error) => console.error("Failed to load CloudRAG data:", error));
  }, [token, loadMessages, loadDocuments, loadConversations]);

  const newChat = async () => {
    try {
      const response = await apiFetch("/api/conversations", { method: "POST" });
      if (!response.ok) throw new Error("Failed to create conversation");
      const data = await response.json();
      setConversationId(data.id);
      setQuestion(""); setMessages([]); setDocuments([]); setSelectedFileId(null);
      setChats((previous) => [{ id: data.id, title: data.title || "New conversation" }, ...previous]);
    } catch (error) { console.error(error); alert("Could not create a new conversation."); }
  };

  const selectChat = (id: string) => { setConversationId(id); setQuestion(""); setMessages([]); setDocuments([]); setSelectedFileId(null); };

  const askQuestion = async () => {
    const trimmedQuestion = question.trim();
    if (!trimmedQuestion || loading) return;
    setMessages((previous) => [...previous, { id: crypto.randomUUID(), role: "user", content: trimmedQuestion }]);
    setQuestion(""); setLoading(true);
    try {
      const params = new URLSearchParams({ question: trimmedQuestion, conversationId });
      if (selectedFileId) params.append("fileId", selectedFileId);
      const response = await apiFetch(`/api/rag?${params.toString()}`);
      if (!response.ok) throw new Error("RAG request failed");
      const data: RagResponse = await response.json();
      setMessages((previous) => [...previous, { id: crypto.randomUUID(), role: "assistant", content: data.answer, sources: data.sources }]);
      await loadConversations();
    } catch (error) {
      console.error(error);
      setMessages((previous) => [...previous, { id: crypto.randomUUID(), role: "assistant", content: "I couldn't connect to CloudRAG. Please make sure Spring Boot is running on port 8080." }]);
    } finally { setLoading(false); }
  };

  const uploadPdf = async (file: File) => {
    if (file.type !== "application/pdf") { alert("Please select a PDF file."); return; }
    setUploading(true);
    try {
      const formData = new FormData(); formData.append("file", file); formData.append("conversationId", conversationId);
      const response = await apiFetch("/api/pdf/upload", { method: "POST", body: formData });
      if (!response.ok) throw new Error("Upload failed");
      const data = await response.json();
      setDocuments((previous) => [...previous, { fileId: data.fileId, fileName: data.fileName, chunks: data.chunks }]);
    } catch (error) { console.error(error); alert("PDF upload failed."); }
    finally { setUploading(false); }
  };

  const deleteDocument = async (documentFile: DocumentFile) => {
    if (!window.confirm(`Delete ${documentFile.fileName}?`)) return;
    try {
      const response = await apiFetch(`/api/documents?conversationId=${encodeURIComponent(conversationId)}&fileId=${encodeURIComponent(documentFile.fileId)}`, { method: "DELETE" });
      if (!response.ok) throw new Error("Delete failed");
      setDocuments((previous) => previous.filter((document) => document.fileId !== documentFile.fileId));
      if (selectedFileId === documentFile.fileId) setSelectedFileId(null);
    } catch (error) { console.error(error); alert("Could not delete the document."); }
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) void uploadPdf(file);
    event.target.value = "";
  };

  if (!token) return <AuthScreen loading={authLoading} error={authError} onAuthenticate={authenticate} />;

  return (
    <div className="app">
      <Sidebar chats={chats} conversationId={conversationId} username={username} onNewChat={newChat} onSelectChat={selectChat} onLogout={logout} />
      <ChatArea messages={messages} question={question} loading={loading} uploading={uploading} onQuestionChange={setQuestion} onAsk={askQuestion} onUploadClick={() => fileInputRef.current?.click()} />
      <DocumentPanel documents={documents} selectedFileId={selectedFileId} uploading={uploading} fileInputRef={fileInputRef} onUploadClick={() => fileInputRef.current?.click()} onFileChange={handleFileChange} onSelect={(fileId) => setSelectedFileId(selectedFileId === fileId ? null : fileId)} onDelete={deleteDocument} />
    </div>
  );
}

export default App;
