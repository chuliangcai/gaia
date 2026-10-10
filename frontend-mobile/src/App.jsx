import { useEffect } from 'react';
import HashRouter, { navigate } from './router/HashRouter.jsx';
import MemberCenter from './pages/MemberCenter/index.jsx';
import TreasureHunt from './pages/TreasureHunt/index.jsx';
import Lottery from './pages/Lottery/index.jsx';

/**
 * 路由表：
 *   /                     会员中心（默认入口）
 *   /treasure/:id         会员夺宝子页面
 *   /lottery              大转盘抽奖子页面（保留，未在会员中心默认展示）
 */
const ROUTES = [
  { pattern: '/', element: MemberCenter },
  { pattern: '/treasure/:id', element: TreasureHunt },
  { pattern: '/lottery', element: Lottery },
];

function NotFound() {
  useEffect(() => {
    navigate('/');
  }, []);
  return null;
}

export default function App() {
  return <HashRouter routes={ROUTES} fallback={<NotFound />} />;
}