import { useEffect, useState } from 'react';
import { navigate } from '../../router/HashRouter.jsx';
import { getActivity } from '../../api/activities.js';
import './index.css';

/**
 * 会员夺宝子页面。
 *
 * props 由 HashRouter 根据路由 /treasure/:id 注入：{ id }
 */
export default function TreasureHunt({ id }) {
  const [activity, setActivity] = useState(null);
  const [loading, setLoading] = useState(true);

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
              <div className="treasure-hunt__prize-name">{activity?.prizeDesc || '神秘大奖'}</div>
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
        <button type="button" className="treasure-hunt__cta" disabled>
          完成任务领取夺宝码（敬请期待）
        </button>
      </section>

      <section className="treasure-hunt__rules">
        <h2 className="treasure-hunt__rules-title">活动规则</h2>
        <ol>
          <li>活动期间完成任务即可获得夺宝码</li>
          <li>开奖时由系统随机抽取幸运用户</li>
          <li>中奖用户将在活动结束后收到通知</li>
        </ol>
      </section>
    </div>
  );
}