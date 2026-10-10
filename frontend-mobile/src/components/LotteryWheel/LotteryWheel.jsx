import { useEffect, useRef, useState } from 'react';
import { PRIZES } from '../../config/prizes.js';
import { pickPrizeByWeight } from '../../utils/random.js';
import ResultModal from '../ResultModal/index.jsx';
import './LotteryWheel.css';

const CX = 150;
const CY = 150;
const R = 140;
const TEXT_R = 78;
const SEG = 360 / PRIZES.length;
const SPIN_DURATION_MS = 4500;
const MIN_SPINS = 6;

/**
 * 极坐标 → 笛卡尔坐标。
 * 0° 表示 12 点钟方向；角度顺时针递增（与屏幕坐标一致）。
 */
function polarToCartesian(cx, cy, r, angleDeg) {
  const rad = ((angleDeg - 90) * Math.PI) / 180;
  return {
    x: cx + r * Math.cos(rad),
    y: cy + r * Math.sin(rad),
  };
}

/**
 * 描述一个扇形（饼图切片）的 SVG 路径 d 属性。
 * startAngle、endAngle 单位为「度」，0° 在顶部，顺时针。
 */
function describeSlice(startAngle, endAngle) {
  const start = polarToCartesian(CX, CY, R, endAngle);
  const end = polarToCartesian(CX, CY, R, startAngle);
  const largeArc = endAngle - startAngle <= 180 ? '0' : '1';
  return `M ${CX} ${CY} L ${start.x} ${start.y} A ${R} ${R} 0 ${largeArc} 0 ${end.x} ${end.y} Z`;
}

export default function LotteryWheel() {
  // 累计旋转角度；每次抽奖叠加，避免归零造成动画回弹。
  const [rotation, setRotation] = useState(0);
  const [isSpinning, setIsSpinning] = useState(false);
  const [result, setResult] = useState(null);
  const transitionTimer = useRef(null);

  // 组件卸载时清掉尚未触发的定时器，避免在已卸载组件上调 setState。
  useEffect(() => {
    return () => {
      if (transitionTimer.current) clearTimeout(transitionTimer.current);
    };
  }, []);

  function spin() {
    if (isSpinning) return;

    setIsSpinning(true);

    // 1) 决定奖项（先按权重抽，后续接入后端 RPC 时替换为 await gaiaApi.lottery.draw()）。
    const prize = pickPrizeByWeight();

    // 2) 计算让该扇区停在指针下方所需的旋转。
    // 旋转 θ 后，原角度 α 的点落在 α + θ 处；要让「扇区中心」落到指针位置（0°）：
    //   θ = 360 * spins - (prizeIndex * SEG + SEG / 2)
    const prizeIndex = PRIZES.findIndex((p) => p.id === prize.id);
    const centerAngle = prizeIndex * SEG + SEG / 2;
    const extraTurns = MIN_SPINS + Math.floor(Math.random() * 2); // 6~7 圈
    const baseRotation = Math.floor(rotation / 360) * 360;
    // 当前累积整圈数 + 新增圈数，避免负向位移
    const nextRotation = baseRotation + extraTurns * 360 - centerAngle;
    setRotation(nextRotation);

    // 3) 动画结束后弹出结果。
    if (transitionTimer.current) clearTimeout(transitionTimer.current);
    transitionTimer.current = setTimeout(() => {
      transitionTimer.current = null;
      setIsSpinning(false);
      setResult(prize);
    }, SPIN_DURATION_MS);
  }

  function closeResult() {
    setResult(null);
  }

  return (
    <div className="lottery">
      <div className="lottery__halo" aria-hidden="true" />
      <div className="lottery__ring">
        <div
          className="lottery__wheel"
          style={{
            transform: `rotate(${rotation}deg)`,
            transition: isSpinning
              ? `transform ${SPIN_DURATION_MS}ms cubic-bezier(0.17, 0.67, 0.12, 0.99)`
              : 'none',
          }}
        >
          <svg viewBox="0 0 300 300" className="lottery__svg" aria-label="大转盘">
            {PRIZES.map((prize, i) => {
              const startAngle = i * SEG;
              const endAngle = startAngle + SEG;
              const textAngle = startAngle + SEG / 2;
              const textPos = polarToCartesian(CX, CY, TEXT_R, textAngle);
              return (
                <g key={prize.id}>
                  <path d={describeSlice(startAngle, endAngle)} fill={prize.color} />
                  <text
                    x={textPos.x}
                    y={textPos.y}
                    fill={prize.textColor}
                    fontSize="15"
                    fontWeight="700"
                    textAnchor="middle"
                    dominantBaseline="middle"
                  >
                    {prize.text}
                  </text>
                </g>
              );
            })}

            {/* 扇区分隔线，避免色块相接处出现锯齿/色差 */}
            {PRIZES.map((prize, i) => {
              const p = polarToCartesian(CX, CY, R, i * SEG);
              return (
                <line
                  key={`line-${prize.id}`}
                  x1={CX}
                  y1={CY}
                  x2={p.x}
                  y2={p.y}
                  stroke="rgba(255,255,255,0.85)"
                  strokeWidth="2"
                />
              );
            })}

            {/* 中心装饰圆 */}
            <circle cx={CX} cy={CY} r="22" fill="#fff5f7" stroke="#ff4d6d" strokeWidth="3" />
          </svg>
        </div>

        {/* 顶部指针（不参与旋转） */}
        <div className="lottery__pointer" aria-hidden="true">
          <div className="lottery__pointer-triangle" />
        </div>

        {/* 中央按钮（位于 SVG 中心圆位置，覆盖在 SVG 之上） */}
        <button
          className="lottery__spin-btn"
          onClick={spin}
          disabled={isSpinning}
          aria-label={isSpinning ? '抽奖中' : '开始抽奖'}
        >
          <span className="lottery__spin-btn-inner">
            {isSpinning ? '抽奖中' : 'GO'}
          </span>
        </button>
      </div>

      <ResultModal
        open={!!result}
        prize={result}
        onClose={closeResult}
      />
    </div>
  );
}
