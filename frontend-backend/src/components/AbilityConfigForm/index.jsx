import { Form, Input, InputNumber, Select, Switch, DatePicker, Button } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

/**
 * 自研 JsonSchema → antd 控件递归渲染器。
 *
 * 接收父级 antd Form 的一个 name 路径（通常是 `['abilityConfigs', '<configClassFqn>']`），
 * 内部用 `<Form.List name={path}>` 或 `<Form.Item name={[...path, k]}>` 嵌套，
 * 由 antd 自动管理 values 与校验。
 *
 * ⚠️ SchemaNode 必须把父级 <Form.Item> cloneElement 注入的 value/onChange 透传给
 * 真实控件（<Input> / <InputNumber> / <DatePicker> / <Select> / <Switch>）。如果像
 * 之前那样 <Input /> 不接任何 props，Form.Item 把 value 给了 SchemaNode 但没人
 * 往下传，编辑回填时控件永远显示空。
 *
 * 支持的 schema 形态（与后端 ConfigSchemaIntrospector 输出对齐）：
 *   - string + enum                 → Select
 *   - string + format=date-time     → DatePicker(showTime)
 *   - string + format=date          → DatePicker
 *   - string                        → Input
 *   - integer / number              → InputNumber
 *   - boolean                       → Switch
 *   - object + properties           → 嵌套 Form.Item
 *   - array                         → Form.List（添加/删除按钮）
 *   - 其他 / 自引用 / 超深度         → JSON TextArea 兜底
 *
 * props:
 *   - formPath: (string | number)[]   父 Form 上的 name 路径
 *   - schema: object                  JSON Schema（来自 AbilityDescriptor.configSchema）
 *   - 其余由 Form.Item cloneElement 注入的 value / onChange 由 SchemaNode 透传给控件
 */
export default function AbilityConfigForm({ formPath, schema }) {
  if (!schema || typeof schema !== 'object') {
    return (
      <Input.TextArea
        rows={3}
        placeholder="该能力无独立配置 schema"
        disabled
        value=""
      />
    );
  }
  return <SchemaNode schema={schema} path={formPath} />;
}

function SchemaNode({ schema, path, value, onChange }) {
  // 透传给 antd 受控控件的 props：value / onChange。DatePicker 因为输入是
  // dayjs、底层 Form.Item 期望 ISO 字符串，往返需要包一层转换。
  const ctrl = ctrlPropsFor(schema, value, onChange);

  const t = schema?.type;

  // string + enum
  if (t === 'string' && Array.isArray(schema.enum)) {
    return <Select {...ctrl} allowClear options={schema.enum.map((v) => ({ value: v, label: v }))} style={{ width: '100%' }} />;
  }
  // string + date-time
  if (t === 'string' && schema.format === 'date-time') {
    return (
      <DatePicker
        {...ctrl}
        showTime={{ format: 'HH:mm:ss' }}
        format="YYYY-MM-DD HH:mm:ss"
        style={{ width: '100%' }}
      />
    );
  }
  // string + date
  if (t === 'string' && schema.format === 'date') {
    return (
      <DatePicker
        {...ctrl}
        format="YYYY-MM-DD"
        style={{ width: '100%' }}
      />
    );
  }
  // string
  if (t === 'string') return <Input {...ctrl} />;
  // integer / number
  if (t === 'integer' || t === 'number') {
    return <InputNumber {...ctrl} style={{ width: '100%' }} />;
  }
  // boolean
  if (t === 'boolean') return <Switch {...ctrl} />;
  // array — 数组场景不能用 SchemaNode 自己被 Form.Item 包来间接生成 Form.List；
  // 那样会出现外层 <Form.Item name=['…','tasks']> 包着 <Form.List name=['…','tasks']>
  // 的双重 owner，antd 5 同名路径只允许一个 owner，外层抢到后内层 Form.List.fields
  // 会变成空。统一由 ArrayField 直接渲染 Form.List，调用方决定是否在外层加 label。
  if (t === 'array') {
    return <ArrayField schema={schema} path={path} />;
  }
  // object
  if (t === 'object' && schema.properties && Object.keys(schema.properties).length > 0) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {Object.entries(schema.properties).map(([k, sub]) => {
          const label = sub?.description || humanize(k);
          // 子属性是数组时不要用 <Form.Item> 包一层：上一行 SchemaNode 已经把数组
          // 归到 ArrayField 直出 Form.List，让 ArrayField 自己在外面渲染 label 即可。
          if (sub?.type === 'array') {
            return (
              <div key={k}>
                <div style={{ marginBottom: 4 }}>{label}</div>
                <ArrayField schema={sub} path={[...path, k]} />
              </div>
            );
          }
          return (
            <Form.Item
              key={k}
              name={[...path, k]}
              label={label}
              style={{ marginBottom: 0 }}
            >
              <SchemaNode schema={sub} path={[...path, k]} />
            </Form.Item>
          );
        })}
      </div>
    );
  }
  // fallback：复杂 / 未识别类型
  return (
    <Input.TextArea
      {...ctrl}
      rows={3}
      placeholder="不支持的 schema 类型，请用 JSON 编辑"
      onChange={(e) => {
        // 兜底 JSON 模式：把字符串解析后回填给 Form
        onChange?.(safeParse(e.target.value));
      }}
    />
  );
}

