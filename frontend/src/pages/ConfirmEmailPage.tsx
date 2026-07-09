import { useState } from "react";
import { api, ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../ui/Toast";
import { IconLogout, IconShield } from "../ui/icons";

export function ConfirmEmailPage() {
  const { user, setConfirmed, logout } = useAuth();
  const toast = useToast();
  const [code, setCode] = useState("");
  const [sending, setSending] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [sent, setSent] = useState(false);

  const sendCode = async () => {
    setSending(true);
    try {
      await api.sendEmailKey();
      setSent(true);
      toast.success("Verification code sent to your email");
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Could not send the code");
    } finally {
      setSending(false);
    }
  };

  const verify = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code.trim()) return;
    setVerifying(true);
    try {
      const ok = await api.confirmEmail(code.trim());
      if (ok) {
        toast.success("Email verified — welcome aboard!");
        setConfirmed();
        // Full reload so every screen re-fetches with confirmed access.
        window.location.href = "/";
      } else {
        toast.error("Invalid or expired code. Try again.");
      }
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Verification failed");
    } finally {
      setVerifying(false);
    }
  };

  return (
    <div className="auth-panel" style={{ minHeight: "100vh" }}>
      <div className="auth-form glass fade-up" style={{ padding: 34, maxWidth: 420 }}>
        <div style={{ width: 54, height: 54, borderRadius: 16, display: "grid", placeItems: "center", background: "var(--accent-soft)", border: "1px solid rgba(198,248,62,0.25)", color: "var(--lime)", marginBottom: 18 }}>
          <IconShield width={26} height={26} />
        </div>
        <h2>Verify your email</h2>
        <p className="switch" style={{ marginBottom: 22 }}>
          For your security, confirm <b style={{ color: "var(--text)" }}>{user?.email}</b> before
          opening accounts or moving money.
        </p>

        <form className="form-grid" onSubmit={verify}>
          <button type="button" className="btn btn-ghost btn-block" onClick={sendCode} disabled={sending}>
            {sending ? <span className="spinner" /> : sent ? "Resend code" : "Send verification code"}
          </button>

          <label className="field">
            <span>Verification code</span>
            <input
              className="input mono"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="Enter the code from your email"
              autoFocus
            />
          </label>

          <button type="submit" className="btn btn-primary btn-block" disabled={verifying || !code.trim()}>
            {verifying ? <span className="spinner" /> : "Verify & continue"}
          </button>
        </form>

        <div className="demo-hint" style={{ marginTop: 18 }}>
          <b style={{ color: "var(--text-muted)" }}>Note:</b> код приходит на email (доставка через
          Brevo API). Если провайдер ещё не настроен, для демо можно подтвердить вручную в БД:
          <code style={{ display: "block", marginTop: 6, fontSize: "0.74rem", color: "var(--lime)" }}>
            UPDATE users SET is_confirmed=true WHERE email='…';
          </code>
        </div>

        <button className="btn btn-ghost btn-block" style={{ marginTop: 14 }} onClick={logout}>
          <IconLogout width={16} height={16} /> Log out
        </button>
      </div>
    </div>
  );
}
