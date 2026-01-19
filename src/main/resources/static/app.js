const authSection = document.getElementById("authSection");
const dashboardSection = document.getElementById("dashboardSection");
const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");
const authMessage = document.getElementById("authMessage");
const logoutButton = document.getElementById("logoutButton");

const accountsList = document.getElementById("accountsList");
const cardsList = document.getElementById("cardsList");
const transactionsList = document.getElementById("transactionsList");
const accountsCount = document.getElementById("accountsCount");
const cardsCount = document.getElementById("cardsCount");
const totalBalances = document.getElementById("totalBalances");

const TOKEN_KEY = "banksystem_token";

const apiRequest = async (url, options = {}) => {
    const token = localStorage.getItem(TOKEN_KEY);
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {}),
    };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(url, { ...options, headers });
    const rawText = await response.text();

    if (!response.ok) {
        let message = rawText || "Request failed";
        try {
            const parsed = JSON.parse(rawText);
            message = parsed.message || parsed.error || message;
        } catch (error) {
            // keep raw text
        }
        throw new Error(message);
    }

    if (!rawText || response.status === 204) {
        return null;
    }

    try {
        return JSON.parse(rawText);
    } catch (error) {
        return rawText;
    }
};

const setMessage = (message, isError = false) => {
    authMessage.textContent = message;
    authMessage.style.color = isError ? "#d44747" : "#4a5578";
};

const updateVisibility = (isAuthenticated) => {
    authSection.classList.toggle("hidden", isAuthenticated);
    dashboardSection.classList.toggle("hidden", !isAuthenticated);
    logoutButton.classList.toggle("hidden", !isAuthenticated);
};

const renderAccounts = (accounts) => {
    accountsList.innerHTML = "";
    accountsCount.textContent = `${accounts.length} счетов`;

    if (accounts.length === 0) {
        accountsList.innerHTML = "<p class=\"hint\">У вас пока нет счетов.</p>";
        totalBalances.innerHTML = "";
        return;
    }

    const totals = accounts.reduce((acc, account) => {
        acc[account.currency] = (acc[account.currency] || 0) + Number(account.balance);
        return acc;
    }, {});

    accounts.forEach((account) => {
        const item = document.createElement("div");
        item.className = "item";
        item.innerHTML = `
      <p class="item-title">${account.accountType} • ${account.currency}</p>
      <p class="item-meta">Номер: ${account.accountNumber}</p>
      <p class="item-meta">Баланс: ${account.balance}</p>
      <p class="item-meta">Статус: ${account.status}</p>
    `;
        accountsList.appendChild(item);
    });

    totalBalances.innerHTML = "";
    Object.entries(totals).forEach(([currency, amount]) => {
        const pill = document.createElement("span");
        pill.className = "total-pill";
        pill.textContent = `${currency}: ${amount.toFixed(2)}`;
        totalBalances.appendChild(pill);
    });
};

const renderCards = (cards) => {
    cardsList.innerHTML = "";
    cardsCount.textContent = `${cards.length} карт`;

    if (cards.length === 0) {
        cardsList.innerHTML = "<p class=\"hint\">Нет выпущенных карт.</p>";
        return;
    }

    cards.forEach((card) => {
        const item = document.createElement("div");
        item.className = "item";
        item.innerHTML = `
      <p class="item-title">${card.cardType} • ${card.paymentSystem}</p>
      <p class="item-meta">Номер: ${card.cardNumber}</p>
      <p class="item-meta">Статус: ${card.cardStatus}</p>
      <p class="item-meta">Срок действия: ${card.expiryDate}</p>
    `;
        cardsList.appendChild(item);
    });
};

const renderTransactions = (transactions) => {
    transactionsList.innerHTML = "";
    if (transactions.length === 0) {
        transactionsList.innerHTML = "<p class=\"hint\">Транзакций пока нет.</p>";
        return;
    }

    transactions.forEach((transaction) => {
        const item = document.createElement("div");
        item.className = "item";
        item.innerHTML = `
      <p class="item-title">${transaction.transactionType} • ${transaction.amount} ${transaction.currency}</p>
      <p class="item-meta">Статус: ${transaction.status}</p>
      <p class="item-meta">Дата: ${transaction.createdAt}</p>
      <p class="item-meta">Описание: ${transaction.description || "—"}</p>
    `;
        transactionsList.appendChild(item);
    });
};

const loadDashboard = async () => {
    const accounts = await apiRequest("/api/accounts/getAll");
    renderAccounts(accounts);

    const cards = await apiRequest("/api/cards/getMyCards");
    renderCards(cards);

    if (accounts.length > 0) {
        const accountId = accounts[0].id;
        const transactions = await apiRequest(`/api/transactions/account/${accountId}/recent?limit=5`);
        renderTransactions(transactions);
    } else {
        renderTransactions([]);
    }
};

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(loginForm).entries());
    Object.keys(payload).forEach((key) => {
        if (typeof payload[key] === "string") {
            payload[key] = payload[key].trim();
        }
    });
    try {
        const token = await apiRequest("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(payload),
        });
        localStorage.setItem(TOKEN_KEY, String(token));
        setMessage("Успешный вход.");
        updateVisibility(true);
        await loadDashboard();
    } catch (error) {
        setMessage(`Ошибка входа: ${error.message}`, true);
    }
});

registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(registerForm).entries());
    Object.keys(payload).forEach((key) => {
        if (typeof payload[key] === "string") {
            payload[key] = payload[key].trim();
        }
    });
    try {
        const response = await apiRequest("/api/auth/register", {
            method: "POST",
            body: JSON.stringify(payload),
        });
        setMessage(response || "Регистрация успешна. Теперь войдите.", false);
    } catch (error) {
        setMessage(`Ошибка регистрации: ${error.message}`, true);
    }
});

logoutButton.addEventListener("click", () => {
    localStorage.removeItem(TOKEN_KEY);
    updateVisibility(false);
});

const bootstrap = async () => {
    const token = localStorage.getItem(TOKEN_KEY);
    updateVisibility(Boolean(token));

    if (token) {
        try {
            await loadDashboard();
        } catch (error) {
            setMessage("Сессия истекла. Войдите снова.", true);
            localStorage.removeItem(TOKEN_KEY);
            updateVisibility(false);
        }
    }
};

bootstrap();
