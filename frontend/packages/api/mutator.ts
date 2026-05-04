export type ApiRequest = {
  url: string;
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  params?: Record<string, unknown>;
  data?: unknown;
  headers?: Record<string, string>;
  signal?: AbortSignal;
};

let baseUrl = "";
let getAuthToken: () => string | null | Promise<string | null> = () => null;

export function configureApi(opts: {
  baseUrl: string;
  getAuthToken?: () => string | null | Promise<string | null>;
}) {
  baseUrl = opts.baseUrl;
  if (opts.getAuthToken) getAuthToken = opts.getAuthToken;
}

export async function apiFetch<T>(req: ApiRequest): Promise<T> {
  const qs = req.params
    ? "?" +
      new URLSearchParams(
        Object.entries(req.params).flatMap(([k, v]) => (v == null ? [] : [[k, String(v)]])),
      ).toString()
    : "";

  const token = await getAuthToken();
  const res = await fetch(`${baseUrl}${req.url}${qs}`, {
    method: req.method,
    signal: req.signal,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...req.headers,
    },
    body: req.data != null ? JSON.stringify(req.data) : undefined,
  });

  if (!res.ok) throw new ApiError(res.status, await res.text());
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: string,
  ) {
    super(`API ${status}: ${body}`);
  }
}
