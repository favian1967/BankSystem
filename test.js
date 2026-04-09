import http from 'k6/http';
import { check, fail } from 'k6';

export const options = {
    vus: 60,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    const payload = JSON.stringify({
        email: 'unityhub4747@gmail.com',
        password: '25442544',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
        },
    };

    const res = http.post('http://localhost:8080/api/auth/login', payload, params);

    check(res, {
        'login success': (r) => r.status === 200,
        'token exists': (r) => r.body && r.body.length > 0,
    });

    if (res.status !== 200 || !res.body) {
        fail(`Login failed. Status: ${res.status}, Body: ${res.body}`);
    }

    const token = res.body.trim().replace(/^"|"$/g, '');

    return { token };
}

export default function (data) {
    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'Accept': 'application/json',
        },
    };

    const res = http.get('http://localhost:8080/api/cards', params);

    if (res.status !== 200) {
        console.log('ERROR STATUS:', res.status);
        console.log('BODY:', res.body);
        return;
    }

    check(res, {
        'status 200': (r) => r.status === 200,
        'has cards array': (r) => {
            try {
                const body = r.json();
                return Array.isArray(body);
            } catch (e) {
                return false;
            }
        },
        'card has id': (r) => {
            try {
                const body = r.json();
                return Array.isArray(body) && body.length > 0 && body[0].id !== undefined;
            } catch (e) {
                return false;
            }
        },
    });
}