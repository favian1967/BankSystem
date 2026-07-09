import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import { IconShield, IconSparkles, IconTrendUp } from "../ui/icons";

type Mode = "login" | "register";

export function LoginPage() {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<Mode>("login");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({ email: "", password: "", first_name: "", phone: "+7" });

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (mode === "login") {
        await login({ email: form.email, password: form.password });
      } else {
        await register({
          email: form.email,
          password: form.password,
          first_name: form.first_name,
          phone: form.phone,
        });
      }
      navigate("/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      {/* Hero */}
      <div className="auth-hero">
        <div>
          <div className="badge-row">
            <span className="badge violet"><IconShield width={13} height={13} /> Bank-grade security</span>
            <span className="badge"><span className="badge-dot" style={{ background: "var(--lime)" }} /> JWT · Encrypted</span>
          </div>
          <h1>
            Banking that feels<br />
            <span className="gradient-text">effortless.</span>
          </h1>
          <p className="lead">
            Open multi-currency accounts, issue virtual cards, move money instantly and let AI keep an eye on your spending — all in one place.
          </p>
        </div>

        <div className="hero-cards">
          <div className="bankcard v-lime">
            <div className="bankcard-top">
              <div className="bankcard-brand">Aurora</div>
              <div className="bankcard-chip" />
            </div>
            <div className="bankcard-number">•••• •••• •••• 4921</div>
            <div className="bankcard-bottom">
              <div><div className="lbl">Card holder</div><div className="val">A. Ivanov</div></div>
              <span className="bankcard-sys">VISA</span>
            </div>
          </div>
          <div className="bankcard v-graphite">
            <div className="bankcard-top">
              <div className="bankcard-brand">Aurora</div>
              <div className="bankcard-chip" />
            </div>
            <div className="bankcard-number">•••• •••• •••• 7830</div>
            <div className="bankcard-bottom">
              <div><div className="lbl">Card holder</div><div className="val">A. Ivanov</div></div>
              <span className="bankcard-sys" style={{ fontStyle: "normal" }}>MIR</span>
            </div>
          </div>
        </div>

        <div className="hero-feats">
          <div className="hero-feat"><span className="fi"><IconTrendUp width={17} height={17} /></span> Real-time balances and spending analytics</div>
          <div className="hero-feat"><span className="fi"><IconSparkles width={17} height={17} /></span> Built-in AI financial assistant</div>
        </div>
      </div>

      {/* Form */}
      <div className="auth-panel">
        <div className="auth-form fade-up">
          <div className="tabs">
            <div className={`tab ${mode === "login" ? "active" : ""}`} onClick={() => { setMode("login"); setError(null); }}>Sign in</div>
            <div className={`tab ${mode === "register" ? "active" : ""}`} onClick={() => { setMode("register"); setError(null); }}>Create account</div>
          </div>

          <h2>{mode === "login" ? "Welcome back" : "Get started"}</h2>
          <p className="switch">
            {mode === "login" ? "Enter your credentials to continue." : "It only takes a minute to open your account."}
          </p>

          <form className="form-grid" onSubmit={submit}>
            {mode === "register" && (
              <label className="field">
                <span>First name</span>
                <input className="input" value={form.first_name} onChange={set("first_name")} placeholder="Alex" required />
              </label>
            )}
            <label className="field">
              <span>Email</span>
              <input className="input" type="email" value={form.email} onChange={set("email")} placeholder="you@example.com" required />
            </label>
            {mode === "register" && (
              <label className="field">
                <span>Phone</span>
                <input className="input" value={form.phone} onChange={set("phone")} placeholder="+7XXXXXXXXXX" pattern="\+7\d{10}" required />
              </label>
            )}
            <label className="field">
              <span>Password</span>
              <input className="input" type="password" value={form.password} onChange={set("password")} placeholder="At least 8 characters" minLength={8} required />
            </label>

            {error && <div className="form-error">{error}</div>}

            <button className="btn btn-primary btn-block" type="submit" disabled={busy} style={{ marginTop: 6 }}>
              {busy ? <span className="spinner" /> : mode === "login" ? "Sign in" : "Create account"}
            </button>
          </form>

          <div className="demo-hint">
            <b style={{ color: "var(--text-muted)" }}>Demo tip:</b> create an account, then open one under <b>Accounts</b> and top it up with a deposit to see the dashboard come alive.
          </div>
        </div>
      </div>
    </div>
  );
}
