import axios from 'axios';

/**
 * 元数据 API（后台管理端 → gaia-bootstrap :8091 / gaia-backend :8092）。
 *
 * - /admin/meta/games     → gaia-bootstrap 8091（@BizProduct 元数据）
 * - /admin/meta/abilities → gaia-backend 8092（@Ability 元数据）
 *
 * 两个端点的来源不同（games 必须放在 gaia-bootstrap，因为 @BizProduct 实现类
 * 只在 gaia-app 模块，gaia-backend 不依赖 gaia-app，按 gaia-app-scope 约束）。
 * 由 vite.config.js 的 proxy rewrite 转发到对应端口。
 */

const http = axios.create({
  baseURL: '/api',
  timeout: 8000,
});

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err?.response?.data?.message || err.message || '请求失败';
    return Promise.reject(new Error(msg));
  },
);

function unwrap(res) {
  if (res == null) return [];
  if (typeof res === 'object' && 'code' in res) {
    if (res.code === 0 || res.code === '0' || res.code === 200) return res.data;
    throw new Error(res.message || `code=${res.code}`);
  }
  return res;
}

/** 列出全部玩法（@BizProduct）。失败时返回空数组，不抛错 — 让表单能继续渲染。 */
export async function listGames() {
  try {
    const data = unwrap(await http.get('/admin/meta/games'));
    return Array.isArray(data) ? data : [];
  } catch (e) {
    console.warn('listGames failed:', e?.message || e);
    return [];
  }
}

/**
 * 列出全部能力（@Ability）。可按 bizNs 过滤；不传 = 全量。
 * 返回项含 configClassName + configSchema，用于前端动态渲染配置表单。
 */
export async function listAbilities(bizNs) {
  try {
    const data = unwrap(await http.get('/admin/meta/abilities', {
      params: bizNs ? { bizNs } : undefined,
    }));
    return Array.isArray(data) ? data : [];
  } catch (e) {
    console.warn('listAbilities failed:', e?.message || e);
    return [];
  }
}
