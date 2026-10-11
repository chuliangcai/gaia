import { Form, Input, InputNumber, Select, Switch, DatePicker, Button } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

/**
 * 自研 JsonSchema → antd 控件递归渲染器。
 *
 * 接收父级 antd Form 的一个 name 路径（通常是 `['abilityConfigs', '<configClassFqn>']`），
 * 内部用 `<Form.List name={path}>` 或 `<Form.Item name={[...path, k]}>` 嵌套，
 * 由 antd 自动管理 values 与校验；不需要 value/onChange prop。
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

function SchemaNode({ schema, path }) {
  const t = schema?.type;

  // string + enum
  if (t === 'string' && Array.isArray(schema.enum)) {
    return (
      <Select
        allowClear
        options={schema.enum.map((v) => ({ value: v, label: v }))}
        style={{ width: '100%' }}
      />
    );
  }
  // string + date-time
  if (t === 'string' && schema.format === 'date-time') {
    return (
      <DatePicker
        showTime={{ format: 'HH:mm:ss' }}
        format="YYYY-MM-DD HH:mm:ss"
        style={{ width: '100%' }}
        onChange={(d) => d ? d.format('YYYY-MM-DD HH:mm:ss') : null}
      />
    );
  }
  // string + date
  if (t === 'string' && schema.format === 'date') {
    return (
      <DatePicker
        format="YYYY-MM-DD"
        style={{ width: '100%' }}
        onChange={(d) => d ? d.format('YYYY-MM-DD') : null}
      />
    );
  }
  // string
  if (t === 'string') return <Input />;
  // integer / number
  if (t === 'integer' || t === 'number') {
    return <InputNumber style={{ width: '100%' }} />;
  }
  // boolean
  if (t === 'boolean') return <Switch />;
  // object
  if (t === 'object' && schema.properties && Object.keys(schema.properties).length > 0) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {Object.entries(schema.properties).map(([k, sub]) => (
          <Form.Item
            key={k}
            name={[...path, k]}
            label={sub?.description || humanize(k)}
            style={{ marginBottom: 0 }}
          >
            <SchemaNode schema={sub} path={[...path, k]} />
          </Form.Item>
        ))}
      </div>
    );
  }
  // array
  if (t === 'array') {
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
  // fallback：复杂 / 未识别类型
  return (
    <Input.TextArea
      rows={3}
      placeholder="不支持的 schema 类型，请用 JSON 编辑"
      onChange={(e) => safeParse(e.target.value)}
    />
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
