import { useEffect, useRef, useState } from "react";
import { api } from "../api/client";
import { IconClose, IconSend, IconSparkles } from "../ui/icons";

interface Msg {
  from: "user" | "bot";
  text: string;
}

const GREETING: Msg = {
  from: "bot",
  text: "Hi! I'm Aurora Assistant. Ask me anything about your finances — spending, transfers or how the app works.",
};

const SUGGESTIONS = ["How do I open an account?", "What are my spending habits?", "How to send a transfer?"];

// Poll /answer/{id} until we get a non-empty reply or run out of attempts.
async function waitForAnswer(id: string, attempts = 12, delay = 1200): Promise<string | null> {
  for (let i = 0; i < attempts; i++) {
    try {
      const ans = await api.getAnswer(id);
      if (ans && ans.trim() && ans.toLowerCase() !== "null") return ans;
    } catch {
      /* keep polling */
    }
    await new Promise((r) => setTimeout(r, delay));
  }
  return null;
}

export function AssistantWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Msg[]>([GREETING]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const bodyRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bodyRef.current?.scrollTo({ top: bodyRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, busy]);

  const send = async (text: string) => {
    const q = text.trim();
    if (!q || busy) return;
    setMessages((m) => [...m, { from: "user", text: q }]);
    setInput("");
    setBusy(true);
    try {
      const id = await api.askAssistant(q);
      let reply: string | null = null;
      // The endpoint may return the answer directly, or an id to poll.
      if (id && id.length < 40 && !id.includes(" ")) {
        reply = await waitForAnswer(id);
      } else {
        reply = id;
      }
      setMessages((m) => [
        ...m,
        {
          from: "bot",
          text:
            reply ??
            "I couldn't reach the AI service right now. Make sure the assistant worker (Kafka) is running, then try again.",
        },
      ]);
    } catch {
      setMessages((m) => [
        ...m,
        { from: "bot", text: "The assistant is offline at the moment. Please try again later." },
      ]);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      {open && (
        <div className="assistant-panel glass">
          <div className="assistant-head">
            <span className="dot" />
            <div style={{ flex: 1 }}>
              <b style={{ fontFamily: "var(--font-display)" }}>Aurora Assistant</b>
              <div style={{ fontSize: "0.74rem", color: "var(--text-muted)" }}>AI-powered · online</div>
            </div>
            <button className="modal-close" onClick={() => setOpen(false)}>
              <IconClose width={17} height={17} />
            </button>
          </div>

          <div className="assistant-body" ref={bodyRef}>
            {messages.map((m, i) => (
              <div key={i} className={`msg ${m.from}`}>
                {m.text}
              </div>
            ))}
            {busy && (
              <div className="msg bot">
                <span className="typing">
                  <span /> <span /> <span />
                </span>
              </div>
            )}
            {messages.length === 1 && (
              <div className="assistant-suggest">
                {SUGGESTIONS.map((s) => (
                  <button key={s} className="chip-btn" onClick={() => send(s)}>
                    {s}
                  </button>
                ))}
              </div>
            )}
          </div>

          <form
            className="assistant-foot"
            onSubmit={(e) => {
              e.preventDefault();
              send(input);
            }}
          >
            <input
              className="input"
              placeholder="Ask something…"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={busy}
            />
            <button className="btn btn-primary" type="submit" disabled={busy || !input.trim()}>
              <IconSend width={17} height={17} />
            </button>
          </form>
        </div>
      )}

      <button className="assistant-fab" onClick={() => setOpen((o) => !o)} title="Aurora Assistant">
        {open ? <IconClose width={22} height={22} /> : <IconSparkles width={24} height={24} />}
      </button>
    </>
  );
}
