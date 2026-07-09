import type { ReactNode } from "react";
import type { AccountStatus, CardStatus, TransactionStatus } from "../api/types";

export function EmptyState({ icon, title, hint, action }: { icon: ReactNode; title: string; hint?: string; action?: ReactNode }) {
  return (
    <div className="empty">
      <div className="ic">{icon}</div>
      <div>
        <b style={{ fontFamily: "var(--font-display)", fontSize: "1.02rem", color: "var(--text)" }}>{title}</b>
        {hint && <div style={{ marginTop: 4, fontSize: "0.88rem" }}>{hint}</div>}
      </div>
      {action}
    </div>
  );
}

export function CardSkeleton({ height = 90 }: { height?: number }) {
  return <div className="skeleton" style={{ height }} />;
}

export function LoadingGrid({ count = 3, height = 90 }: { count?: number; height?: number }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      {Array.from({ length: count }).map((_, i) => (
        <CardSkeleton key={i} height={height} />
      ))}
    </div>
  );
}

const STATUS_CLASS: Record<string, string> = {
  ACTIVE: "green",
  COMPLETED: "green",
  BLOCKED: "red",
  FAILED: "red",
  CLOSED: "red",
  EXPIRED: "amber",
  PENDING: "amber",
};

export function StatusBadge({ status }: { status: AccountStatus | CardStatus | TransactionStatus }) {
  const cls = STATUS_CLASS[status] ?? "";
  return (
    <span className={`badge ${cls}`}>
      <span className="badge-dot" />
      {status.toLowerCase()}
    </span>
  );
}
