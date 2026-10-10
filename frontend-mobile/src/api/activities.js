import { http } from './client.js';

/**
 * 活动 API（移动端 C 端）。
 *
 * 契约（gateway → server）：
 *   GET /api/activities
 *     -> { code, message, data: Activity[] }
 *   其中 Activity = { id, activityCode, name, coverUrl, link, startTime, endTime, status }
 *   status: 0=草稿 1=已上架 2=已下架
 *
 * 兼容：当后端暂未提供 /api/activities 时，getActivities 会回退到内置的占位数据，
 * 保证前端能跑通 demo。接入正式 gateway 后会优先返回服务端数据。
 */

const PLACEHOLDER = [
  {
    id: 1001,
    activityCode: 'treasure_2026q4',
    name: '会员夺宝',
    coverUrl: '',
    link: '/treasure/1001',
    startTime: '2026-01-01 00:00:00',
    endTime: '2026-12-31 23:59:59',
    status: 1,
  },
];

function unwrap(res) {
  if (res == null) return null;
  if (typeof res === 'object' && 'code' in res) {
    if (res.code === 0 || res.code === '0' || res.code === 200) return res.data;
    return null;
  }
  return res;
}

export async function getActivities() {
  try {
    const res = await http.get('/activities');
    const data = unwrap(res);
    if (Array.isArray(data) && data.length > 0) return data;
    return PLACEHOLDER;
  } catch (e) {
    // 网络/网关未启动等异常时回退到占位数据
    if (e && (e.status === 404 || e.name === 'AbortError')) return PLACEHOLDER;
    return PLACEHOLDER;
  }
}

export async function getActivity(id) {
  const list = await getActivities();
  return list.find((a) => String(a.id) === String(id)) || null;
}