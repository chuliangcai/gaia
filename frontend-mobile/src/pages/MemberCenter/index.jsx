import { useEffect, useState } from 'react';
import ActivityCard from '../../components/ActivityCard/index.jsx';
import { getActivities } from '../../api/activities.js';
import { navigate } from '../../router/HashRouter.jsx';
import './index.css';

export default function MemberCenter() {
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

  const handleClick = (activity) => {
    if (!activity.link) return;
    // 内链（#/treasure/123）走 hash 路由；外链直接 window.location
    if (activity.link.startsWith('#/') || activity.link.startsWith('/')) {
      navigate(activity.link.replace(/^#/, ''));
    } else {
      window.location.href = activity.link;
    }
  };

  return (
    <div className="page page--member-center">
      <header className="member-center__header">
        <div className="member-center__avatar" aria-hidden="true">G</div>
        <div className="member-center__user">
          <div className="member-center__nick">Gaia 会员</div>
          <div className="member-center__level">Lv.3 · 至尊会员</div>
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
              <ActivityCard activity={a} onClick={() => handleClick(a)} />
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}