import LotteryWheel from '../../components/LotteryWheel/index.jsx';
import { PRIZES } from '../../config/prizes.js';
import './index.css';

const DAILY_LIMIT = 3;
const RULES = [
  `每位用户每日可参与 ${DAILY_LIMIT} 次抽奖`,
  '实物奖品将在 3 个工作日内联系发放',
  '最终解释权归 Gaia 平台所有',
];

export default function Home() {
  return (
    <div className="home">
      <header className="home__header">
        <div className="home__brand">Gaia</div>
        <h1 className="home__title">幸运大转盘</h1>
        <p className="home__subtitle">转动好运，把惊喜带回家</p>
      </header>

      <main className="home__main">
        <div className="home__counter" aria-live="polite">
          今日参与上限 <strong>{DAILY_LIMIT}</strong> 次
        </div>

        <LotteryWheel />

        <p className="home__hint">点击中央按钮开始抽奖</p>
      </main>

      <section className="home__panel">
        <h2 className="home__panel-title">活动奖品</h2>
        <ul className="home__prize-list">
          {PRIZES.filter((p) => p.weight > 0).map((p) => (
            <li key={p.id} className="home__prize-item" style={{ background: p.color }}>
              <span className="home__prize-name">{p.name}</span>
              <span className="home__prize-text">{p.text}</span>
            </li>
          ))}
        </ul>

        <h2 className="home__panel-title">活动规则</h2>
        <ul className="home__rules">
          {RULES.map((rule, i) => (
            <li key={i}>{rule}</li>
          ))}
        </ul>
      </section>

      <footer className="home__footer">© Gaia Interactive · 互动平台</footer>
    </div>
  );
}
