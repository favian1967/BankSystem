import type { CardResponse, PaymentSystem } from "../api/types";
import { formatExpiry, groupCard, maskCard } from "../lib/format";

const VARIANTS = ["v-lime", "v-graphite", "v-olive"] as const;

export function cardVariant(seed: number): string {
  return VARIANTS[seed % VARIANTS.length];
}

function SystemMark({ system, dark }: { system: PaymentSystem; dark: boolean }) {
  const color = dark ? "#10140a" : "#f2f4ef";
  if (system === "VISA") return <span className="bankcard-sys" style={{ color }}>VISA</span>;
  if (system === "MASTERCARD")
    return (
      <span style={{ display: "inline-flex", alignItems: "center" }}>
        <span style={{ width: 22, height: 22, borderRadius: "50%", background: "#eb001b", display: "inline-block" }} />
        <span style={{ width: 22, height: 22, borderRadius: "50%", background: "#f79e1b", marginLeft: -9, opacity: 0.9, display: "inline-block" }} />
      </span>
    );
  return <span className="bankcard-sys" style={{ fontStyle: "normal", letterSpacing: "0.05em", color }}>MIR</span>;
}

interface Props {
  card: CardResponse;
  reveal?: boolean;
  variant?: string;
  variantSeed?: number;
}

export function BankCard({ card, reveal = false, variant, variantSeed }: Props) {
  const v = variant ?? cardVariant(variantSeed ?? card.id);
  const dark = v === "v-lime";
  const blocked = card.card_status !== "ACTIVE";

  return (
    <div className={`bankcard ${v} ${blocked ? "blocked" : ""}`}>
      <div className="bankcard-top">
        <div>
          <div className="bankcard-brand">Aurora {card.card_type === "CREDIT" ? "Credit" : "Business"}</div>
          <div style={{ fontSize: "0.62rem", opacity: 0.7, letterSpacing: "0.12em", textTransform: "uppercase", marginTop: 2 }}>
            {card.card_type}
          </div>
        </div>
        <div className="bankcard-chip" />
      </div>

      <div className="bankcard-number">{reveal ? groupCard(card.card_number) : maskCard(card.card_number)}</div>

      <div className="bankcard-bottom">
        <div style={{ display: "flex", gap: 26 }}>
          <div>
            <div className="lbl">Card holder</div>
            <div className="val">{card.card_holder_name || "—"}</div>
          </div>
          <div>
            <div className="lbl">Exp</div>
            <div className="val">{formatExpiry(card.expiry_date)}</div>
          </div>
        </div>
        <SystemMark system={card.payment_system} dark={dark} />
      </div>
    </div>
  );
}
