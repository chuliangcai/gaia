import { useEffect, useState } from 'react';

/**
 * 极简 Hash 路由（无依赖）。
 * 约定路由格式：#/path/:param1/:param2
 * - 支持 useRoute() 读取当前路径与参数
 * - 支持 navigate(path) 跳转
 */

function parseHash() {
  const raw = window.location.hash.replace(/^#/, '') || '/';
  const [pathPart, queryPart] = raw.split('?');
  const segments = pathPart.split('/').filter(Boolean);
  const path = '/' + segments.join('/');
  const params = {};
  segments.forEach((seg, i) => {
    // 把 :name 占位符解析到 params 上
  });
  // query string
  const query = {};
  if (queryPart) {
    queryPart.split('&').forEach((kv) => {
      const [k, v] = kv.split('=');
      if (k) query[decodeURIComponent(k)] = decodeURIComponent(v || '');
    });
  }
  return { path, segments, params, query };
}

export function navigate(path) {
  if (!path.startsWith('/')) path = '/' + path;
  window.location.hash = path;
}

let listeners = new Set();

export function useRoute() {
  const [route, setRoute] = useState(() => parseHash());
  useEffect(() => {
    const cb = () => setRoute(parseHash());
    window.addEventListener('hashchange', cb);
    return () => window.removeEventListener('hashchange', cb);
  }, []);
  return route;
}

/**
 * <Router routes={...} />
 * routes: [{ pattern: '/member', element: <.../> }, { pattern: '/treasure/:id', element: <.../> }]
 */
export default function HashRouter({ routes, fallback = null }) {
  const { path } = useRoute();
  for (const r of routes) {
    const params = matchPath(r.pattern, path);
    if (params) {
      const Element = r.element;
      return <Element {...params} />;
    }
  }
  return fallback;
}

function matchPath(pattern, path) {
  const ps = pattern.split('/').filter(Boolean);
  const xs = path.split('/').filter(Boolean);
  if (ps.length !== xs.length) return null;
  const params = {};
  for (let i = 0; i < ps.length; i++) {
    if (ps[i].startsWith(':')) {
      params[ps[i].slice(1)] = decodeURIComponent(xs[i]);
    } else if (ps[i] !== xs[i]) {
      return null;
    }
  }
  return params;
}