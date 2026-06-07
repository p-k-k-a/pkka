let baseUrl = "";
let getAuthToken: () => string | null | Promise<string | null> = () => null;
let onUnauthenticated: () => void = () => {};
let getRefreshToken: () => Promise<string | null>;
let onTokenRefreshed: (newAT: string, newRT: string) => void = () => {};

let refreshingTokenPromise: Promise<string> | null = null;

export function configureApi(opts: {
  baseUrl: string;
  getAuthToken?: () => string | null | Promise<string | null>;
  onUnauthenticated?: () => void;
  getRefreshToken?: () => Promise<string | null>;
  onTokenRefreshed?: (newAT: string, newRT: string) => void;
}) {
  baseUrl = opts.baseUrl.replace(/\/$/, "");
  if (opts.getAuthToken) getAuthToken = opts.getAuthToken;
  if (opts.onUnauthenticated) onUnauthenticated = opts.onUnauthenticated;
  if (opts.getRefreshToken) getRefreshToken = opts.getRefreshToken;
  if (opts.onTokenRefreshed) onTokenRefreshed = opts.onTokenRefreshed;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: unknown,
    public url: string,
  ) {
    super(`API ${status} ${url}: ${typeof body === "string" ? body : JSON.stringify(body)}`);
    this.name = "ApiError";
  }
}

const buildUrl = (path: string): string => {
  if (/^https?:\/\//i.test(path)) return path;
  return `${baseUrl}${path.startsWith("/") ? path : `/${path}`}`;
};

const parseBody = async <T>(res: Response): Promise<T> => {
  if (res.status === 204) return undefined as T;
  const ct = res.headers.get("content-type") ?? "";
  if (ct.includes("application/json")) return (await res.json()) as T;
  return (await res.text()) as T;
};

export const refreshTokens = async (): Promise<string> => {
  if (refreshingTokenPromise) return refreshingTokenPromise;

  const rt = await getRefreshToken();

  if (!rt) {
    onUnauthenticated();
    throw new Error("No refresh token");
  }

  refreshingTokenPromise = performTokenRefresh(rt);

  try {
    return await refreshingTokenPromise;
  } finally {
    refreshingTokenPromise = null;
  }
};

const performTokenRefresh = async (rt: string): Promise<string> => {
  const res = await fetch(buildUrl("/api/public/auth/refresh"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: rt }),
  });

  if (!res.ok) {
    onUnauthenticated();
    throw new Error("Refresh failed");
  }

  const data = (await res.json()) as { access_token: string; refresh_token: string };
  onTokenRefreshed(data.access_token, data.refresh_token);
  return data.access_token;
};

export const apiFetch = async <T>(url: string, options: RequestInit = {}): Promise<T> => {
  const fullUrl = buildUrl(url);
  const token = await getAuthToken();

  const headers = new Headers(options.headers);
  if (!headers.has("Content-Type") && options.body) headers.set("Content-Type", "application/json");
  if (token && !headers.has("Authorization")) headers.set("Authorization", `Bearer ${token}`);

  let res: Response;
  try {
    res = await fetch(fullUrl, { ...options, headers });
  } catch (e) {
    throw new ApiError(0, `network error: ${(e as Error).message}`, fullUrl);
  }

  let body = await parseBody<unknown>(res);

  if (res.status === 401) {
    const newAT = await refreshTokens();
    headers.set("Authorization", `Bearer ${newAT}`);
    res = await fetch(fullUrl, { ...options, headers });
    body = await parseBody<unknown>(res);
  }

  if (!res.ok) {
    throw new ApiError(res.status, body, fullUrl);
  }
  return { data: body, status: res.status, headers: res.headers } as T;
};
