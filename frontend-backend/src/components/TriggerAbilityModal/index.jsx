import { useEffect, useMemo, useState } from 'react';
import { Modal, Radio, Tag, Empty, Spin, Alert } from 'antd';
import { listTriggerableAbilities } from '../../api/meta.js';

/**
 * 触发能力弹窗。
 *
 * props:
 *   open: boolean
 *   activity: { id, name, bizCode } | null   当前选中活动
 *   onCancel: () => void
 *   onSubmit: (code: string) => Promise<void>   用户点确定后回调，父组件负责关弹窗 + 调 API
 *
 * 后端契约：
 *   GET  /api/admin/meta/abilities/triggerable
 *   POST /api/admin/activity/:id/ability/:code
 *
 * 内部按 bizNs 分组渲染（样式对齐 ActivityFormModal 的分组结构），单选，
 * 弹窗打开时拉一次元数据；空列表 / 加载失败都走 Empty。
 */
export default function TriggerAbilityModal({ open, activity, onCancel, onSubmit }) {
  const [abilities, setAbilities] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedCode, setSelectedCode] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // 拉取元数据
  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setLoading(true);
    setSelectedCode(null);
    listTriggerableAbilities()
      .then((list) => {
        if (cancelled) return;
        setAbilities(Array.isArray(list) ? list : []);
      })
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [open]);

  // 按 bizNs 分组（与 ActivityFormModal.groupedAbilities 同款排序规则）
  const grouped = useMemo(() => {
    const map = new Map();
    (abilities || []).forEach((a) => {
      const ns = a.bizNs || '__ungrouped__';
      if (!map.has(ns)) map.set(ns, []);
      map.get(ns).push(a);
    });
    return Array.from(map.entries()).sort(([a], [b]) => {
      if (a === '__ungrouped__') return 1;
      if (b === '__ungrouped__') return -1;
      return a.localeCompare(b);
    });
  }, [abilities]);

  const handleOk = async () => {
    if (!selectedCode) return;
    setSubmitting(true);
    try {
      await onSubmit(selectedCode);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={
        activity
          ? `触发能力：${activity.name || `#${activity.id}`}`
          : '触发能力'
      }
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      okText="确定触发"
      cancelText="取消"
      okButtonProps={{ disabled: !selectedCode, loading: submitting }}
      destroyOnHidden
      width={640}
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="只列出 @Ability.backendTriggerable=true 的能力；后端会再次校验。"
      />

      {loading ? (
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          <Spin />
        </div>
      ) : abilities.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无可触发的能力" />
      ) : (
        <Radio.Group
          value={selectedCode}
          onChange={(e) => setSelectedCode(e.target.value)}
          style={{ width: '100%' }}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {grouped.map(([bizNs, items]) => (
              <div
                key={bizNs}
                style={{
                  border: '1px solid #f0f0f0',
                  borderRadius: 8,
                  overflow: 'hidden',
                  background: '#fafafa',
                }}
              >
                <div
                  style={{
                    padding: '8px 12px',
                    borderBottom: '1px solid #f0f0f0',
                    background: '#fff',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                  }}
                >
                  <Tag color="geekblue" style={{ margin: 0, fontWeight: 600 }}>
                    {bizNs === '__ungrouped__'
                      ? '未分类'
                      : (items.find((i) => i.domainName)?.domainName || bizNs)}
                  </Tag>
                  {bizNs !== '__ungrouped__' && (
                    <code style={{ color: '#999', fontSize: 12 }}>{bizNs}</code>
                  )}
                  <span style={{ color: '#bbb', fontSize: 12 }}>
                    · {items.length} 个能力
                  </span>
                </div>
                <div
                  style={{
                    padding: '10px 12px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 6,
                  }}
                >
                  {items.map((a) => (
                    <Radio key={a.code} value={a.code}>
                      <span
                        style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
                      >
                        <span>{a.name}</span>
                        <code style={{ color: '#999', fontSize: 12 }}>{a.code}</code>
                      </span>
                    </Radio>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </Radio.Group>
      )}
    </Modal>
  );
}