/**
 * 把 Form.Item 注入的 value/onChange 转成 antd 控件期望的形状。
 * - DatePicker: 存的是 ISO 字符串，需要 dayjs 包裹；onChange 把 dayjs 折回字符串。
 * - 其它控件：value/onChange 原样转发。
 */
function ctrlPropsFor(schema, value, onChange) {
  const t = schema?.type;
  if (t === 'string' && (schema.format === 'date-time' || schema.format === 'date')) {
    return {
      value: value == null || value === '' ? null : dayjs(value),
      onChange: (d) => onChange?.(d ? d.format(schema.format === 'date-time' ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD') : null),
    };
  }
  return { value, onChange };
}

/**
 * 数组类型字段的渲染器。直接挂 <Form.List name={path}>，不再让父级把它包进
 * <Form.Item name={path}>。path 必须直接指向数组（不包含数组名）。
 *
 * items.schema：
 *   - items: { type: 'object', properties: {…} } → 每条渲染一组字段（用相对名）
 *   - 其他 / 简单类型 → 每条渲染一个原生控件
 */
function ArrayField({ schema, path }) {
  return (
    <Form.List name={path}>
      {(fields, { add, remove }) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {fields.map((field) => (
            <div
              key={field.key}
              style={{
                display: 'flex',
                gap: 8,
                alignItems: 'flex-start',
                padding: 8,
                border: '1px dashed #d9d9d9',
                borderRadius: 6,
                background: '#fafafa',
              }}
            >
              <div style={{ flex: 1 }}>
                {schema.items && schema.items.properties
                  ? Object.entries(schema.items.properties).map(([k, sub]) => (
                      <Form.Item
                        key={k}
                        name={[field.name, k]}
                        label={sub?.description || humanize(k)}
                        style={{ marginBottom: 8 }}
                      >
                        <SchemaNode schema={sub} path={[field.name, k]} />
                      </Form.Item>
                    ))
                  : (
                      <Form.Item name={field.name} style={{ marginBottom: 0 }}>
                        <SchemaNode schema={schema.items || { type: 'object' }} path={[]} />
                      </Form.Item>
                    )}
              </div>
              <Button
                type="text"
                danger
                icon={<MinusCircleOutlined />}
                onClick={() => remove(field.name)}
              />
            </div>
          ))}
          <Button
            type="dashed"
            icon={<PlusOutlined />}
            onClick={() => add(emptyValueFor(schema.items))}
            block
          >
            添加一项
          </Button>
        </div>
      )}
    </Form.List>
  );
}

function humanize(s) {
  if (!s) return '';
  // camelCase / snake_case → 单词首字母大写
  return s
    .replace(/[_-]+/g, ' ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

function emptyValueFor(schema) {
  if (!schema || !schema.type) return {};
  if (schema.type === 'object') return {};
  if (schema.type === 'array') return [];
  if (schema.type === 'string') return '';
  if (schema.type === 'number' || schema.type === 'integer') return 0;
  if (schema.type === 'boolean') return false;
  return null;
}

function safeParse(s) {
  try { return JSON.parse(s); } catch { return s; }
}
