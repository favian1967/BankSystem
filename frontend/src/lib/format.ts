import type { Currency } from "../api/types";

const CURRENCY_LOCALE: Record<Currency, string> = {
  RUB: "ru-RU",
  USD: "en-US",
  EUR: "de-DE",
};

export function formatMoney(value: string | number, currency: Currency = "RUB"): string {
  const n = typeof value === "string" ? Number(value) : value;
  if (Number.isNaN(n)) return "—";
  return new Intl.NumberFormat(CURRENCY_LOCALE[currency] ?? "en-US", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(n);
}

// Splits a money value into a bright integer part (with currency symbol) and dimmed decimals.
export function formatMoneyParts(value: string | number, currency: Currency = "RUB"): { intStr: string; dec: string } {
  const n = typeof value === "string" ? Number(value) : value;
  const safe = Number.isNaN(n) ? 0 : n;
  const intPart = Math.trunc(safe);
  const dec = Math.abs(Math.round((safe - intPart) * 100)).toString().padStart(2, "0");
  const intStr = new Intl.NumberFormat(CURRENCY_LOCALE[currency] ?? "en-US", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(intPart);
  return { intStr, dec };
}

export function formatNumber(value: string | number): string {
  const n = typeof value === "string" ? Number(value) : value;
  return new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 }).format(n);
}

export function maskCard(num: string): string {
  const clean = (num || "").replace(/\s+/g, "");
  if (clean.length < 4) return num;
  return `•••• •••• •••• ${clean.slice(-4)}`;
}

export function groupCard(num: string): string {
  return (num || "").replace(/\s+/g, "").replace(/(.{4})/g, "$1 ").trim();
}

export function maskAccount(num: string): string {
  if (!num) return "—";
  return num.length > 8 ? `${num.slice(0, 4)} •••• ${num.slice(-4)}` : num;
}

export function formatDate(iso: string): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
}

export function formatDateTime(iso: string): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("en-GB", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatExpiry(iso: string): string {
  if (!iso) return "••/••";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "••/••";
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const yy = String(d.getFullYear()).slice(-2);
  return `${mm}/${yy}`;
}

export function initials(name: string): string {
  if (!name) return "U";
  const parts = name.trim().split(/\s+/);
  return (parts[0]?.[0] ?? "") + (parts[1]?.[0] ?? "");
}
