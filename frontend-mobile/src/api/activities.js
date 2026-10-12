import { http } from './client.js';

/**
 * 活动 API（移动端 C 端）。
 *
 * 契约（gateway → server）：
 *   GET   /api/activities           -> { code, message, data: Activity[] }
 *   POST  /api/interact  (task_list)    -> 该活动下的任务列表
 *   POST  /api/interact  (task_execute) -> 执行任务，领取夺宝码
 *
 * 过滤口径由后端把控：{@code status ∈ {1,2} AND start_time <= now < end_time}，
 * 移动端不再做兜底/mock，调用方拿到空数组即视为"暂无活动"。
 *
 * interact 走玩法统一入口：bizCode=treasure（详见 gaia-app @BizProduct 索引）。
 * 后端 IFResult.model 在 success=true 时携带业务负载；fail=true 时携带 msgCode。
 */

function unwrap(res) {
  if (res == null) return null;
  if (typeof res === 'object' && 'code' in res) {
    if (res.code === 0 || res.code === '0' || res.code === 200) return res.data;
    return null;
  }
  return res;
}

/**
 * 在统一 interact 响应体中，按能力 code 取出对应的 IFResult。
 *
 * 返回结构：{ success: boolean, msgCode: string|null, model: any } | null
 * 调用方需自行根据 success 判断 model 是否可用。
 */
function pickAbilityResult(data, abilityCode) {
  const items = data && typeof data === 'object' ? data.items : null;
  if (!items || typeof items !== 'object') return null;
  return items[abilityCode] || null;
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

/**
 * 调 task_list ability 拉取活动下的任务列表。
 *
 * 后端 TaskListAbility.enrichProgress 已按当前 session 用户填充 executedCount
 * 与 available，无需前端再算一遍。失败或未注册时返回空数组。
 */
export async function getTaskList(activityId) {
  if (activityId == null) return [];
  const res = await http.post('/interact', {
    bizCode: 'treasure',
    models: [
      {
        assertType: 'task_list',
        extParams: { activityId: String(activityId) },
      },
    ],
  });
  const data = unwrap(res);
  const r = pickAbilityResult(data, 'task_list');
  if (!r || !r.success) return [];
  return Array.isArray(r.model) ? r.model : [];
}

/**
 * 调 task_execute ability 执行任务（领取夺宝码）。
 *
 * @returns {Promise<{ok: boolean, msgCode: string|null, result: any}>}
 *   - ok=true 时 result 为 TaskExecuteResultDTO（newCodes/totalCodes/executedCount/idempotent）。
 *   - ok=false 时 result 为 null，msgCode 携带失败原因（如 TASK_ALREADY_EXECUTED_TODAY / TASK_EXHAUSTED）。
 */
export async function executeTask({ activityId, taskId }) {
  if (activityId == null || taskId == null) {
    return { ok: false, msgCode: 'INVALID_PARAMS', result: null };
  }
  // 后端幂等键 requestId 由 client 自生成：同 requestId 在同一自然日内视为同一笔。
  const requestId = `web-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  const res = await http.post('/interact', {
    bizCode: 'treasure',
    models: [
      {
        assertType: 'task_execute',
        extParams: {
          activityId: String(activityId),
          taskId: String(taskId),
          requestId,
        },
      },
    ],
  });
  const data = unwrap(res);
  const r = pickAbilityResult(data, 'task_execute');
  if (!r) return { ok: false, msgCode: 'NO_RESULT', result: null };
  return {
    ok: !!r.success,
    msgCode: r.msgCode || null,
    result: r.success ? r.model : null,
  };
}
