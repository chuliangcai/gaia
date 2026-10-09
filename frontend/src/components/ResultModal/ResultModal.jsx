import './ResultModal.css';

/**
 * 中奖结果弹窗。
 * @param {Object} props
 * @param {boolean} props.open    - 是否可见
 * @param {Object} props.prize    - 奖品对象 { name, text, color }
 * @param {Function} props.onClose - 关闭回调
 */
export default function ResultModal({ open, prize, onClose }) {
  if (!open || !prize) return null;

  // 「谢谢参与」类未中奖奖品走灰色样式
  const isNoWin = prize.id === 6 || prize.weight <= 0;

  return (
    <div className="result-modal" role="dialog" aria-modal="true" aria-labelledby="result-title">
      <div className="result-modal__backdrop" onClick={onClose} />
      <div className="result-modal__card" style={{ '--prize-color': prize.color }}>
        <div className="result-modal__ribbon">中奖啦</div>
        <h3 id="result-title" className="result-modal__title">
          恭喜获得
        </h3>
        <div className="result-modal__prize">
          <div className="result-modal__prize-name">{prize.name}</div>
          <div className="result-modal__prize-text">{prize.text}</div>
        </div>
        {isNoWin && <div className="result-modal__tip">再接再厉，下一次大奖属于你</div>}
        <button className="result-modal__btn" onClick={onClose}>
          收下奖品
        </button>
      </div>
    </div>
  );
}
