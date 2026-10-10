import './index.css';

/**
 * 会员中心活动入口卡片
 * props:
 *   activity: { id, name, coverUrl, link, status, ... }
 *   onClick: () => void
 */
export default function ActivityCard({ activity, onClick }) {
  const statusText = STATUS_TEXT[activity.status] || '未知';
  const disabled = activity.status !== 1;

  return (
    <button
      type="button"
      className={`activity-card ${disabled ? 'activity-card--disabled' : ''}`}
      onClick={onClick}
      aria-label={`进入活动 ${activity.name}`}
      disabled={disabled}
    >
      <div className="activity-card__cover">
        {activity.coverUrl ? (
          <img src={activity.coverUrl} alt={activity.name} />
        ) : (
          <span className="activity-card__cover-fallback">{activity.name?.slice(0, 1) || '活'}</span>
        )}
      </div>
      <div className="activity-card__body">
        <div className="activity-card__name">{activity.name}</div>
        <div className={`activity-card__status activity-card__status--${activity.status}`}>
          {statusText}
        </div>
      </div>
    </button>
  );
}

const STATUS_TEXT = {
  0: '未开始',
  1: '进行中',
  2: '已结束',
};