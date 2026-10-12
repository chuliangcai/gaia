import { http } from './client.js';

/**
 * 活动 API（移动端 C 端）。
 *
 * 契约（gateway → server）：
 *   GET /api/activities
 *     -> { code, message, data: Activity[] }
 *   其中 Activity = { id, name, link, startTime, endTime, status }
 *   status: 0=草稿 1=已上架 2=已下架
 *
 * 过滤口径由后端把控：{@code status ∈ {1,2} AND start_time <= now < end_time}，
 * 移动端不再做兜底/mock，调用方拿到空数组即视为"暂无活动"。
 */

function unwrap(res) {
  if (res == null) return null;
  if (typeof res === 'object' && 'code' in res) {
    if (res.code === 0 || res.code === '0' || res.code === 200) return res.data;
    return null;
  }
  return res;
}

export async function getActivities() {
  const res = await http.get('/activities');
  const data = unwrap(res);
  return Array.isArray(data) ? data : [];
}

export async function getActivity(id) {
  const list = await getActivities();
  return list.find((a) => String(a.id) === String(id)) || null;
}
