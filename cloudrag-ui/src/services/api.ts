const API_BASE_URL = "http://localhost:8080";

export function getToken(): string | null {
  return localStorage.getItem("cloudrag_token");
}

export function authHeaders(): HeadersInit {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function apiFetch(path: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers);
  const auth = authHeaders();

  Object.entries(auth).forEach(([key, value]) => headers.set(key, String(value)));

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401 || response.status === 403) {
    localStorage.removeItem("cloudrag_token");
    localStorage.removeItem("cloudrag_username");
    window.dispatchEvent(new Event("cloudrag:logout"));
  }

  return response;
}

export { API_BASE_URL };
