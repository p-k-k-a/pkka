let baseUrl = "";
let getAuthToken: () => string | null | Promise<string | null> = () => null;

export function configureApi(opts: {
  baseUrl: string;
  getAuthToken?: () => string | null | Promise<string | null>;
}) {
  baseUrl = opts.baseUrl.replace(/\/$/, "");
  if (opts.getAuthToken) getAuthToken = opts.getAuthToken;
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

  const body = await parseBody<unknown>(res);

  if (!res.ok) throw new ApiError(res.status, body, fullUrl);

  return { data: body, status: res.status, headers: res.headers } as T;
};
