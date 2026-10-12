import { useEffect, useState, useMemo, useCallback } from 'react';
import { navigate } from '../../router/HashRouter.jsx';
import { getActivity, getTaskList, executeTask } from '../../api/activities.js';
import './index.css';

/**
 * 会员夺宝子页面。
 *
 * props 由 HashRouter 根据路由 /treasure/:id 注入：{ id }
 *
 * 页面拉取口径：
 *   1. getActivity(id)        — 活动基本信息（标题、奖品、时间）
 *   2. getTaskList(id)        — 任务清单（带用户维度的 executedCount / available）
 *   3. executeTask(...)       — 用户点击 CTA 时触发，完成任务领取夺宝码
 *
 * task_list 与 task_execute 的服务端实现见 gaia-domain/ability/task/
 * TaskListAbility 与 TaskExecuteAbility，通过 POST /api/interact (bizCode=treasure) 触达。
 */
export default function TreasureHunt({ id }) {
  const [activity, setActivity] = useState(null);
  const [loading, setLoading] = useState(true);
  const [tasks, setTasks] = useState([]);
  const [tasksLoading, setTasksLoading] = useState(true);
  const [executing, setExecuting] = useState(false);
  const [toast, setToast] = useState(null);

  // 活动基础信息
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const data = await getActivity(id);
      if (!cancelled) {
        setActivity(data);
        setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const reloadTasks = useCallback(async () => {
    setTasksLoading(true);
    const list = await getTaskList(id);
    setTasks(list);
    setTasksLoading(false);
  }, [id]);

  // 任务列表
  useEffect(() => {
    reloadTasks();
  }, [reloadTasks]);

  const sortedTasks = useMemo(() => {
    return [...tasks].sort((a, b) => {
      const sa = a.sortOrder ?? 0;
      const sb = b.sortOrder ?? 0;
      if (sa !== sb) return sa - sb;
      return (a.id ?? 0) - (b.id ?? 0);
    });
  }, [tasks]);

  const availableTask = useMemo(
    () => sortedTasks.find((t) => t.available),
    [sortedTasks],
  );

  const showToast = useCallback((msg, tone = 'info') => {
    setToast({ msg, tone });
    window.clearTimeout(showToast._t);
    showToast._t = window.setTimeout(() => setToast(null), 2200);
  }, []);

  const onClickCta = useCallback(async () => {
    if (executing) return;
    if (!availableTask) {
      showToast('今日暂无可执行的任务', 'warn');
      return;
    }
    setExecuting(true);
    try {
      const r = await executeTask({
        activityId: id,
        taskId: availableTask.id,
      });
      if (r.ok) {
        const earned =
          r.result && Array.isArray(r.result.newCodes) ? r.result.newCodes.length : 0;
        showToast(
          earned > 0 ? `领取成功，获得 ${earned} 个夺宝码` : '领取成功',
          'success',
        );
        await reloadTasks();
      } else {
        showToast(mapMsgCode(r.msgCode), 'warn');
      }
    } catch (e) {
      showToast('网络异常，请稍后再试', 'warn');
    } finally {
      setExecuting(false);
    }
  }, [executing, availableTask, id, reloadTasks, showToast]);

  return (
    <div className="page page--treasure-hunt">
      <header className="treasure-hunt__topbar">
        <button
          type="button"
          className="treasure-hunt__back"
          onClick={() => navigate('/')}
          aria-label="返回会员中心"
        >
          ‹
        </button>
        <h1 className="treasure-hunt__title">{activity?.name || '会员夺宝'}</h1>
        <span className="treasure-hunt__back treasure-hunt__back--placeholder" aria-hidden="true" />
      </header>

      <section className="treasure-hunt__hero">
        {loading ? (
          <div className="treasure-hunt__loading">加载中…</div>
        ) : (
          <>
            <div className="treasure-hunt__prize">
              <div className="treasure-hunt__prize-label">本期奖品</div>
              <div className="treasure-hunt__prize-name">
                {activity?.prizeDesc || '神秘大奖'}
              </div>
            </div>
            <div className="treasure-hunt__time">
              {activity?.startTime?.slice(0, 10)} ~ {activity?.endTime?.slice(0, 10)}
            </div>
          </>
        )}
      </section>

      <section className="treasure-hunt__panel">
        <div className="treasure-hunt__codes">
          <div className="treasure-hunt__codes-label">我的夺宝码</div>
          <div className="treasure-hunt__codes-value">0</div>
        </div>
        <button
          type="button"
          className="treasure-hunt__cta"
          onClick={onClickCta}
          disabled={executing || !availableTask}
        >
          {!availableTask
            ? '今日任务已全部完成'
            : executing
              ? '领取中…'
              : `完成任务领取夺宝码`}
        </button>
      </section>

      <section className="treasure-hunt__tasks" aria-label="任务列表">
        <h2 className="treasure-hunt__tasks-title">任务列表</h2>
        {tasksLoading ? (
          <div className="treasure-hunt__tasks-empty">任务加载中…</div>
        ) : sortedTasks.length === 0 ? (
          <div className="treasure-hunt__tasks-empty">本期暂未配置任务</div>
        ) : (
          <ul className="treasure-hunt__task-list">
            {sortedTasks.map((t) => (
              <li
                key={t.id}
                className={`treasure-hunt__task${t.available ? '' : ' treasure-hunt__task--done'}`}
              >
                <div className="treasure-hunt__task-main">
                  <div className="treasure-hunt__task-name">
                    {t.taskName || `任务 #${t.id}`}
                  </div>
                  {t.taskDesc ? (
                    <div className="treasure-hunt__task-desc">{t.taskDesc}</div>
                  ) : null}
                  <div className="treasure-hunt__task-progress">
                    <span className="treasure-hunt__task-progress-text">
                      进度 {t.executedCount ?? 0}/{t.maxExecutions ?? '∞'}
                    </span>
                    <span className="treasure-hunt__task-reward">
                      单次奖励 {t.rewardCount ?? 0} 码
                    </span>
                  </div>
                </div>
                <div className="treasure-hunt__task-status">
                  {t.available ? (
                    <span className="treasure-hunt__task-tag treasure-hunt__task-tag--avail">
                      可领取
                    </span>
                  ) : (
                    <span className="treasure-hunt__task-tag treasure-hunt__task-tag--done">
                      已完成
                    </span>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="treasure-hunt__rules">
        <h2 className="treasure-hunt__rules-title">活动规则</h2>
        <ol>
          <li>活动期间完成任务即可获得夺宝码</li>
          <li>开奖时由系统随机抽取幸运用户</li>
          <li>中奖用户将在活动结束后收到通知</li>
        </ol>
      </section>

      {toast ? (
        <div className={`treasure-hunt__toast treasure-hunt__toast--${toast.tone}`}>
          {toast.msg}
        </div>
      ) : null}
    </div>
  );
}

/**
 * 把后端 msgCode 翻译成中文提示。失败时若未识别，回退通用文案。
 */
function mapMsgCode(code) {
  switch (code) {
    case 'TASK_ALREADY_EXECUTED_TODAY':
      return '今日已领取过';
    case 'TASK_EXHAUSTED':
      return '任务次数已用完';
    case 'ACTIVITY_NOT_ONGOING':
      return '活动未在进行中';
    case 'TASK_NOT_FOUND':
    case 'ACTIVITY_NOT_FOUND':
      return '任务不存在';
    case 'USER_ID_REQUIRED':
      return '请先登录';
    case 'REQUEST_ID_REQUIRED':
    case 'ACTIVITY_ID_TASK_ID_REQUIRED':
    case 'INVALID_PARAMS':
      return '参数错误';
    default:
      return '领取失败，请稍后再试';
  }
}
