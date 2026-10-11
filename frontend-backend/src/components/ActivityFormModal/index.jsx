import { useEffect } from 'react';
import { Modal, Form, Input, DatePicker, Select, InputNumber } from 'antd';
import dayjs from 'dayjs';
import { STATUS_OPTIONS } from '../../api/activity.js';

/**
 * 新增 / 编辑 活动表单弹窗。
 *
 * props:
 *   open: boolean
 *   initial: Activity | null       编辑时传入
 *   onCancel: () => void
 *   onSubmit: (values) => Promise<void>
 */
export default function ActivityFormModal({ open, initial, onCancel, onSubmit }) {
  const [form] = Form.useForm();
  const isEdit = !!initial;

  useEffect(() => {
    if (open) {
      if (initial) {
        form.setFieldsValue({
          ...initial,
          timeRange: initial.startTime && initial.endTime
            ? [dayjs(initial.startTime), dayjs(initial.endTime)]
            : null,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ status: 1 });
      }
    }
  }, [open, initial, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const [start, end] = values.timeRange || [];
    const payload = {
      name: values.name,
      coverUrl: values.coverUrl || '',
      link: values.link || '',
      status: values.status,
      startTime: start ? start.format('YYYY-MM-DD HH:mm:ss') : '',
      endTime: end ? end.format('YYYY-MM-DD HH:mm:ss') : '',
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
      width={560}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="name"
          label="活动标题"
          rules={[{ required: true, message: '请输入活动标题' }, { max: 50 }]}
        >
          <Input placeholder="例如：会员夺宝" />
        </Form.Item>

        <Form.Item
          name="coverUrl"
          label="封面图 URL"
          rules={[{ type: 'url', message: '请输入合法的 URL', warningOnly: true }]}
        >
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
            options={STATUS_OPTIONS.map((o) => ({
              value: o.value,
              label: o.label,
            }))}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}