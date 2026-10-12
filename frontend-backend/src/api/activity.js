import axios from 'axios';

/**
 * 活动管理 API（后台管理端 → gaia-backend :8088）。
 *
 * 后端契约：
 *   GET    /api/admin/activity/list              活动列表
 *   POST   /api/admin/activity                  新增活动
 *   PUT    /api/admin/activity/:id              更新活动
 *   DELETE /api/admin/activity/:id              删除活动
 *   POST   /api/admin/activity/:id/draw         手动开奖（保留，硬编码 draw）
 *   POST   /api/admin/activity/:id/ability/:code 通用触发能力（@Ability.backendTriggerable=true）
 *
 * 字段：id, name, link, startTime, endTime, status(0=草稿 1=上架 2=下架)
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

// 内存 mock：gaia-backend 暂未提供上述路径时的兜底数据，便于本地演示
let MOCK = [
  {
    id: 1001,
    bizCode: 'treasure',
    name: '会员夺宝',
    link: '#/treasure/1001',
    startTime: '2026-01-01 00:00:00',
    endTime: '2026-12-31 23:59:59',
    status: 1,
    configurations: {},
  },
];

export async function listActivities() {
  try {
    const data = unwrap(await http.get('/admin/activity/list'));
    if (Array.isArray(data)) {
      MOCK = data;
      return data;
    }
    return MOCK;
  } catch (e) {
    // 后端未启动或路径不存在时返回 mock，避免管理后台空白
    return MOCK;
  }
}

export async function createActivity(payload) {
  try {
    const data = unwrap(await http.post('/admin/activity', payload));
    if (data && data.id) {
      MOCK = [...MOCK, data];
      return data;
    }
    const next = {
      ...payload,
      id: Date.now(),
    };
    MOCK = [...MOCK, next];
    return next;
  } catch (e) {
    const next = { ...payload, id: Date.now() };
    MOCK = [...MOCK, next];
    return next;
  }
}

export async function updateActivity(id, payload) {
  try {
    const data = unwrap(await http.put(`/admin/activity/${id}`, payload));
    MOCK = MOCK.map((it) => (it.id === id ? { ...it, ...payload, id } : it));
    return data ?? { ...payload, id };
  } catch (e) {
    MOCK = MOCK.map((it) => (it.id === id ? { ...it, ...payload, id } : it));
    return { ...payload, id };
  }
}

export async function deleteActivity(id) {
  try {
    await http.delete(`/admin/activity/${id}`);
  } catch (e) {
    // mock 下也走兜底
  }
  MOCK = MOCK.filter((it) => it.id !== id);
}

/**
 * 对指定活动触发指定能力。后端会校验该能力的 @Ability.backendTriggerable=true。
 * 成功返回 { activityId, code }，失败抛 Error。
 */
export async function triggerAbility(activityId, code) {
  const data = unwrap(
    await http.post(`/admin/activity/${activityId}/ability/${encodeURIComponent(code)}`),
  );
  return data ?? { activityId, code };
}

export const STATUS_OPTIONS = [
  { value: 0, label: '草稿', color: 'default' },
  { value: 1, label: '已上架', color: 'success' },
  { value: 2, label: '已下架', color: 'warning' },
];

export function statusToOption(status) {
  return STATUS_OPTIONS.find((o) => o.value === status) || STATUS_OPTIONS[0];
}