/**
 * fetch 封装：统一前缀、统一 JSON、超时与错误处理。
 *
 * 所有请求都走相对路径 /api/*，由 vite dev server 或生产网关代理到 gaia-gateway（8090）。
 */
const BASE = '/api';
const DEFAULT_TIMEOUT_MS = 8000;

async function request(method, path, { body, headers, timeout = DEFAULT_TIMEOUT_MS } = {}) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeout);
  try {
    const res = await fetch(BASE + path, {
      method,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        ...(headers || {}),
      },
      body: body == null ? undefined : JSON.stringify(body),
      signal: ctrl.signal,
      credentials: 'include',
    });
    const text = await res.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch (_) {
      data = text;
    }
    if (!res.ok) {
      const err = new Error(`HTTP ${res.status} ${res.statusText}`);
      err.status = res.status;
      err.data = data;
      throw err;
    }
    return data;
  } finally {
    clearTimeout(timer);
  }
}

export const http = {
  get: (path, opts) => request('GET', path, opts),
  post: (path, body, opts) => request('POST', path, { ...(opts || {}), body }),
  put: (path, body, opts) => request('PUT', path, { ...(opts || {}), body }),
  del: (path, opts) => request('DELETE', path, opts),
};