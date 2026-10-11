import { useState } from 'react';
import { Layout, Menu, Typography } from 'antd';
import { GiftOutlined, UserOutlined } from '@ant-design/icons';
import ActivityList from './pages/ActivityList/index.jsx';
import MemberList from './pages/MemberList/index.jsx';

const { Header, Sider, Content } = Layout;

const MENU_ITEMS = [
  { key: 'activities', icon: <GiftOutlined />, label: '活动管理' },
  { key: 'members', icon: <UserOutlined />, label: '会员管理' },
];

const HEADER_TITLE = {
  activities: '活动管理',
  members: '会员管理',
};

export default function App() {
  const [activeKey, setActiveKey] = useState('activities');

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={220} theme="light" style={{ borderRight: '1px solid #f0f0f0' }}>
        <div
          style={{
            height: 56,
            margin: 16,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'linear-gradient(135deg, #ff8fa3 0%, #ff4d6d 100%)',
            color: '#fff',
            fontWeight: 700,
            fontSize: 16,
            borderRadius: 10,
          }}
        >
          Gaia 管理后台
        </div>
        <Menu
          mode="inline"
          selectedKeys={[activeKey]}
          onClick={({ key }) => setActiveKey(key)}
          items={MENU_ITEMS}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            borderBottom: '1px solid #f0f0f0',
            display: 'flex',
            alignItems: 'center',
          }}
        >
          <Typography.Title level={4} style={{ margin: 0 }}>
            {HEADER_TITLE[activeKey] || '管理后台'}
          </Typography.Title>
        </Header>
        <Content style={{ padding: 24, background: '#f5f5f5' }}>
          {activeKey === 'members' ? <MemberList /> : <ActivityList />}
        </Content>
      </Layout>
    </Layout>
  );
}
