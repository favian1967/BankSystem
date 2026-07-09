import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, tokenStore } from "../api/client";
import type { LoginRequest, RegisterRequest, UserRole } from "../api/types";
import { decodeJwt, isExpired } from "../lib/jwt";

interface AuthUser {
  email: string;
  role: UserRole;
  name: string;
}

interface AuthCtx {
  user: AuthUser | null;
  ready: boolean;
  needsConfirmation: boolean;
  setConfirmed: () => void;
  login: (b: LoginRequest) => Promise<void>;
  register: (b: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const Ctx = createContext<AuthCtx | null>(null);

function userFromToken(token: string | null): AuthUser | null {
  if (!token) return null;
  const claims = decodeJwt(token);
  if (!claims || isExpired(claims)) return null;
  const email = claims.sub ?? "user";
  return {
    email,
    role: (claims.role as UserRole) ?? "USER",
    name: email.split("@")[0] || "user",
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => userFromToken(tokenStore.get()));
  const [ready, setReady] = useState(false);
  const [needsConfirmation, setNeedsConfirmation] = useState(false);

  useEffect(() => {
    // Drop an expired token on boot.
    const t = tokenStore.get();
    const u = userFromToken(t);
    if (t && !u) tokenStore.clear();
    setUser(u);
    setReady(true);

    const onUnauthorized = () => setUser(null);
    const onUnconfirmed = () => setNeedsConfirmation(true);
    window.addEventListener("aurora:unauthorized", onUnauthorized);
    window.addEventListener("aurora:unconfirmed", onUnconfirmed);
    return () => {
      window.removeEventListener("aurora:unauthorized", onUnauthorized);
      window.removeEventListener("aurora:unconfirmed", onUnconfirmed);
    };
  }, []);

  const applyToken = useCallback((token: string) => {
    tokenStore.set(token);
    setNeedsConfirmation(false);
    setUser(userFromToken(token));
  }, []);

  const setConfirmed = useCallback(() => setNeedsConfirmation(false), []);

  const login = useCallback(
    async (b: LoginRequest) => {
      const token = await api.login(b);
      applyToken(token);
    },
    [applyToken]
  );

  const register = useCallback(
    async (b: RegisterRequest) => {
      const token = await api.register(b);
      applyToken(token);
    },
    [applyToken]
  );

  const logout = useCallback(() => {
    api.logout().catch(() => undefined);
    tokenStore.clear();
    setNeedsConfirmation(false);
    setUser(null);
  }, []);

  const value = useMemo<AuthCtx>(
    () => ({ user, ready, needsConfirmation, setConfirmed, login, register, logout }),
    [user, ready, needsConfirmation, setConfirmed, login, register, logout]
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useAuth(): AuthCtx {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
