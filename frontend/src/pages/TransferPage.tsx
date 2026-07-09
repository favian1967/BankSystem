import { useCallback, useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { AccountResponse } from "../api/types";
import { formatMoney } from "../lib/format";
import { EmptyState } from "../components/common";
import { useToast } from "../ui/Toast";
import { IconExchange, IconSend, IconWallet } from "../ui/icons";
import { Link } from "react-router-dom";

export function TransferPage() {
  const toast = useToast();
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const location = useLocation();
  const presetAmount = (location.state as { amount?: string } | null)?.amount ?? "";
  const [fromId, setFromId] = useState<number>(0);
  const [toNumber, setToNumber] = useState("");
  const [amount, setAmount] = useState(presetAmount.replace(/[^\d.]/g, ""));
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const accs = await api.getAccounts();
      setAccounts(accs);
      const firstActive = accs.find((a) => a.status === "ACTIVE") ?? accs[0];
      if (firstActive) setFromId(firstActive.id);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to load accounts");
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const from = useMemo(() => accounts.find((a) => a.id === fromId), [accounts, fromId]);
  const value = Number(amount) || 0;
  const insufficient = from ? value > Number(from.balance) : false;
  const canSubmit = from && toNumber.trim().length >= 4 && value > 0 && !insufficient && !busy;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit || !from) return;
    setBusy(true);
    try {
      await api.transfer({
        from_account_id: from.id,
        to_account_id: toNumber.trim(),
        amount: value,
        description: description || undefined,
      });
      toast.success(`Sent ${formatMoney(value, from.currency)}`);
      setToNumber("");
      setAmount("");
      setDescription("");
      load();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Transfer failed");
    } finally {
      setBusy(false);
    }
  };

  if (!loading && accounts.length === 0) {
    return (
      <div className="glass section">
        <EmptyState
          icon={<IconWallet width={26} height={26} />}
          title="You need an account first"
          hint="Open an account before sending money."
          action={<Link to="/accounts" className="btn btn-primary btn-sm">Open account</Link>}
        />
      </div>
    );
  }

  return (
    <div className="transfer-grid">
      <form className="glass section fade-up" onSubmit={submit}>
        <div className="section-head"><h2>Send money</h2></div>
        <div className="form-grid">
          <label className="field">
            <span>From account</span>
            <select className="select" value={fromId} onChange={(e) => setFromId(Number(e.target.value))} disabled={loading}>
              {accounts.map((a) => (
                <option key={a.id} value={a.id} disabled={a.status !== "ACTIVE"}>
                  {a.account_type} · {a.currency} · {formatMoney(a.balance, a.currency)}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Recipient account number</span>
            <input className="input mono" value={toNumber} onChange={(e) => setToNumber(e.target.value)} placeholder="e.g. 40817810099910004321" />
          </label>

          <label className="field">
            <span>Amount {from ? `(${from.currency})` : ""}</span>
            <input className="input mono" type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" />
            {insufficient && <span style={{ color: "var(--red)", fontSize: "0.8rem" }}>Insufficient funds</span>}
          </label>

          <label className="field">
            <span>Description (optional)</span>
            <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="What's it for?" />
          </label>

          <button className="btn btn-primary btn-block" type="submit" disabled={!canSubmit} style={{ marginTop: 6 }}>
            {busy ? <span className="spinner" /> : <><IconSend width={17} height={17} /> Send transfer</>}
          </button>
        </div>
      </form>

      {/* Live preview */}
      <div className="glass section fade-up" style={{ animationDelay: "0.08s" }}>
        <div className="section-head"><h2>Preview</h2></div>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 18, padding: "20px 0" }}>
          <div style={{ fontFamily: "var(--font-display)", fontSize: "2.6rem", fontWeight: 700 }} className="gradient-text">
            {from ? formatMoney(value, from.currency) : "—"}
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 16, width: "100%", justifyContent: "center" }}>
            <div style={{ textAlign: "center" }}>
              <div className="acc-ring" style={{ margin: "0 auto 8px" }}>{from?.currency ?? "—"}</div>
              <div style={{ fontSize: "0.82rem" }}>{from ? from.account_type : "From"}</div>
              <div className="dim" style={{ fontSize: "0.74rem" }}>••{from?.account_number.slice(-4) ?? ""}</div>
            </div>
            <div style={{ color: "var(--lime)" }}><IconExchange width={26} height={26} /></div>
            <div style={{ textAlign: "center" }}>
              <div className="acc-ring" style={{ margin: "0 auto 8px", background: "rgba(255,255,255,0.05)", borderColor: "var(--stroke)", color: "var(--text-muted)" }}>
                <IconWallet width={20} height={20} />
              </div>
              <div style={{ fontSize: "0.82rem" }}>Recipient</div>
              <div className="dim mono" style={{ fontSize: "0.74rem" }}>{toNumber ? `••${toNumber.slice(-4)}` : "—"}</div>
            </div>
          </div>

          <div className="dim" style={{ fontSize: "0.82rem", textAlign: "center", maxWidth: 320 }}>
            Transfers are processed instantly and protected with an idempotency key, so a double-click never sends twice.
          </div>
        </div>
      </div>
    </div>
  );
}
