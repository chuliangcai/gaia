import { useEffect, useMemo, useState } from 'react';
import { Modal, Form, Input, DatePicker, Select, Checkbox, Tag, Empty, message } from 'antd';
import dayjs from 'dayjs';
import { STATUS_OPTIONS } from '../../api/activity.js';
import { listGames, listAbilities } from '../../api/meta.js';
import AbilityConfigForm from '../AbilityConfigForm/index.jsx';

/**
 * 新增 / 编辑 活动表单弹窗。
 *
 * props:
 *   open: boolean
 *   initial: Activity | null       编辑时传入
 *   onCancel: () => void
 *   onSubmit: (values) => Promise<void>
 *
 * 字段：
 *   - 玩法（必填）— 从 /admin/meta/games 拉取
 *   - 能力（必填 ≥1）— 从 /admin/meta/abilities 拉取；勾选后按 configClassName
 *     去重联动渲染配置子表单（自研 JsonSchema→antd，详见 AbilityConfigForm）
 *   - 原有：标题、封面、链接、时间、状态
 *
 * 提交 payload：
 *   - bizCode 必填（玩法 code）
 *   - configurations = Map<configClassFqn, configObject>（按 ability.configClassName 合并）
 */
export default function ActivityFormModal({ open, initial, onCancel, onSubmit }) {
  const [form] = Form.useForm();
  const isEdit = !!initial;

  const [games, setGames] = useState([]);
  const [abilities, setAbilities] = useState([]);
  const [loadingMeta, setLoadingMeta] = useState(false);

  // form 是 abilities 选中的单一来源：edit 回填 / 勾选 / 取消勾选都直接落 form，
  // 由 Form.useWatch 同步给 UI，避免 useState + useEffect 双源不一致。
  const watched = Form.useWatch('abilities', form);
  const selectedCodes = Array.isArray(watched) ? watched : [];

  // 拉取元数据
  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setLoadingMeta(true);
    Promise.all([listGames(), listAbilities()])
      .then(([g, a]) => {
        if (cancelled) return;
        setGames(g || []);
        setAbilities(a || []);
      })
      .finally(() => !cancelled && setLoadingMeta(false));
    return () => { cancelled = true; };
  }, [open]);

  // 初始化 / 回填
  useEffect(() => {
    if (!open) return;
    if (initial) {
      const cfg = initial.configurations || {};
      // 优先用表单显式写入的 ability 编码集合（覆盖 Void-config ability
      // 不留 configurations 痕迹、无法反推的情况）。__ 前缀视为本表单私有元数据。
      let codes;
      if (Array.isArray(cfg.__abilityCodes__)) {
        codes = cfg.__abilityCodes__.filter((c) => typeof c === 'string');
      } else {
        // 兼容旧数据：仅按 configClassName 反推
        const classNames = new Set(
          Object.keys(cfg).filter((k) => !k.startsWith('__')),
        );
        codes = (abilities || [])
          .filter((a) => classNames.has(a.configClassName))
          .map((a) => a.code);
      }
      // 回填时剔除私有元数据，避免子表单误把它当 schema 渲染
      const abilityConfigs = Object.fromEntries(
        Object.entries(cfg).filter(([k]) => !k.startsWith('__')),
      );
      form.setFieldsValue({
        name: initial.name,
        coverUrl: initial.coverUrl,
        link: initial.link,
        timeRange:
          initial.startTime && initial.endTime
            ? [dayjs(initial.startTime), dayjs(initial.endTime)]
            : null,
        status: initial.status,
        bizCode: initial.bizCode || undefined,
        abilities: codes,
        abilityConfigs,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ status: 1, abilities: [] });
    }
  }, [open, initial, abilities, form]);

  // 按 configClassName 去重；同一 configClass 被多个 ability 共用时只渲染一份
  const distinctConfigs = useMemo(() => {
    const map = new Map();
    (selectedCodes || []).forEach((code) => {
      const a = abilities.find((x) => x.code === code);
      if (!a) return;
      // 跳过无独立配置的能力（configClassName = java.lang.Void）
      if (a.configClassName === 'java.lang.Void') return;
      if (!map.has(a.configClassName)) {
        map.set(a.configClassName, {
          className: a.configClassName,
          schema: a.configSchema,
          abilities: [a],
        });
      } else {
        map.get(a.configClassName).abilities.push(a);
      }
    });
    return Array.from(map.values());
  }, [selectedCodes, abilities]);

  // 按所属域（@Ability.bizNs）聚合，让"每个能力所属的域"成为一级视觉结构；
  // 避免把域塞在能力行内一颗小 Tag 里被忽略。
  // 无 bizNs 的归入"未分类"，并固定排在最后；其它域按字母序排。
  const groupedAbilities = useMemo(() => {
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

  // 单个能力勾选/取消勾选 — 直接写 form，避免内嵌受控 Checkbox 再维护一份 selectedCodes。
  const toggleAbility = (code, checked) => {
    const cur = form.getFieldValue('abilities') || [];
    const next = checked
      ? Array.from(new Set([...cur, code]))
      : cur.filter((c) => c !== code);
    form.setFieldsValue({ abilities: next });
  };

  const handleOk = async () => {
    let values;
    try {
      values = await form.validateFields();
    } catch (e) {
      // 表单校验失败时弹 toast 让用户看到原因（Antd 仅在字段下方画红框，
      // 弹窗顶部没有视觉提示，容易让人误以为"点保存没反应"）
      const firstFieldErr = e?.errorFields?.[0]?.errors?.[0];
      message.error(firstFieldErr || '请检查表单填写');
      throw e; // re-throw 让 Modal 保持打开 + 退出 loading
    }
    const [start, end] = values.timeRange || [];

    // 把 abilityConfigs 里所有 dayjs 转成字符串（避免 Jackson 收到 dayjs 实例）
    const configurations = stringifyDates(values.abilityConfigs || {});
    // 把选中的 ability code 列表一起序列化进 configurations（用 __ 前缀避开
    // 与 configClass FQN key 的命名冲突）。Void-config ability
    // （如 treasure_draw，configClassName=java.lang.Void）不会在 configurations
    // 留任何痕迹，存下编码后才能在编辑时正确回填。
    configurations.__abilityCodes__ = selectedCodes;

    const payload = {
      activityCode: initial?.activityCode,
      bizCode: values.bizCode,
      name: values.name,
      prizeDesc: '',
      prizeImgUrl: values.coverUrl || '',
      link: values.link || '',
      status: values.status,
      startTime: start ? start.format('YYYY-MM-DDTHH:mm:ss') : '',
      endTime: end ? end.format('YYYY-MM-DDTHH:mm:ss') : '',
      winnerCount: 1,
      configurations,
    };

    await onSubmit(payload);
  };

  return (
    <Modal
      title={isEdit ? '编辑活动' : '新建活动'}
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      okText="保存"
      cancelText="取消"
      destroyOnHidden
      width={720}
      confirmLoading={loadingMeta}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="name"
          label="活动标题"
          rules={[{ required: true, message: '请输入活动标题' }, { max: 50 }]}
        >
          <Input placeholder="例如：会员夺宝" />
        </Form.Item>

        <Form.Item name="coverUrl" label="封面图 URL">
          <Input placeholder="https://..." />
        </Form.Item>

        <Form.Item
          name="link"
          label="跳转链接"
          tooltip="前端内链使用相对路径（如 /treasure/1001），外链使用完整 URL"
        >
          <Input placeholder="/treasure/1001 或 https://..." />
        </Form.Item>

        <Form.Item
          name="timeRange"
          label="活动时间"
          rules={[{ required: true, message: '请选择活动时间' }]}
        >
          <DatePicker.RangePicker
            showTime={{ format: 'HH:mm' }}
            format="YYYY-MM-DD HH:mm"
            style={{ width: '100%' }}
          />
        </Form.Item>

        <Form.Item
          name="status"
          label="状态"
          rules={[{ required: true }]}
        >
          <Select
            options={STATUS_OPTIONS.map((o) => ({ value: o.value, label: o.label }))}
          />
        </Form.Item>

        <Form.Item
          name="bizCode"
          label="玩法"
          rules={[{ required: true, message: '请选择玩法' }]}
          tooltip="活动所属玩法（@BizProduct.code），由后台元数据 /admin/meta/games 动态拉取"
        >
          <Select
            placeholder="选择一个玩法"
            loading={loadingMeta}
            options={games.map((g) => ({ value: g.code, label: g.name }))}
          />
        </Form.Item>

        <Form.Item
          name="abilities"
          label="能力（可多选）"
          rules={[{ required: true, message: '请至少选择一个能力', type: 'array', min: 1 }]}
          tooltip="能力按所属域（@Ability.bizNs）分组展示；勾选后按 configClassName 去重联动渲染配置子表单"
        >
          {/* 占位：Form.Item 必须有 child 才能挂 cloneElement。
              真正可视的勾选 UI 放在下方的兄弟节点里 ——
              如果直接把 UI 放进来，Form.Item 会 cloneElement 把 onChange 注入到我们的容器 div，
              勾选 checkbox 时事件冒泡到容器，Antd 用 e.target.value="on" 覆盖掉我们 setFieldsValue 的数组。 */}
          <span aria-hidden style={{ display: 'none' }} />
        </Form.Item>

        {/* 可视 UI：脱离 Form.Item 的 FieldContext，cloneElement 不再干扰勾选事件。 */}
        <div data-testid="abilities-list">
          {abilities.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无可选能力" />
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {groupedAbilities.map(([bizNs, items]) => (
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
                      {bizNs === '__ungrouped__' ? '未分类' : bizNs}
                    </Tag>
                    <span style={{ color: '#999', fontSize: 12 }}>域</span>
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
                      <Checkbox
                        key={a.code}
                        checked={selectedCodes.includes(a.code)}
                        onChange={(e) => toggleAbility(a.code, e.target.checked)}
                      >
                        <span
                          style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
                        >
                          <span>{a.name}</span>
                          <code style={{ color: '#999', fontSize: 12 }}>{a.code}</code>
                        </span>
                      </Checkbox>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 联动配置类表单：按 configClassName 去重渲染 */}
        {distinctConfigs.length === 0 && selectedCodes.length > 0 && (
          <div style={{ marginBottom: 12, padding: 8, background: '#fafafa', borderRadius: 6 }}>
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="所选能力均无独立配置（如 treasure_draw），无需填写额外表单"
            />
          </div>
        )}

        {distinctConfigs.map(({ className, schema, abilities: sharedAbilities }) => (
          <div
            key={className}
            style={{
              marginBottom: 16,
              padding: 12,
              border: '1px solid #f0f0f0',
              borderRadius: 8,
              background: '#fff',
            }}
          >
            <div style={{ marginBottom: 12 }}>
              <span style={{ fontWeight: 600 }}>{shortClassName(className)}</span>
              <span style={{ marginLeft: 8, color: '#999', fontSize: 12 }}>
                （{sharedAbilities.map((a) => a.code).join(' / ')} 共用）
              </span>
            </div>
            <Form.Item name={['abilityConfigs', className]} style={{ marginBottom: 0 }}>
              <AbilityConfigForm
                schema={schema}
                formPath={['abilityConfigs', className]}
              />
            </Form.Item>
          </div>
        ))}
      </Form>
    </Modal>
  );
}

function shortClassName(fqn) {
  if (!fqn) return '';
  const last = fqn.split('.').pop();
  return last;
}

function stringifyDates(obj) {
  if (obj == null) return obj;
  if (dayjs.isDayjs(obj)) return obj.format('YYYY-MM-DD HH:mm:ss');
  if (Array.isArray(obj)) return obj.map(stringifyDates);
  if (typeof obj === 'object') {
    const r = {};
    for (const k in obj) r[k] = stringifyDates(obj[k]);
    return r;
  }
  return obj;
}
