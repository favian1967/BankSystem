import type { TransactionResponse } from "../api/types";
import { formatMoney, initials } from "../lib/format";
import { direction } from "../lib/tx";

const TYPE_LABEL: Record<string, string> = {
  TRANSFER: "Transfer",
  DEPOSIT: "Deposit",
  WITHDRAW: "Withdrawal",
  PAYMENT: "Payment",
};

function shortDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleDateString("en-GB", { day: "numeric", month: "short" });
}

export function TransactionRow({ tx, myAccountIds }: { tx: TransactionResponse; myAccountIds: Set<number> }) {
  const dir = direction(tx, myAccountIds);
  const isIn = dir === "in";
  const amount = typeof tx.amount === "string" ? Number(tx.amount) : tx.amount;
  const title = tx.description?.trim() || TYPE_LABEL[tx.transaction_type] || tx.transaction_type;
  const subtitle = `${TYPE_LABEL[tx.transaction_type] ?? tx.transaction_type}${tx.to_account_id ? ` · acct ·${String(tx.to_account_id).slice(-4)}` : ""}`;

  return (
    <div className="row">
      <div className="row-ic">{initials(title)}</div>
      <div className="row-main">
        <b>{title}</b>
        <span>{subtitle}</span>
      </div>
      <span className="row-cat">{TYPE_LABEL[tx.transaction_type] ?? tx.transaction_type}</span>
      <span className="row-date">{shortDate(tx.created_at)}</span>
      <div className={`row-amt ${isIn ? "in" : "out"}`}>
        {isIn ? "+" : "−"}
        {formatMoney(amount, tx.currency).replace(/^[-−]/, "")}
      </div>
    </div>
  );
}
