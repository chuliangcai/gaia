// 转盘奖品配置：颜色与文案集中管理，方便后端下发时整体替换。
// 字段说明：
//   id        奖品 ID
//   name      奖品名称（用于结果弹窗）
//   text      扇区内显示文案（建议 <= 4 个汉字）
//   color     扇区填充色
//   textColor 扇区内文字颜色
//   weight    权重，整数越大越容易中（前端仅做演示用；真正抽奖以后端为准）
export const PRIZES = [
  {
    id: 1,
    name: '一等奖',
    text: 'iPhone',
    color: '#FF4D6D',
    textColor: '#fff',
    weight: 1,
  },
  {
    id: 2,
    name: '二等奖',
    text: '¥100',
    color: '#FF8FA3',
    textColor: '#fff',
    weight: 5,
  },
  {
    id: 3,
    name: '三等奖',
    text: '¥50',
    color: '#FFB3C1',
    textColor: '#5b1a2b',
    weight: 15,
  },
  {
    id: 4,
    name: '四等奖',
    text: '¥20',
    color: '#FFD6E0',
    textColor: '#5b1a2b',
    weight: 30,
  },
  {
    id: 5,
    name: '五等奖',
    text: '¥10',
    color: '#B5EAD7',
    textColor: '#10403a',
    weight: 30,
  },
  {
    id: 6,
    name: '谢谢参与',
    text: '谢谢参与',
    color: '#C7CEEA',
    textColor: '#1d2a5b',
    weight: 19,
  },
];
