import type { TransactionResponse } from "../api/types";

export type Direction = "in" | "out";

// Classify a transaction relative to the set of account ids the user owns.
export function direction(tx: TransactionResponse, myAccountIds: Set<number>): Direction {
  switch (tx.transaction_type) {
    case "DEPOSIT":
      return "in";
    case "WITHDRAW":
    case "PAYMENT":
      return "out";
    case "TRANSFER":
      // Outgoing if the source account is mine; otherwise it's money coming in.
      return tx.from_account_id != null && myAccountIds.has(tx.from_account_id) ? "out" : "in";
    default:
      return "out";
  }
}

export function amountNum(tx: TransactionResponse): number {
  const n = typeof tx.amount === "string" ? Number(tx.amount) : tx.amount;
  return Number.isNaN(n) ? 0 : n;
}

export interface DaySeries {
  label: string;
  income: number;
  expense: number;
}

// Build a per-day income/expense series for the last `days` days.
export function buildDailySeries(
  txs: TransactionResponse[],
  myAccountIds: Set<number>,
  days = 14
): DaySeries[] {
  const buckets = new Map<string, DaySeries>();
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    buckets.set(key, {
      label: d.toLocaleDateString("en-GB", { day: "2-digit", month: "short" }),
      income: 0,
      expense: 0,
    });
  }

  for (const tx of txs) {
    const key = (tx.created_at || "").slice(0, 10);
    const bucket = buckets.get(key);
    if (!bucket) continue;
    const amt = amountNum(tx);
    if (direction(tx, myAccountIds) === "in") bucket.income += amt;
    else bucket.expense += amt;
  }

  return Array.from(buckets.values());
}

export interface TypeBreakdown {
  name: string;
  value: number;
  color: string;
}

const TYPE_META: Record<string, { name: string; color: string }> = {
  TRANSFER: { name: "Transfers", color: "#c6f83e" },
  DEPOSIT: { name: "Deposits", color: "#8bd450" },
  WITHDRAW: { name: "Withdrawals", color: "#f2727f" },
  PAYMENT: { name: "Payments", color: "#f5c451" },
};

export function buildTypeBreakdown(txs: TransactionResponse[]): TypeBreakdown[] {
  const totals = new Map<string, number>();
  for (const tx of txs) {
    totals.set(tx.transaction_type, (totals.get(tx.transaction_type) ?? 0) + amountNum(tx));
  }
  return Array.from(totals.entries())
    .map(([type, value]) => ({
      name: TYPE_META[type]?.name ?? type,
      value,
      color: TYPE_META[type]?.color ?? "#6366f1",
    }))
    .filter((x) => x.value > 0)
    .sort((a, b) => b.value - a.value);
}
