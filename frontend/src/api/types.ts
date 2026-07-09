// ---- Enums (mirror backend) ----
export type Currency = "RUB" | "USD" | "EUR";
export type AccountType = "CHECKING" | "SAVED" | "DEPOSIT";
export type AccountStatus = "ACTIVE" | "BLOCKED" | "CLOSED";
export type CardType = "DEBIT" | "CREDIT";
export type CardStatus = "ACTIVE" | "BLOCKED" | "EXPIRED";
export type PaymentSystem = "VISA" | "MASTERCARD" | "MIR";
export type TransactionType = "TRANSFER" | "DEPOSIT" | "WITHDRAW" | "PAYMENT";
export type TransactionStatus = "PENDING" | "COMPLETED" | "FAILED";
export type UserRole = "USER" | "ADMIN";

// ---- Responses (snake_case as sent by backend) ----
export interface AccountResponse {
  id: number;
  account_number: string;
  account_type: AccountType;
  currency: Currency;
  balance: string | number;
  status: AccountStatus;
}

export interface CardResponse {
  id: number;
  card_number: string;
  card_holder_name: string;
  expiry_date: string; // ISO date
  card_type: CardType;
  payment_system: PaymentSystem;
  card_status: CardStatus;
  account_id: number;
  created_at: string;
}

export interface CardIssueResponse extends CardResponse {
  cvv: string;
}

export interface TransactionResponse {
  id: number;
  from_account_id: number | null;
  to_account_id: number | null;
  transaction_type: TransactionType;
  amount: string | number;
  currency: Currency;
  description: string | null;
  status: TransactionStatus;
  created_at: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// ---- Requests ----
export interface RegisterRequest {
  email: string;
  password: string;
  first_name: string;
  phone: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface CreateAccountRequest {
  account_type: AccountType;
  currency: Currency;
}

export interface CreateCardRequest {
  account_id: number;
  card_type: CardType;
  payment_system: PaymentSystem;
}

export interface DepositRequest {
  account_id: number;
  amount: number;
  description?: string;
}

export type WithdrawRequest = DepositRequest;

export interface TransferRequest {
  from_account_id: number;
  to_account_id: string; // NB: destination is the account NUMBER, not id
  amount: number;
  description?: string;
}
