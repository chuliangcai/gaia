import { useEffect } from 'react';
import { Modal, Form, Input, Select, DatePicker } from 'antd';
import dayjs from 'dayjs';
import { LEVEL_OPTIONS, STATUS_OPTIONS } from '../../api/member.js';

/**
 * 新增 / 编辑 会员表单弹窗。
 *
 * props:
 *   open: boolean
 *   initial: Member | null          编辑时传入
 *   onCancel: () => void
 *   onSubmit: (values) => Promise<void>
 *
 * 编辑模式下 userId 字段禁用（userId 是自然键，不可改）。
 * 提交时把 form values 原样回传给父组件，由父组件决定调用 createMember / updateMember。
 */
export default function MemberFormModal({ open, initial, onCancel, onSubmit }) {
  const [form] = Form.useForm();
  const isEdit = !!initial;

  useEffect(() => {
    if (open) {
      if (initial) {
        form.setFieldsValue({
          userId: initial.userId,
          level: initial.level || 'NORMAL',
          status: initial.status ?? 1,
          joinAt: initial.joinAt ? dayjs(initial.joinAt) : null,
          expireAt: initial.expireAt ? dayjs(initial.expireAt) : null,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ level: 'NORMAL', status: 1 });
      }
    }
  }, [open, initial, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const payload = {
      userId: values.userId,
      level: values.level,
      status: values.status,
      joinAt: values.joinAt ? values.joinAt.format('YYYY-MM-DD HH:mm:ss') : null,
      expireAt: values.expireAt ? values.expireAt.format('YYYY-MM-DD HH:mm:ss') : null,
    };
    await onSubmit(payload);
  };

  return (
    <Modal
      title={isEdit ? '编辑会员' : '新增会员'}
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      okText="保存"
      cancelText="取消"
      destroyOnHidden
      width={520}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="userId"
          label="用户 ID"
          rules={[
            { required: true, message: '请输入用户 ID' },
            { type: 'number', transform: (v) => (v == null || v === '' ? undefined : Number(v)) },
          ]}
        >
          <Input
            placeholder="例如：10001"
            disabled={isEdit}
            allowClear={!isEdit}
          />
        </Form.Item>

        <Form.Item
          name="level"
          label="会员等级"
          rules={[{ required: true, message: '请选择会员等级' }]}
        >
          <Select
            options={LEVEL_OPTIONS.map((o) => ({
              value: o.value,
              label: o.label,
            }))}
          />
        </Form.Item>

        <Form.Item
          name="status"
          label="状态"
          rules={[{ required: true }]}
        >
          <Select
            options={STATUS_OPTIONS.map((o) => ({
              value: o.value,
              label: o.label,
            }))}
          />
        </Form.Item>

        {!isEdit && (
          <Form.Item name="joinAt" label="加入时间">
            <DatePicker
              showTime={{ format: 'HH:mm' }}
              format="YYYY-MM-DD HH:mm"
              style={{ width: '100%' }}
              placeholder="缺省取当前时间"
            />
          </Form.Item>
        )}

        <Form.Item name="expireAt" label="到期时间">
          <DatePicker
            showTime={{ format: 'HH:mm' }}
            format="YYYY-MM-DD HH:mm"
            style={{ width: '100%' }}
            placeholder="永久有效请留空"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
