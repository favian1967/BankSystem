// Thin fetch wrapper around the Bank_System API.
// - Injects the Bearer token from storage.
// - Adds an Idempotency-Key for money-moving requests.
// - Normalizes error responses (backend returns ErrorResponse JSON or a raw string).

import type {
  AccountResponse,
  CardIssueResponse,
  CardResponse,
  CreateAccountRequest,
  CreateCardRequest,
  Currency,
  DepositRequest,
  LoginRequest,
  Page,
  RegisterRequest,
  TransactionResponse,
  TransferRequest,
  WithdrawRequest,
} from "./types";

const BASE = import.meta.env.VITE_API_URL ?? ""; // empty -> use Vite proxy (/api ...)
const TOKEN_KEY = "aurora_token";

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

type ReqOptions = {
  method?: string;
  body?: unknown;
  auth?: boolean;
  idempotent?: boolean;
  headers?: Record<string, string>;
};

async function request<T>(path: string, opts: ReqOptions = {}): Promise<T> {
  const { method = "GET", body, auth = true, idempotent = false, headers = {} } = opts;

  const h: Record<string, string> = { ...headers };
  if (body !== undefined) h["Content-Type"] = "application/json";
  if (auth) {
    const token = tokenStore.get();
    if (token) h["Authorization"] = `Bearer ${token}`;
  }
  if (idempotent) h["Idempotency-Key"] = crypto.randomUUID();

  let res: Response;
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers: h,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError("Network error — is the backend running on :8080?", 0);
  }

  const raw = await res.text();

  if (!res.ok) {
    let message = raw || `Request failed (${res.status})`;
    try {
      const parsed = JSON.parse(raw);
      message = parsed.message || parsed.error || message;
    } catch {
      /* keep raw text */
    }
    if (res.status === 401 && auth) {
      tokenStore.clear();
      window.dispatchEvent(new Event("aurora:unauthorized"));
    }
    // Backend blocks certain actions until the email is confirmed.
    if (res.status === 403 && /not\s*confirmed/i.test(message)) {
      window.dispatchEvent(new Event("aurora:unconfirmed"));
    }
    throw new ApiError(message, res.status);
  }

  if (!raw || res.status === 204) return undefined as T;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return raw as unknown as T;
  }
}

export const api = {
  // ---- Auth (returns a raw JWT string) ----
  register: (b: RegisterRequest) =>
    request<string>("/api/auth/register", { method: "POST", body: b, auth: false }),
  login: (b: LoginRequest) =>
    request<string>("/api/auth/login", { method: "POST", body: b, auth: false }),
  logout: () => request<{ message: string }>("/api/auth/logout", { method: "POST" }),
  sendEmailKey: () => request<void>("/api/auth/send", { method: "POST" }),
  confirmEmail: (key: string) =>
    request<boolean>("/api/auth/confirm", { method: "POST", body: { key } }),

  // ---- Accounts ----
  getAccounts: () => request<AccountResponse[]>("/api/accounts/getAll"),
  createAccount: (b: CreateAccountRequest) =>
    request<AccountResponse>("/api/accounts/add", { method: "POST", body: b }),
  totalBalance: (c: Currency) =>
    request<{ totalBalance: number; currency: string }>(`/api/accounts/totalBalance/${c}`),
  blockAccount: (id: number) =>
    request<AccountResponse>(`/api/accounts/${id}/block`, { method: "PATCH" }),
  unblockAccount: (id: number) =>
    request<AccountResponse>(`/api/accounts/${id}/unblock`, { method: "PATCH" }),
  closeAccount: (id: number) =>
    request<void>(`/api/accounts/${id}/close`, { method: "DELETE" }),

  // ---- Cards ----
  getCards: () => request<CardResponse[]>("/api/cards"),
  createCard: (b: CreateCardRequest) =>
    request<CardIssueResponse>("/api/cards", { method: "POST", body: b }),
  blockCard: (id: number) => request<CardResponse>(`/api/cards/block/${id}`, { method: "PATCH" }),
  unblockCard: (id: number) => request<CardResponse>(`/api/cards/unblock/${id}`, { method: "PATCH" }),
  deleteCard: (id: number) => request<void>(`/api/cards/${id}`, { method: "DELETE" }),

  // ---- Transactions (idempotent) ----
  deposit: (b: DepositRequest) =>
    request<TransactionResponse>("/api/transactions/deposit", { method: "POST", body: b, idempotent: true }),
  withdraw: (b: WithdrawRequest) =>
    request<TransactionResponse>("/api/transactions/withdraw", { method: "POST", body: b, idempotent: true }),
  transfer: (b: TransferRequest) =>
    request<TransactionResponse>("/api/transactions/transfer", { method: "POST", body: b, idempotent: true }),
  getTransactions: (accountId: number, page = 0, size = 20) =>
    request<Page<TransactionResponse>>(`/api/transactions/account/${accountId}?page=${page}&size=${size}`),
  getRecent: (accountId: number, limit = 6) =>
    request<TransactionResponse[]>(`/api/transactions/account/${accountId}/recent?limit=${limit}`),

  // ---- AI assistant ----
  askAssistant: (message: string) =>
    request<string>(`/api/helper/ask?message=${encodeURIComponent(message)}`),
  getAnswer: (id: string) => request<string>(`/api/helper/answer/${id}`),

  // ---- Change password ----
  changePassword: (b: { old_password: string; new_password: string; repeat_new_password: string }) =>
    request<{ email: string; message: string }>("/api/users/changePassword", { method: "PATCH", body: b }),
};
