import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import type { AccountResponse, CardResponse, TransactionResponse } from "../api/types";

interface BankData {
  accounts: AccountResponse[];
  cards: CardResponse[];
  transactions: TransactionResponse[];
  loading: boolean;
  error: string | null;
  reload: () => void;
}

// Loads accounts + cards, then merges recent transactions across all accounts.
export function useBankData(): BankData {
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [accs, crds] = await Promise.all([api.getAccounts(), api.getCards()]);
      setAccounts(accs);
      setCards(crds);

      const txLists = await Promise.all(
        accs.map((a) =>
          api
            .getTransactions(a.id, 0, 50)
            .then((p) => p.content)
            .catch(() => [] as TransactionResponse[])
        )
      );
      // Deduplicate (a transfer shows up under both accounts).
      const seen = new Map<number, TransactionResponse>();
      for (const list of txLists) for (const tx of list) seen.set(tx.id, tx);
      const merged = Array.from(seen.values()).sort(
        (a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
      );
      setTransactions(merged);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load data");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return { accounts, cards, transactions, loading, error, reload: load };
}
