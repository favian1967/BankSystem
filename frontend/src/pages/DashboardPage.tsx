import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useBankData } from "../lib/useBankData";
import { amountNum, buildDailySeries, direction } from "../lib/tx";
import { formatMoney, formatMoneyParts, initials } from "../lib/format";
import type { Currency } from "../api/types";
import { BankCard } from "../components/BankCard";
import { TransactionRow } from "../components/TransactionRow";
import { EmptyState, LoadingGrid } from "../components/common";
import { IconCard, IconList, IconPlus, IconTrendUp, IconWallet } from "../ui/icons";

const SUBTILE_LABELS = ["Operating", "Payroll", "Reserve"];

export function DashboardPage() {
  const { accounts, cards, transactions, loading, error } = useBankData();
  const navigate = useNavigate();
  const [qtAmount, setQtAmount] = useState("");

  const myAccountIds = useMemo(() => new Set(accounts.map((a) => a.id)), [accounts]);

  const totalsByCurrency = useMemo(() => {
    const m = new Map<Currency, number>();
    for (const a of accounts) {
      const bal = typeof a.balance === "string" ? Number(a.balance) : a.balance;
      m.set(a.currency, (m.get(a.currency) ?? 0) + (Number.isNaN(bal) ? 0 : bal));
    }
    return m;
  }, [accounts]);

  const primaryCurrency: Currency = accounts[0]?.currency ?? "USD";
  const primaryTotal = totalsByCurrency.get(primaryCurrency) ?? 0;
  const { intStr, dec } = formatMoneyParts(primaryTotal, primaryCurrency);

  const { income, expense } = useMemo(() => {
    let inc = 0;
    let exp = 0;
    const monthAgo = Date.now() - 30 * 24 * 3600 * 1000;
    for (const tx of transactions) {
      if (new Date(tx.created_at).getTime() < monthAgo) continue;
      if (direction(tx, myAccountIds) === "in") inc += amountNum(tx);
      else exp += amountNum(tx);
    }
    return { income: inc, expense: exp };
  }, [transactions, myAccountIds]);

  const lineData = useMemo(() => {
    const series = buildDailySeries(transactions, myAccountIds, 14);
    let running = 0;
    return series.map((d) => {
      running += d.income - d.expense;
      return { label: d.label, v: running };
    });
  }, [transactions, myAccountIds]);

  const barData = useMemo(() => buildDailySeries(transactions, myAccountIds, 7), [transactions, myAccountIds]);

  const featuredCard = cards.find((c) => c.card_status === "ACTIVE") ?? cards[0];
  const recentPeople = useMemo(() => {
    const seen = new Set<string>();
    const out: string[] = [];
    for (const tx of transactions) {
      const key = tx.description?.trim() || tx.transaction_type;
      const ini = initials(key);
      if (!seen.has(ini)) { seen.add(ini); out.push(ini); }
      if (out.length >= 3) break;
    }
    return out;
  }, [transactions]);

  if (loading) {
    return (
      <>
        <div className="grid-hero">
          <div className="skeleton" style={{ height: 320 }} />
          <div className="skeleton" style={{ height: 320 }} />
        </div>
        <div className="glass section"><LoadingGrid /></div>
      </>
    );
  }

  if (error) {
    return (
      <div className="glass section">
        <EmptyState icon={<IconWallet width={26} height={26} />} title="Couldn't load your data" hint={error} />
      </div>
    );
  }

  return (
    <>
      {/* Row 1: balance + featured card */}
      <div className="grid-hero">
        <div className="glass section balance-hero fade-up">
          <div className="section-head" style={{ marginBottom: 4 }}>
            <div className="balance-label">Total balance · all accounts</div>
            <span className="badge">{primaryCurrency}</span>
          </div>
          <div className="balance-big">
            {intStr}
            <span className="dec">.{dec}</span>
          </div>
          <div className="balance-delta">
            {income >= expense ? "+" : "−"}
            {formatMoney(Math.abs(income - expense), primaryCurrency).replace(/^[-−]/, "")} · net this month
          </div>

          <div className="balance-chart">
            {lineData.some((d) => d.v !== 0) ? (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={lineData} margin={{ top: 8, right: 4, left: 4, bottom: 0 }}>
                  <Tooltip
                    contentStyle={{ background: "#1b1e19", border: "1px solid rgba(255,255,255,0.14)", borderRadius: 10, fontSize: 12 }}
                    labelStyle={{ color: "#9ba199" }}
                    formatter={(v: number) => [formatMoney(v, primaryCurrency), "Net"]}
                  />
                  <Line type="monotone" dataKey="v" stroke="#c6f83e" strokeWidth={2.4} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ height: "100%", display: "grid", placeItems: "center", color: "var(--text-dim)", fontSize: "0.82rem" }}>
                No movement yet — top up an account to see the trend
              </div>
            )}
          </div>

          <div className="subtiles">
            {accounts.slice(0, 3).map((a, i) => (
              <div className="subtile" key={a.id}>
                <div className="t">{SUBTILE_LABELS[i]} · {a.currency}</div>
                <div className="v">{formatMoney(a.balance, a.currency)}</div>
              </div>
            ))}
            {accounts.length === 0 &&
              SUBTILE_LABELS.map((l) => (
                <div className="subtile" key={l}>
                  <div className="t">{l}</div>
                  <div className="v">—</div>
                </div>
              ))}
          </div>
        </div>

        <div className="glass section fade-up" style={{ display: "flex", flexDirection: "column", justifyContent: "space-between", gap: 16, animationDelay: "0.06s" }}>
          {featuredCard ? (
            <>
              <BankCard card={featuredCard} variant="v-lime" />
              <div style={{ display: "flex", gap: 10 }}>
                <button className="btn btn-ghost btn-block" onClick={() => navigate("/accounts")}>Add money</button>
                <button className="btn btn-primary btn-block" onClick={() => navigate("/transfer")}>Send</button>
              </div>
            </>
          ) : (
            <EmptyState
              icon={<IconCard width={24} height={24} />}
              title="No cards yet"
              hint="Issue a card linked to one of your accounts."
              action={<Link to="/cards" className="btn btn-primary btn-sm">Issue card</Link>}
            />
          )}
        </div>
      </div>

      {/* Row 2: cash flow + quick transfer */}
      <div className="grid-2">
        <div className="glass section fade-up">
          <div className="section-head">
            <h2>Cash flow</h2>
            <div className="legend" style={{ marginTop: 0 }}>
              <div className="legend-item"><span className="legend-dot" style={{ background: "#c6f83e" }} /> Inflow</div>
              <div className="legend-item"><span className="legend-dot" style={{ background: "rgba(255,255,255,0.18)" }} /> Outflow</div>
            </div>
          </div>
          {transactions.length === 0 ? (
            <EmptyState icon={<IconTrendUp width={24} height={24} />} title="No activity yet" hint="Deposits and transfers will chart here." />
          ) : (
            <div className="chart-box">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={barData} margin={{ top: 6, right: 6, left: -18, bottom: 0 }} barGap={-14}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                  <XAxis dataKey="label" tick={{ fill: "#63685f", fontSize: 11 }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fill: "#63685f", fontSize: 11 }} axisLine={false} tickLine={false} width={46} tickFormatter={(v) => (v >= 1000 ? `${v / 1000}k` : `${v}`)} />
                  <Tooltip
                    cursor={{ fill: "rgba(255,255,255,0.03)" }}
                    contentStyle={{ background: "#1b1e19", border: "1px solid rgba(255,255,255,0.14)", borderRadius: 10, fontSize: 12 }}
                    labelStyle={{ color: "#9ba199" }}
                    formatter={(v: number, n) => [formatMoney(v, primaryCurrency), n === "income" ? "Inflow" : "Outflow"]}
                  />
                  <Bar dataKey="expense" fill="rgba(255,255,255,0.14)" radius={[5, 5, 0, 0]} barSize={16} />
                  <Bar dataKey="income" fill="#c6f83e" radius={[5, 5, 0, 0]} barSize={16} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="glass section fade-up" style={{ animationDelay: "0.06s" }}>
          <div className="section-head"><h2>Quick transfer</h2></div>
          <div className="qt-avatars">
            <button className="qt-av add" title="New transfer" onClick={() => navigate("/transfer")}>
              <IconPlus width={18} height={18} />
            </button>
            {recentPeople.map((ini, i) => (
              <div className="qt-av" key={i}>{ini}</div>
            ))}
          </div>
          <div className="qt-amount">
            <div className="lbl">Amount</div>
            <input
              inputMode="decimal"
              value={qtAmount}
              onChange={(e) => setQtAmount(e.target.value)}
              placeholder={formatMoney(1500, primaryCurrency)}
            />
          </div>
          <button
            className="btn btn-primary btn-block"
            onClick={() => navigate("/transfer", { state: { amount: qtAmount } })}
          >
            Send money
          </button>
        </div>
      </div>

      {/* Row 3: recent transactions */}
      <div className="glass section fade-up">
        <div className="section-head">
          <h2>Recent transactions</h2>
          <Link to="/transactions" className="link-accent">View all →</Link>
        </div>
        {transactions.length === 0 ? (
          <EmptyState icon={<IconList width={24} height={24} />} title="No transactions" hint="Your latest activity will appear here." />
        ) : (
          <div className="rows">
            {transactions.slice(0, 6).map((tx) => (
              <TransactionRow key={tx.id} tx={tx} myAccountIds={myAccountIds} />
            ))}
          </div>
        )}
      </div>
    </>
  );
}
