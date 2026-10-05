import { useEffect, useState } from "react";
import { login, register } from "../services/authService";

export function useAuth() {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem("cloudrag_token"));
  const [username, setUsername] = useState<string | null>(() => localStorage.getItem("cloudrag_username"));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const handleLogout = () => {
      setToken(null);
      setUsername(null);
    };

    window.addEventListener("cloudrag:logout", handleLogout);
    return () => window.removeEventListener("cloudrag:logout", handleLogout);
  }, []);

  const authenticate = async (mode: "login" | "register", values: { username: string; password: string; email?: string }) => {
    setLoading(true);
    setError("");

    try {
      const result = mode === "login" ? await login(values) : await register(values);
      localStorage.setItem("cloudrag_token", result.token);
      localStorage.setItem("cloudrag_username", result.username);
      setToken(result.token);
      setUsername(result.username);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Authentication failed");
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem("cloudrag_token");
    localStorage.removeItem("cloudrag_username");
    setToken(null);
    setUsername(null);
  };

  return { token, username, loading, error, authenticate, logout };
}
