import http from 'k6/http';
import { check, sleep } from 'k6';

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
        password: '25442544'
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
    };

    const res = http.post('http://localhost:8080/api/auth/login', payload, params);

    check(res, {
        'login success': (r) => r.status === 200,
        'token exists': (r) => r.body && r.body.length > 0,
    });

    const token = res.body;

    return { token };
}

export default function (data) {
    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
        },
    };

    const res = http.get('http://localhost:8080/api/accounts/getAll', params);
    if (res.status !== 200) {
        console.log('ERROR STATUS:', res.status);
        console.log('BODY:', res.body);
    }
    check(res, {
        'status 200': (r) => r.status === 200,
        'has accounts': (r) => {
            const data = r.json();
            return Array.isArray(data);
        },
        'has id field': (r) => {
            const data = r.json();
            return data.length > 0 && data[0].id !== undefined;
        },
    });

    sleep(1);
}