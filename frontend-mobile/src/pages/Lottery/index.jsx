import LotteryWheel from '../../components/LotteryWheel/index.jsx';
import { navigate } from '../../router/HashRouter.jsx';
import { PRIZES } from '../../config/prizes.js';
import './index.css';

const DAILY_LIMIT = 3;
const RULES = [
  `每位用户每日可参与 ${DAILY_LIMIT} 次抽奖`,
  '实物奖品将在 3 个工作日内联系发放',
  '最终解释权归 Gaia 平台所有',
];

export default function Lottery() {
  return (
    <div className="lottery">
      <header className="lottery__topbar">
        <button
          type="button"
          className="lottery__back"
          onClick={() => navigate('/')}
          aria-label="返回会员中心"
        >
          ‹
        </button>
        <h1 className="lottery__topbar-title">幸运大转盘</h1>
        <span className="lottery__back lottery__back--placeholder" aria-hidden="true" />
      </header>

      <main className="lottery__main">
        <LotteryWheel />

        <p className="lottery__hint">点击中央按钮开始抽奖</p>
      </main>

      <section className="lottery__panel">
        <h2 className="lottery__panel-title">活动奖品</h2>
        <ul className="lottery__prize-list">
          {PRIZES.filter((p) => p.weight > 0).map((p) => (
            <li key={p.id} className="lottery__prize-item" style={{ background: p.color }}>
              <span className="lottery__prize-name">{p.name}</span>
              <span className="lottery__prize-text">{p.text}</span>
            </li>
          ))}
        </ul>

        <h2 className="lottery__panel-title">活动规则</h2>
        <ul className="lottery__rules">
          {RULES.map((rule, i) => (
            <li key={i}>{rule}</li>
          ))}
        </ul>
      </section>

      <footer className="lottery__footer">© Gaia Interactive · 互动平台</footer>
    </div>
  );
}