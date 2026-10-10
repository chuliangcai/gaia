import { http } from './client.js';

/**
 * 鉴权 / 当前用户切换 API。
 *
 * 契约（gateway → server）：
 *   GET  /api/auth/me
 *     -> { code: 0, message, data: { userId } }           // 200
 *     -> { code: 401, message }                            // 未切过用户
 *   GET  /api/auth/users
 *     -> { code: 0, message, data: MemberSummary[] }
 *   POST /api/auth/switch  body: { userId }
 *     -> { code: 0, message, data: { userId } }
 *
 * 切换成功后服务端通过 Set-Cookie 写入 SESSION cookie（Spring Session），
 * 后续 /api/interact 会自动以该 session.userId 为准。
 */

function unwrap(res) {
  if (res == null) return null;
  if (typeof res === 'object' && 'code' in res) return res;
  return res;
}

/**
 * 获取当前会话用户。未切换 / cookie 失效时返回 null。
 * （401 在 unwrap 之外抛错，故需要包一层 try）
 */
export async function getCurrentUser() {
  try {
    const res = await http.get('/auth/me');
    const env = unwrap(res);
    if (env && env.code === 0 && env.data) return env.data.userId ?? null;
    return null;
  } catch (e) {
    // 401 / 网络异常 都视为"未登录"
    return null;
  }
}

/**
 * 列出可切换的会员用户。
 */
export async function listAvailableUsers() {
  const res = await http.get('/auth/users');
  const env = unwrap(res);
  if (env && env.code === 0 && Array.isArray(env.data)) return env.data;
  return [];
}

/**
 * 把当前会话切到指定用户；服务端会写 SESSION cookie。
 */
export async function switchCurrentUser(userId) {
  const res = await http.post('/auth/switch', { userId });
  const env = unwrap(res);
  if (env && env.code === 0 && env.data) return env.data.userId;
  throw new Error(env?.message || '切换用户失败');
}