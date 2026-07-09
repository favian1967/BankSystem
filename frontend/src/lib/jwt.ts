import type { UserRole } from "../api/types";

export interface JwtClaims {
  sub: string; // email
  role?: UserRole | string;
  exp?: number;
  iat?: number;
}

// Decode a JWT payload without verifying the signature (client-side display only).
export function decodeJwt(token: string): JwtClaims | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(json) as JwtClaims;
  } catch {
    return null;
  }
}

export function isExpired(claims: JwtClaims | null): boolean {
  if (!claims?.exp) return false;
  return Date.now() >= claims.exp * 1000;
}
