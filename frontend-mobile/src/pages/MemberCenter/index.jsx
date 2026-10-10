import { useEffect, useState } from 'react';
import ActivityCard from '../../components/ActivityCard/index.jsx';
import { getActivities } from '../../api/activities.js';
import { listAvailableUsers } from '../../api/auth.js';
import { useCurrentUser } from '../../auth/CurrentUserContext.jsx';
import { navigate } from '../../router/HashRouter.jsx';
import './index.css';

export default function MemberCenter() {
  const { userId, loading: userLoading, switchUser } = useCurrentUser();

  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 用户切换面板
  const [pickerOpen, setPickerOpen] = useState(false);
  const [pickerLoading, setPickerLoading] = useState(false);
  const [pickerUsers, setPickerUsers] = useState([]);
  const [pickerError, setPickerError] = useState(null);
  const [switchingId, setSwitchingId] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const list = await getActivities();
        if (!cancelled) setActivities(list);
      } catch (e) {
        if (!cancelled) setError(e);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const handleActivityClick = (activity) => {
    if (!activity.link) return;
    if (activity.link.startsWith('#/') || activity.link.startsWith('/')) {
      navigate(activity.link.replace(/^#/, ''));
    } else {
      window.location.href = activity.link;
    }
  };

  const openPicker = async () => {
    setPickerOpen(true);
    setPickerLoading(true);
    setPickerError(null);
    try {
      const users = await listAvailableUsers();
      setPickerUsers(users);
    } catch (e) {
      setPickerError(e);
    } finally {
      setPickerLoading(false);
    }
  };

  const closePicker = () => {
    setPickerOpen(false);
  };

  const handlePick = async (id) => {
    if (id === userId || switchingId != null) return;
    setSwitchingId(id);
    try {
      await switchUser(id);
      closePicker();
    } catch (e) {
      // 失败保留面板以便用户重试
      setPickerError(e);
    } finally {
      setSwitchingId(null);
    }
  };

  const headerNick = userLoading ? '加载中…' : (userId != null ? `用户 #${userId}` : '未选择用户');
  const headerLevel = userId != null ? '点击右下角切换' : '请选择当前会话用户';

  return (
    <div className="page page--member-center">
      <header className="member-center__header">
        <div className="member-center__avatar" aria-hidden="true">G</div>
        <div className="member-center__user">
          <div className="member-center__nick">{headerNick}</div>
          <div className="member-center__level">{headerLevel}</div>
        </div>
      </header>

      <section className="member-center__section">
        <h2 className="member-center__title">热门活动</h2>
        {loading && <div className="member-center__loading">加载中…</div>}
        {error && !loading && (
          <div className="member-center__error">活动加载失败，请稍后再试</div>
        )}
        {!loading && !error && activities.length === 0 && (
          <div className="member-center__empty">暂无活动，敬请期待</div>
        )}
        <ul className="member-center__list">
          {activities.map((a) => (
            <li key={a.id}>
              <ActivityCard activity={a} onClick={() => handleActivityClick(a)} />
            </li>
          ))}
        </ul>
      </section>

      {/* 悬浮按钮：切用户 */}
      <button
        type="button"
        className="fab fab--switch-user"
        aria-label="切换当前用户"
        onClick={openPicker}
      >
        <span className="fab__icon" aria-hidden="true">⇄</span>
        <span className="fab__label">{userId != null ? `#${userId}` : '切用户'}</span>
      </button>

      {/* 用户选择面板 */}
      {pickerOpen && (
        <div className="picker-mask" onClick={closePicker} role="presentation">
          <div
            className="picker"
            role="dialog"
            aria-modal="true"
            aria-label="切换当前会话用户"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="picker__header">
              <h3 className="picker__title">切换用户</h3>
              <button
                type="button"
                className="picker__close"
                aria-label="关闭"
                onClick={closePicker}
              >
                ×
              </button>
            </div>
            <div className="picker__body">
              {pickerLoading && <div className="picker__loading">加载候选用户…</div>}
              {pickerError && !pickerLoading && (
                <div className="picker__error">加载失败：{pickerError.message || '网络异常'}</div>
              )}
              {!pickerLoading && !pickerError && pickerUsers.length === 0 && (
                <div className="picker__empty">暂无可用用户</div>
              )}
              {!pickerLoading && !pickerError && pickerUsers.length > 0 && (
                <ul className="picker__list">
                  {pickerUsers.map((u) => {
                    const isCurrent = u.userId === userId;
                    const isSwitching = switchingId === u.userId;
                    return (
                      <li key={u.userId}>
                        <button
                          type="button"
                          className={
                            'picker__item' +
                            (isCurrent ? ' picker__item--current' : '')
                          }
                          onClick={() => handlePick(u.userId)}
                          disabled={switchingId != null}
                        >
                          <span className="picker__item-id">#{u.userId}</span>
                          <span className="picker__item-level">
                            {u.level || 'NORMAL'}
                            {u.status === 1 ? ' · 有效' : ' · 失效'}
                          </span>
                          {isCurrent && <span className="picker__item-tag">当前</span>}
                          {isSwitching && <span className="picker__item-tag">切换中…</span>}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}