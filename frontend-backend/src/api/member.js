import axios from 'axios';

/**
 * 会员管理 API（后台管理端 → gaia-backend :8092）。
 *
 * 后端契约：
 *   GET    /api/admin/members/list?status=&level=   会员列表（按 id 倒序，可选过滤）
 *   POST   /api/admin/members                      新增会员（返回 userId）
 *   GET    /api/admin/members/:userId              会员详情
 *   PUT    /api/admin/members/:userId              更新会员（level / status / expireAt）
 *   DELETE /api/admin/members/:userId              删除会员（物理删除）
 *
 * 字段：id, userId, level, status(0=失效 1=有效), joinAt, expireAt, createTime, updateTime
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

/**
 * 解包 Result：{ code, message, data } → data
 */
function unwrap(res) {
  if (res == null) return null;
  if (typeof res === 'object' && 'code' in res) {
    if (res.code === 0 || res.code === '0' || res.code === 200) return res.data;
    throw new Error(res.message || `code=${res.code}`);
  }
  return res;
}

// 内存 mock：后端暂未提供上述路径时的兜底数据，便于本地演示
let MOCK = [
  {
    id: 1,
    userId: 10001,
    level: 'NORMAL',
    status: 1,
    joinAt: '2026-01-01 00:00:00',
    expireAt: null,
    createTime: '2026-01-01 00:00:00',
    updateTime: '2026-01-01 00:00:00',
  },
];

export async function listMembers(params = {}) {
  try {
    const data = unwrap(await http.get('/admin/members/list', { params }));
    if (Array.isArray(data)) {
      MOCK = data;
      return data;
    }
    return MOCK;
  } catch (e) {
    return MOCK;
  }
}

export async function createMember(payload) {
  try {
    const data = unwrap(await http.post('/admin/members', payload));
    if (data) return data;
  } catch (e) {
    // mock fallback
  }
  const next = {
    ...payload,
    id: Date.now(),
    joinAt: payload.joinAt || new Date().toISOString(),
    createTime: new Date().toISOString(),
    updateTime: new Date().toISOString(),
  };
  MOCK = [next, ...MOCK];
  return next;
}

export async function updateMember(userId, payload) {
  try {
    await http.put(`/admin/members/${userId}`, payload);
  } catch (e) {
    // mock fallback
  }
  MOCK = MOCK.map((it) =>
    it.userId === userId
      ? {
          ...it,
          ...payload,
          updateTime: new Date().toISOString(),
        }
      : it,
  );
}

export async function deleteMember(userId) {
  try {
    await http.delete(`/admin/members/${userId}`);
  } catch (e) {
    // mock fallback
  }
  MOCK = MOCK.filter((it) => it.userId !== userId);
}

export const LEVEL_OPTIONS = [
  { value: 'NORMAL', label: '普通', color: 'default' },
  { value: 'VIP', label: 'VIP', color: 'gold' },
  { value: 'SVIP', label: 'SVIP', color: 'magenta' },
];

export const STATUS_OPTIONS = [
  { value: 1, label: '有效', color: 'success' },
  { value: 0, label: '失效', color: 'default' },
];

export function levelToOption(level) {
  return (
    LEVEL_OPTIONS.find((o) => o.value === level) || {
      value: level,
      label: level || '—',
      color: 'default',
    }
  );
}

export function statusToOption(status) {
  return (
    STATUS_OPTIONS.find((o) => o.value === status) || {
      value: status,
      label: status === 1 ? '有效' : status === 0 ? '失效' : '—',
      color: 'default',
    }
  );
}
