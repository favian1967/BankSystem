import { useCallback, useEffect, useMemo, useState } from "react";
import { api, ApiError } from "../api/client";
import type { AccountResponse, AccountType, Currency } from "../api/types";
import { formatMoney } from "../lib/format";
import { EmptyState, LoadingGrid, StatusBadge } from "../components/common";
import { Modal } from "../ui/Modal";
import { useToast } from "../ui/Toast";
import { IconArrowDown, IconArrowUp, IconLock, IconPlus, IconTrash, IconUnlock, IconWallet } from "../ui/icons";

const ACCOUNT_TYPES: AccountType[] = ["CHECKING", "SAVED", "DEPOSIT"];
const CURRENCIES: Currency[] = ["RUB", "USD", "EUR"];

type MoneyMode = "deposit" | "withdraw";

export function AccountsPage() {
  const toast = useToast();
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [money, setMoney] = useState<{ mode: MoneyMode; account: AccountResponse } | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setAccounts(await api.getAccounts());
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to load accounts");
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const totals = useMemo(() => {
    const m = new Map<Currency, number>();
    for (const a of accounts) {
      const bal = Number(a.balance) || 0;
      m.set(a.currency, (m.get(a.currency) ?? 0) + bal);
    }
    return m;
  }, [accounts]);

  const setStatus = async (a: AccountResponse, action: "block" | "unblock" | "close") => {
    try {
      if (action === "block") await api.blockAccount(a.id);
      else if (action === "unblock") await api.unblockAccount(a.id);
      else {
        if (!confirm("Close this account? This cannot be undone.")) return;
        await api.closeAccount(a.id);
      }
      toast.success(`Account ${action === "close" ? "closed" : action + "ed"}`);
      load();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Action failed");
    }
  };

  return (
    <>
      <div className="glass section fade-up">
        <div className="section-head">
          <div>
            <h2>All accounts</h2>
            <div className="sub">
              {Array.from(totals.entries()).map(([c, v]) => formatMoney(v, c)).join(" · ") || "No balances yet"}
            </div>
          </div>
          <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
            <IconPlus width={17} height={17} /> Open account
          </button>
        </div>

        {loading ? (
          <LoadingGrid count={3} height={82} />
        ) : accounts.length === 0 ? (
          <EmptyState
            icon={<IconWallet width={26} height={26} />}
            title="No accounts yet"
            hint="Open your first multi-currency account to start banking."
            action={<button className="btn btn-primary btn-sm" onClick={() => setShowCreate(true)}>Open account</button>}
          />
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
            {accounts.map((a) => {
              const active = a.status === "ACTIVE";
              return (
                <div className="acc" key={a.id} style={{ flexWrap: "wrap", rowGap: 12 }}>
                  <div className="acc-ring">{a.currency}</div>
                  <div className="acc-main">
                    <b style={{ display: "flex", alignItems: "center", gap: 10 }}>
                      {a.account_type} <StatusBadge status={a.status} />
                    </b>
                    <div className="num">{a.account_number}</div>
                  </div>
                  <div className="acc-bal" style={{ marginRight: 8 }}>
                    <b>{formatMoney(a.balance, a.currency)}</b>
                  </div>
                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                    <button className="btn btn-ghost btn-sm" disabled={!active} onClick={() => setMoney({ mode: "deposit", account: a })}>
                      <IconArrowDown width={15} height={15} /> Top up
                    </button>
                    <button className="btn btn-ghost btn-sm" disabled={!active} onClick={() => setMoney({ mode: "withdraw", account: a })}>
                      <IconArrowUp width={15} height={15} /> Withdraw
                    </button>
                    {active ? (
                      <button className="btn btn-ghost btn-sm" title="Freeze" onClick={() => setStatus(a, "block")}>
                        <IconLock width={15} height={15} />
                      </button>
                    ) : a.status === "BLOCKED" ? (
                      <button className="btn btn-ghost btn-sm" title="Unfreeze" onClick={() => setStatus(a, "unblock")}>
                        <IconUnlock width={15} height={15} />
                      </button>
                    ) : null}
                    {a.status !== "CLOSED" && (
                      <button className="btn btn-danger btn-sm" title="Close account" onClick={() => setStatus(a, "close")}>
                        <IconTrash width={15} height={15} />
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {showCreate && (
        <CreateAccountModal
          onClose={() => setShowCreate(false)}
          onDone={() => {
            setShowCreate(false);
            load();
          }}
        />
      )}

      {money && (
        <MoneyModal
          mode={money.mode}
          account={money.account}
          onClose={() => setMoney(null)}
          onDone={() => {
            setMoney(null);
            load();
          }}
        />
      )}
    </>
  );
}

function CreateAccountModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const toast = useToast();
  const [type, setType] = useState<AccountType>("CHECKING");
  const [currency, setCurrency] = useState<Currency>("RUB");
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setBusy(true);
    try {
      await api.createAccount({ account_type: type, currency });
      toast.success("Account opened");
      onDone();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Could not open account");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title="Open a new account"
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={submit} disabled={busy}>
            {busy ? <span className="spinner" /> : "Open account"}
          </button>
        </>
      }
    >
      <div className="form-grid">
        <label className="field">
          <span>Account type</span>
          <select className="select" value={type} onChange={(e) => setType(e.target.value as AccountType)}>
            {ACCOUNT_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label className="field">
          <span>Currency</span>
          <select className="select" value={currency} onChange={(e) => setCurrency(e.target.value as Currency)}>
            {CURRENCIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </label>
      </div>
    </Modal>
  );
}

function MoneyModal({
  mode,
  account,
  onClose,
  onDone,
}: {
  mode: MoneyMode;
  account: AccountResponse;
  onClose: () => void;
  onDone: () => void;
}) {
  const toast = useToast();
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);
  const isDeposit = mode === "deposit";

  const submit = async () => {
    const value = Number(amount);
    if (!value || value <= 0) {
      toast.error("Enter a valid amount");
      return;
    }
    setBusy(true);
    try {
      const body = { account_id: account.id, amount: value, description: description || undefined };
      if (isDeposit) await api.deposit(body);
      else await api.withdraw(body);
      toast.success(isDeposit ? "Funds added" : "Withdrawal complete");
      onDone();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Transaction failed");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title={isDeposit ? "Top up account" : "Withdraw funds"}
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className={`btn ${isDeposit ? "btn-primary" : "btn-danger"}`} onClick={submit} disabled={busy}>
            {busy ? <span className="spinner" /> : isDeposit ? "Add funds" : "Withdraw"}
          </button>
        </>
      }
    >
      <div style={{ marginBottom: 16, padding: "12px 14px", borderRadius: 12, background: "rgba(255,255,255,0.03)", border: "1px solid var(--stroke)" }}>
        <div className="dim" style={{ fontSize: "0.78rem" }}>{account.account_type} · {account.account_number.slice(-4)}</div>
        <div style={{ fontFamily: "var(--font-display)", fontSize: "1.2rem" }}>{formatMoney(account.balance, account.currency)}</div>
      </div>
      <div className="form-grid">
        <label className="field">
          <span>Amount ({account.currency})</span>
          <input className="input mono" type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" autoFocus />
        </label>
        <label className="field">
          <span>Description (optional)</span>
          <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="e.g. Salary, groceries…" />
        </label>
      </div>
    </Modal>
  );
}
