import { useEffect, useState } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Popconfirm,
  Card,
  message,
  Select,
  Form,
  Row,
  Col,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import {
  listMembers,
  createMember,
  updateMember,
  deleteMember,
  levelToOption,
  statusToOption,
  LEVEL_OPTIONS,
  STATUS_OPTIONS,
} from '../../api/member.js';
import MemberFormModal from '../../components/MemberFormModal/index.jsx';

export default function MemberList() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [msgApi, ctx] = message.useMessage();
  const [filterForm] = Form.useForm();

  // 初始过滤项：status=1（仅有效）
  const [filters, setFilters] = useState({ status: 1, level: undefined });

  const reload = async (override) => {
    const next = { ...filters, ...(override || {}) };
    setFilters(next);
    setLoading(true);
    try {
      // 过滤掉空值，避免后端拿到 status=undefined / level=''
      const params = {};
      if (next.status != null) params.status = next.status;
      if (next.level) params.level = next.level;
      const list = await listMembers(params);
      setData(list);
    } catch (e) {
      msgApi.error(e.message || '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    reload({ status: 1 });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };

  const handleEdit = (record) => {
    setEditing(record);
    setModalOpen(true);
  };

  const handleDelete = async (userId) => {
    try {
      await deleteMember(userId);
      msgApi.success('已删除');
      reload();
    } catch (e) {
      msgApi.error(e.message || '删除失败');
    }
  };

  const handleSubmit = async (values) => {
    try {
      if (editing) {
        // 更新：仅传 level / status / expireAt（与后端 MemberUpdateRequest 一致）
        const updatePayload = {
          level: values.level,
          status: values.status,
          expireAt: values.expireAt,
        };
        await updateMember(editing.userId, updatePayload);
        msgApi.success('已更新');
      } else {
        // 新增：传完整字段
        await createMember({
          userId: Number(values.userId),
          level: values.level,
          status: values.status,
          joinAt: values.joinAt,
          expireAt: values.expireAt,
        });
        msgApi.success('已创建');
      }
      setModalOpen(false);
      reload();
    } catch (e) {
      msgApi.error(e.message || '保存失败');
    }
  };

  const handleFilterSubmit = async () => {
    const v = await filterForm.validateFields();
    reload({
      status: v.status,
      level: v.level,
    });
  };

  const handleFilterReset = () => {
    filterForm.resetFields();
    filterForm.setFieldsValue({ status: undefined, level: undefined });
    reload({ status: undefined, level: undefined });
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
    },
    {
      title: '用户 ID',
      dataIndex: 'userId',
      width: 110,
      render: (v) => <code>{v}</code>,
    },
    {
      title: '等级',
      dataIndex: 'level',
      width: 100,
      render: (level) => {
        const o = levelToOption(level);
        return <Tag color={o.color}>{o.label}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status) => {
        const o = statusToOption(status);
        return <Tag color={o.color}>{o.label}</Tag>;
      },
    },
    {
      title: '加入时间',
      dataIndex: 'joinAt',
      width: 170,
      render: (v) => (v ? <span style={{ fontSize: 12 }}>{v}</span> : <span style={{ color: '#bbb' }}>—</span>),
    },
    {
      title: '到期时间',
      dataIndex: 'expireAt',
      width: 170,
      render: (v) => (v ? <span style={{ fontSize: 12 }}>{v}</span> : <span style={{ color: '#bbb' }}>永久</span>),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 170,
      render: (v) => (v ? <span style={{ fontSize: 12, color: '#999' }}>{v}</span> : <span style={{ color: '#bbb' }}>—</span>),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title={`确认删除会员 userId=${record.userId}？`}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleDelete(record.userId)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <>
      {ctx}
      <Card
        variant="borderless"
        title="会员列表"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => reload()}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              新增会员
            </Button>
          </Space>
        }
      >
        <Form
          form={filterForm}
          layout="inline"
          onFinish={handleFilterSubmit}
          initialValues={{ status: 1 }}
          style={{ marginBottom: 16 }}
        >
          <Row gutter={8} wrap={false}>
            <Col>
              <Form.Item name="status" label="状态">
                <Select
                  allowClear
                  placeholder="全部"
                  style={{ width: 140 }}
                  options={STATUS_OPTIONS.map((o) => ({
                    value: o.value,
                    label: o.label,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="level" label="等级">
                <Select
                  allowClear
                  placeholder="全部"
                  style={{ width: 140 }}
                  options={LEVEL_OPTIONS.map((o) => ({
                    value: o.value,
                    label: o.label,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col>
              <Space>
                <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                  查询
                </Button>
                <Button onClick={handleFilterReset}>重置</Button>
              </Space>
            </Col>
          </Row>
        </Form>

        <Table
          rowKey="userId"
          columns={columns}
          dataSource={data}
          loading={loading}
          pagination={{ pageSize: 10, showSizeChanger: true }}
          scroll={{ x: 1100 }}
        />
      </Card>

      <MemberFormModal
        open={modalOpen}
        initial={editing}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </>
  );
}
