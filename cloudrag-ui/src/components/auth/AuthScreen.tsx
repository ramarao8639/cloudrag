import { useState } from "react";

type Props = {
  loading: boolean;
  error: string;
  onAuthenticate: (mode: "login" | "register", values: { username: string; password: string; email?: string }) => Promise<void>;
};

export default function AuthScreen({ loading, error, onAuthenticate }: Props) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    await onAuthenticate(mode, { username, email: mode === "register" ? email : undefined, password });
  };

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <div className="welcome-icon">✦</div>
        <h1>CloudRAG</h1>
        <p>Knowledge Assistant</p>

        <div className="auth-tabs">
          <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>Login</button>
          <button className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>Register</button>
        </div>

        <form onSubmit={submit} className="auth-form">
          <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Username" required />
          {mode === "register" && (
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email" required />
          )}
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Password" required minLength={6} />
          {error && <div className="auth-error">{error}</div>}
          <button type="submit" disabled={loading} className="auth-submit">
            {loading ? "Please wait..." : mode === "login" ? "Login" : "Create account"}
          </button>
        </form>
      </div>
    </div>
  );
}
