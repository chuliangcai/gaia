import { useEffect, useState } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Popconfirm,
  Card,
  message,
  Image,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  listActivities,
  createActivity,
  updateActivity,
  deleteActivity,
  statusToOption,
} from '../../api/activity.js';
import ActivityFormModal from '../../components/ActivityFormModal/index.jsx';

export default function ActivityList() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [msgApi, ctx] = message.useMessage();

  const reload = async () => {
    setLoading(true);
    try {
      const list = await listActivities();
      setData(list);
    } catch (e) {
      msgApi.error(e.message || '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    reload();
  }, []);

  const handleCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };

  const handleEdit = (record) => {
    setEditing(record);
    setModalOpen(true);
  };

  const handleDelete = async (id) => {
    try {
      await deleteActivity(id);
      msgApi.success('已删除');
      reload();
    } catch (e) {
      msgApi.error(e.message || '删除失败');
    }
  };

  const handleSubmit = async (values) => {
    try {
      if (editing) {
        await updateActivity(editing.id, values);
        msgApi.success('已更新');
      } else {
        await createActivity(values);
        msgApi.success('已创建');
      }
      setModalOpen(false);
      reload();
    } catch (e) {
      msgApi.error(e.message || '保存失败');
    }
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
    },
    {
      title: '封面',
      dataIndex: 'coverUrl',
      width: 80,
      render: (url) =>
        url ? (
          <Image src={url} width={48} height={48} style={{ objectFit: 'cover', borderRadius: 6 }} />
        ) : (
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: 6,
              background: 'linear-gradient(135deg, #ff8fa3 0%, #ff4d6d 100%)',
            }}
          />
        ),
    },
    {
      title: '活动标题',
      dataIndex: 'name',
      ellipsis: true,
    },
    {
      title: '跳转链接',
      dataIndex: 'link',
      ellipsis: true,
      render: (v) => (v ? <code style={{ fontSize: 12 }}>{v}</code> : <span style={{ color: '#bbb' }}>—</span>),
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      width: 170,
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      width: 170,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status) => {
        const o = statusToOption(status);
        return <Tag color={o.color}>{o.label}</Tag>;
      },
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
            title="确认删除该活动？"
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleDelete(record.id)}
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
        bordered={false}
        title="活动列表"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={reload}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              新建活动
            </Button>
          </Space>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          pagination={{ pageSize: 10, showSizeChanger: true }}
          scroll={{ x: 1000 }}
        />
      </Card>

      <ActivityFormModal
        open={modalOpen}
        initial={editing}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </>
  );
}