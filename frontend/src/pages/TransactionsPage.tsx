import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { AccountResponse, TransactionResponse, TransactionType } from "../api/types";
import { formatDateTime, formatMoney } from "../lib/format";
import { direction } from "../lib/tx";
import { EmptyState, StatusBadge } from "../components/common";
import { useToast } from "../ui/Toast";
import { IconArrowDown, IconArrowUp, IconList, IconWallet } from "../ui/icons";

const TYPES: (TransactionType | "ALL")[] = ["ALL", "TRANSFER", "DEPOSIT", "WITHDRAW", "PAYMENT"];
const PAGE_SIZE = 12;

export function TransactionsPage() {
  const toast = useToast();
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [accountId, setAccountId] = useState<number>(0);
  const [txs, setTxs] = useState<TransactionResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState<TransactionType | "ALL">("ALL");
  const [loading, setLoading] = useState(true);

  const myAccountIds = useMemo(() => new Set(accounts.map((a) => a.id)), [accounts]);

  useEffect(() => {
    api
      .getAccounts()
      .then((accs) => {
        setAccounts(accs);
        if (accs[0]) setAccountId(accs[0].id);
        else setLoading(false);
      })
      .catch((e) => {
        toast.error(e instanceof ApiError ? e.message : "Failed to load accounts");
        setLoading(false);
      });
  }, [toast]);

  const loadPage = useCallback(async () => {
    if (!accountId) return;
    setLoading(true);
    try {
      const p = await api.getTransactions(accountId, page, PAGE_SIZE);
      setTxs(p.content);
      setTotalPages(p.totalPages);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to load transactions");
    } finally {
      setLoading(false);
    }
  }, [accountId, page, toast]);

  useEffect(() => {
    loadPage();
  }, [loadPage]);

  const visible = filter === "ALL" ? txs : txs.filter((t) => t.transaction_type === filter);

  if (!loading && accounts.length === 0) {
    return (
      <div className="glass section">
        <EmptyState
          icon={<IconWallet width={26} height={26} />}
          title="No accounts yet"
          hint="Open an account to start building your transaction history."
          action={<Link to="/accounts" className="btn btn-primary btn-sm">Open account</Link>}
        />
      </div>
    );
  }

  return (
    <div className="glass section fade-up">
      <div className="section-head" style={{ flexWrap: "wrap", rowGap: 12 }}>
        <h2>Transaction history</h2>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <select
            className="select"
            style={{ width: "auto" }}
            value={accountId}
            onChange={(e) => {
              setAccountId(Number(e.target.value));
              setPage(0);
            }}
          >
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>{a.account_type} · {a.currency} · ••{a.account_number.slice(-4)}</option>
            ))}
          </select>
          <select className="select" style={{ width: "auto" }} value={filter} onChange={(e) => setFilter(e.target.value as TransactionType | "ALL")}>
            {TYPES.map((t) => <option key={t} value={t}>{t === "ALL" ? "All types" : t}</option>)}
          </select>
        </div>
      </div>

      {loading ? (
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          {Array.from({ length: 6 }).map((_, i) => <div key={i} className="skeleton" style={{ height: 52 }} />)}
        </div>
      ) : visible.length === 0 ? (
        <EmptyState icon={<IconList width={24} height={24} />} title="No transactions" hint="Transactions for this account will show up here." />
      ) : (
        <>
          <div className="table-wrap">
            <table className="tbl">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Description</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th style={{ textAlign: "right" }}>Amount</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((tx) => {
                  const dir = direction(tx, myAccountIds);
                  const isIn = dir === "in";
                  return (
                    <tr key={tx.id}>
                      <td>
                        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                          <span className={`row-ic ${dir}`} style={{ width: 32, height: 32 }}>
                            {isIn ? <IconArrowDown width={15} height={15} /> : <IconArrowUp width={15} height={15} />}
                          </span>
                          <b style={{ fontWeight: 600, textTransform: "capitalize" }}>{tx.transaction_type.toLowerCase()}</b>
                        </div>
                      </td>
                      <td className="muted">{tx.description?.trim() || "—"}</td>
                      <td className="muted mono">{formatDateTime(tx.created_at)}</td>
                      <td><StatusBadge status={tx.status} /></td>
                      <td style={{ textAlign: "right" }} className={`mono ${isIn ? "" : ""}`}>
                        <b style={{ color: isIn ? "var(--lime)" : "var(--text)" }}>
                          {isIn ? "+" : "−"}{formatMoney(tx.amount, tx.currency).replace(/^[-−]/, "")}
                        </b>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginTop: 18 }}>
              <span className="dim" style={{ fontSize: "0.85rem" }}>Page {page + 1} of {totalPages}</span>
              <div style={{ display: "flex", gap: 8 }}>
                <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</button>
                <button className="btn btn-ghost btn-sm" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
