export type Source = {
  id: string;
  fileName?: string;
  pageNumber?: number;
  chunkIndex?: number;
  distance?: number;
};

export type RagResponse = {
  question: string;
  answer: string;
  conversationId: string;
  sources: Source[];
};

export type DocumentFile = {
  fileId: string;
  fileName: string;
  chunks: number;
};

export type Message = {
  id: string;
  role: "user" | "assistant";
  content: string;
  sources?: Source[];
};

export type Chat = {
  id: string;
  title: string;
};

export type AuthResponse = {
  token: string;
  username: string;
};
