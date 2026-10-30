import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import crypto from 'k6/crypto';

const base = (__ENV.BASE_URL || 'http://127.0.0.1:5098').replace(/\/$/, '');
const mode = __ENV.MODE || 'users';
if (!['users','throughput','login','writes'].includes(mode)) throw new Error('Unsupported MODE.');
const maximum = Number(__ENV.MAX_USERS || 100);
const rate = Number(__ENV.RATE || 1000);
const seconds = __ENV.DURATION || '5m';
if (!/^https?:\/\/[^/]+$/.test(base)) throw new Error('BASE_URL must contain only the explicitly authorized host.');
if (__ENV.ISOLATED_TEST_ENV !== 'yes') throw new Error('Confirm ISOLATED_TEST_ENV=yes for an isolated environment with synthetic data.');
if ((maximum > 100 || mode === 'throughput') && __ENV.ALLOW_HIGH_LOAD !== 'yes') throw new Error('High loads require ALLOW_HIGH_LOAD=yes and suitable infrastructure.');
if (!Number.isInteger(maximum) || maximum < 1 || maximum > 5000 || !Number.isInteger(rate) || rate < 1 || rate > 3000) throw new Error('Invalid load bounds.');
const fixture = JSON.parse(open(__ENV.FIXTURE_FILE || './fixtures.local.json'));
if (fixture.synthetic !== true || !fixture.accounts?.length || fixture.accounts.some(account => !['oportunidades','empresas','capacita'].includes(account.trail) || !account.token || account.token.startsWith('REPLACE_'))) throw new Error('Use a synthetic fixture with independently authenticated accounts.');
const serverErrors = new Rate('server_errors');
const users = [100, 500, 1000, 2500, 5000].filter(value => value <= maximum);
if (!users.includes(maximum)) users.push(maximum);
if (mode === 'writes' && fixture.accounts.length < maximum) throw new Error('Write tests require a separate synthetic account per VU.');
export const options = {
  discardResponseBodies: false,
  scenarios: mode === 'throughput'
    ? { throughput: { executor: 'constant-arrival-rate', rate, timeUnit: '1s', duration: seconds, preAllocatedVUs: Math.min(maximum, 100), maxVUs: maximum } }
    : { users: { executor: 'ramping-vus', startVUs: 0, stages: users.flatMap(target => [{ target, duration: '30s' }, { target, duration: seconds }]).concat([{ target: 0, duration: '30s' }]), gracefulRampDown: '30s' } },
  thresholds: { 'http_req_duration{operation:read}': ['p(95)<500'], server_errors: ['rate<0.01'], checks: ['rate>0.99'], ...(mode === 'throughput' ? { dropped_iterations: ['count==0'] } : {}) },
};
function nonce() { const bytes = new Uint8Array(crypto.randomBytes(16)); bytes[6]=(bytes[6]&15)|64;bytes[8]=(bytes[8]&63)|128;const hex=Array.from(bytes,b=>b.toString(16).padStart(2,'0')).join('');return `${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`; }
function send(method, path, token, body, operation, expected=200) {
  if (!path.startsWith('/api/') || /payments|webhooks|premium|password\/reset/.test(path)) throw new Error('Unsafe or chargeable load-test route.');
  const response = http.request(method,base+path,body == null ? null : JSON.stringify(body),{ timeout:'15s', headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{})},redirects:0,tags:{name:path.split('?')[0].replace(/[0-9a-f-]{36}|\/\d+/g,'/:id'),operation},responseType:'text' });
  serverErrors.add(response.status === 0 || response.status >= 500); check(response,{ 'expected response': r=>r.status===expected });return response;
}
export default function () {
  const account=fixture.accounts[(__VU-1)%fixture.accounts.length];
  if (mode==='login') {
    const paths={ oportunidades:'/api/auth/login',empresas:'/api/empresas/auth/login',capacita:'/api/capacita/auth/login' };
    send('POST',paths[account.trail],null,{email:account.email,password:account.password},'login');sleep(1);return;
  }
  if (mode==='writes') {
    if (__ITER > 0) { sleep(1); return; } // Preserve attempt limits: one deliberate synthetic workflow per account.
    if (__ENV.ALLOW_TEST_WRITES!=='yes' || !account.actions?.length) throw new Error('Synthetic action fixtures and ALLOW_TEST_WRITES=yes required.');
    const context={requestId:nonce()};
    for (const action of account.actions) {
      const expand=value=>JSON.stringify(value).replace(/\$\{([A-Za-z]+)\}/g,(_,key)=>context[key]||'');
      const response=send(action.method,JSON.parse(expand(action.path)),account.token,action.body ? JSON.parse(expand(action.body)) : null,'write',action.expected||200);
      if (action.captureId && response.status>=200 && response.status<300) context[action.captureId]=String(response.json('id'));
    }
    sleep(1);return;
  }
  if (!account.reads?.length) throw new Error('Each account needs representative read endpoints.');
  const path=account.reads[__ITER%account.reads.length];send('GET',path,account.token,null,'read');if(mode!=='throughput')sleep(1);
}
