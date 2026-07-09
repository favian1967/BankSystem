import { useState, type ReactNode } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { initials } from "../lib/format";
import {
  IconCard,
  IconExchange,
  IconGrid,
  IconList,
  IconLogout,
  IconMenu,
  IconPlus,
  IconWallet,
} from "../ui/icons";
import { AssistantWidget } from "./AssistantWidget";

const NAV = [
  {
    section: "General",
    items: [
      { to: "/", label: "Overview", icon: IconGrid, exact: true },
      { to: "/accounts", label: "Accounts", icon: IconWallet },
      { to: "/cards", label: "Cards", icon: IconCard },
    ],
  },
  {
    section: "Money",
    items: [
      { to: "/transfer", label: "Transfers", icon: IconExchange },
      { to: "/transactions", label: "Transactions", icon: IconList },
    ],
  },
];

const TITLES: Record<string, { title: string }> = {
  "/": { title: "Overview" },
  "/accounts": { title: "Accounts" },
  "/cards": { title: "Cards" },
  "/transfer": { title: "Send money" },
  "/transactions": { title: "Transactions" },
};

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const meta = TITLES[location.pathname] ?? { title: "Aurora Bank" };
  const today = new Date().toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });

  return (
    <div className="app">
      {open && <div className="sidebar-backdrop" onClick={() => setOpen(false)} />}
      <aside className={`sidebar ${open ? "open" : ""}`}>
        <div className="brand">
          <div className="brand-logo">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#10140a" strokeWidth="2.6" strokeLinejoin="round" strokeLinecap="round">
              <path d="M5 18 L12 5 L19 18 Z" />
              <line x1="8.5" y1="18" x2="15.5" y2="18" />
            </svg>
          </div>
          <div className="brand-name">Aurora</div>
        </div>

        <nav className="nav" onClick={() => setOpen(false)}>
          {NAV.map((group) => (
            <div key={group.section}>
              <div className="nav-section">{group.section}</div>
              {group.items.map(({ to, label, icon: Icon, exact }) => (
                <NavLink key={to} to={to} end={exact} className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}>
                  <span className="nav-dot" />
                  <span className="nav-ic"><Icon width={18} height={18} /></span>
                  {label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="sidebar-foot">
          <div className="avatar">{initials(user?.name ?? "U")}</div>
          <div className="who">
            <b>{user?.name}</b>
            <span className="dim" style={{ fontSize: "0.74rem" }}>{user?.role === "ADMIN" ? "Administrator" : "Personal · Aurora"}</span>
          </div>
          <button className="icon-btn logout" title="Log out" onClick={logout}>
            <IconLogout width={17} height={17} />
          </button>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <button className="icon-btn menu-toggle" onClick={() => setOpen(true)}>
              <IconMenu width={20} height={20} />
            </button>
            <div>
              <h1>{meta.title}</h1>
              <div className="sub">{today}</div>
            </div>
          </div>
          <div className="topbar-actions">
            <div className="searchbox">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="11" cy="11" r="7" /><path d="M21 21l-4-4" /></svg>
              <input placeholder="Search…" />
            </div>
            <button className="icon-btn" title="Notifications">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.7 21a2 2 0 0 1-3.4 0" /></svg>
              <span className="notif-dot" />
            </button>
            <button className="btn btn-primary" onClick={() => navigate("/transfer")}>
              <IconPlus width={16} height={16} /> New transfer
            </button>
          </div>
        </header>

        <div className="content">{children}</div>
      </div>

      <AssistantWidget />
    </div>
  );
}
