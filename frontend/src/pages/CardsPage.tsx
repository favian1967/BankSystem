import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../api/client";
import type { AccountResponse, CardIssueResponse, CardResponse, CardType, PaymentSystem } from "../api/types";
import { BankCard } from "../components/BankCard";
import { EmptyState, StatusBadge } from "../components/common";
import { Modal } from "../ui/Modal";
import { useToast } from "../ui/Toast";
import { formatExpiry, groupCard } from "../lib/format";
import { IconCard, IconCheck, IconEye, IconLock, IconPlus, IconTrash, IconUnlock } from "../ui/icons";

const CARD_TYPES: CardType[] = ["DEBIT", "CREDIT"];
const SYSTEMS: PaymentSystem[] = ["VISA", "MASTERCARD", "MIR"];

export function CardsPage() {
  const toast = useToast();
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showIssue, setShowIssue] = useState(false);
  const [issued, setIssued] = useState<CardIssueResponse | null>(null);
  const [revealed, setRevealed] = useState<Set<number>>(new Set());

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [crds, accs] = await Promise.all([api.getCards(), api.getAccounts()]);
      setCards(crds);
      setAccounts(accs);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to load cards");
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const toggleReveal = (id: number) =>
    setRevealed((s) => {
      const next = new Set(s);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });

  const action = async (card: CardResponse, act: "block" | "unblock" | "delete") => {
    try {
      if (act === "block") await api.blockCard(card.id);
      else if (act === "unblock") await api.unblockCard(card.id);
      else {
        if (!confirm("Delete this card permanently?")) return;
        await api.deleteCard(card.id);
      }
      toast.success(`Card ${act === "delete" ? "deleted" : act + "ed"}`);
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
            <h2>Your cards</h2>
            <div className="sub">{cards.length} card{cards.length === 1 ? "" : "s"} issued</div>
          </div>
          <button className="btn btn-primary" onClick={() => setShowIssue(true)} disabled={accounts.length === 0}>
            <IconPlus width={17} height={17} /> Issue card
          </button>
        </div>

        {loading ? (
          <div className="grid-cards">
            {Array.from({ length: 3 }).map((_, i) => <div key={i} className="skeleton" style={{ aspectRatio: "1.586", borderRadius: 20 }} />)}
          </div>
        ) : cards.length === 0 ? (
          <EmptyState
            icon={<IconCard width={26} height={26} />}
            title="No cards yet"
            hint={accounts.length === 0 ? "Open an account first, then issue a card." : "Issue a virtual card linked to one of your accounts."}
            action={accounts.length > 0 ? <button className="btn btn-primary btn-sm" onClick={() => setShowIssue(true)}>Issue card</button> : undefined}
          />
        ) : (
          <div className="grid-cards">
            {cards.map((c) => {
              const active = c.card_status === "ACTIVE";
              return (
                <div key={c.id} style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                  <BankCard card={c} reveal={revealed.has(c.id)} />
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, flexWrap: "wrap" }}>
                    <StatusBadge status={c.card_status} />
                    <div style={{ display: "flex", gap: 6 }}>
                      <button className="btn btn-ghost btn-sm" title="Reveal number" onClick={() => toggleReveal(c.id)}>
                        <IconEye width={15} height={15} />
                      </button>
                      {active ? (
                        <button className="btn btn-ghost btn-sm" title="Freeze" onClick={() => action(c, "block")}>
                          <IconLock width={15} height={15} />
                        </button>
                      ) : c.card_status === "BLOCKED" ? (
                        <button className="btn btn-ghost btn-sm" title="Unfreeze" onClick={() => action(c, "unblock")}>
                          <IconUnlock width={15} height={15} />
                        </button>
                      ) : null}
                      <button className="btn btn-danger btn-sm" title="Delete" onClick={() => action(c, "delete")}>
                        <IconTrash width={15} height={15} />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {showIssue && (
        <IssueCardModal
          accounts={accounts}
          onClose={() => setShowIssue(false)}
          onDone={(card) => {
            setShowIssue(false);
            setIssued(card);
            load();
          }}
        />
      )}

      {issued && <IssuedCardModal card={issued} onClose={() => setIssued(null)} />}
    </>
  );
}

function IssueCardModal({
  accounts,
  onClose,
  onDone,
}: {
  accounts: AccountResponse[];
  onClose: () => void;
  onDone: (c: CardIssueResponse) => void;
}) {
  const toast = useToast();
  const [accountId, setAccountId] = useState<number>(accounts[0]?.id ?? 0);
  const [cardType, setCardType] = useState<CardType>("DEBIT");
  const [system, setSystem] = useState<PaymentSystem>("VISA");
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setBusy(true);
    try {
      const card = await api.createCard({ account_id: accountId, card_type: cardType, payment_system: system });
      toast.success("Card issued");
      onDone(card);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Could not issue card");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title="Issue a new card"
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={submit} disabled={busy}>
            {busy ? <span className="spinner" /> : "Issue card"}
          </button>
        </>
      }
    >
      <div className="form-grid">
        <label className="field">
          <span>Linked account</span>
          <select className="select" value={accountId} onChange={(e) => setAccountId(Number(e.target.value))}>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>{a.account_type} · {a.currency} · {a.account_number.slice(-4)}</option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Card type</span>
          <select className="select" value={cardType} onChange={(e) => setCardType(e.target.value as CardType)}>
            {CARD_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label className="field">
          <span>Payment system</span>
          <select className="select" value={system} onChange={(e) => setSystem(e.target.value as PaymentSystem)}>
            {SYSTEMS.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
      </div>
    </Modal>
  );
}

function IssuedCardModal({ card, onClose }: { card: CardIssueResponse; onClose: () => void }) {
  return (
    <Modal
      title="Card issued 🎉"
      onClose={onClose}
      footer={<button className="btn btn-primary" onClick={onClose}><IconCheck width={16} height={16} /> Done</button>}
    >
      <BankCard card={card} reveal />
      <div style={{ marginTop: 16, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
        <Field label="Card number" value={groupCard(card.card_number)} />
        <Field label="CVV" value={card.cvv} />
        <Field label="Expires" value={formatExpiry(card.expiry_date)} />
        <Field label="Holder" value={card.card_holder_name} />
      </div>
      <p className="dim" style={{ fontSize: "0.8rem", marginTop: 14 }}>
        Store the CVV securely — it won't be shown again.
      </p>
    </Modal>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ padding: "10px 12px", borderRadius: 10, background: "rgba(255,255,255,0.03)", border: "1px solid var(--stroke)" }}>
      <div className="dim" style={{ fontSize: "0.72rem", textTransform: "uppercase", letterSpacing: "0.05em" }}>{label}</div>
      <div className="mono" style={{ fontFamily: "var(--font-display)", fontSize: "0.98rem", marginTop: 2 }}>{value}</div>
    </div>
  );
}
