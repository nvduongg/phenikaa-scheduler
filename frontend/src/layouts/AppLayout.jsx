import React, { useMemo } from 'react';
import { Layout, Menu, Dropdown, Space, Typography, theme } from 'antd';
import { LogoutOutlined, UserOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';

import logo from '../assets/Logo.png';

const { Header, Content, Footer } = Layout;
const { Text } = Typography;

const toAntdMenuItems = (items) => {
  return items.map((item) => {
    if (item.children) {
      return {
        key: item.key,
        label: item.label,
        icon: item.icon,
        children: toAntdMenuItems(item.children),
      };
    }

    return {
      key: item.key,
      label: item.path ? <Link to={item.path}>{item.label}</Link> : item.label,
      icon: item.icon,
    };
  });
};

const AppLayout = ({
  user,
  contentWidth = '2000px',
  menuItems,
  selectedKey,
  onMenuClick,
  children,
  onLogout,
}) => {
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  const antdMenuItems = useMemo(() => toAntdMenuItems(menuItems || []), [menuItems]);

  const userMenu = useMemo(
    () => ({
      items: [
        { key: 'profile', label: 'Hồ sơ của tôi', icon: <UserOutlined /> },
        { key: 'logout', label: 'Đăng xuất', icon: <LogoutOutlined />, danger: true },
      ],
      onClick: (e) => {
        if (e.key === 'logout') onLogout?.();
      },
    }),
    [onLogout]
  );

  return (
    <Layout style={{ minHeight: '100vh', background: '#f0f2f5' }}>
      <Header
        style={{
          height: '40px',
          lineHeight: '40px',
          background: '#003a70',
          padding: 0,
          color: 'white',
          zIndex: 2,
        }}
      >
        <div
          style={{
            maxWidth: contentWidth,
            margin: '0 auto',
            padding: '0 20px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            height: '100%',
          }}
        >
          <Text style={{ color: 'white', fontSize: '12px', fontWeight: 'bold', letterSpacing: '0.5px' }}>
            HỆ THỐNG XẾP THỜI KHÓA BIỂU
          </Text>
          <Space size="middle">
            <Dropdown menu={userMenu} placement="bottomRight">
              <a onClick={(e) => e.preventDefault()} style={{ cursor: 'pointer' }}>
                <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: '12px', fontWeight: 'bold' }}>
                  Xin chào, {user?.fullName || user?.username} ({user?.role})
                </Text>
              </a>
            </Dropdown>
          </Space>
        </div>
      </Header>

      <Header
        style={{
          background: colorBgContainer,
          padding: 0,
          boxShadow: '0 2px 8px #f0f1f2',
          height: '64px',
          zIndex: 1,
        }}
      >
        <div
          style={{
            maxWidth: contentWidth,
            margin: '0 auto',
            padding: '0 20px',
            display: 'flex',
            alignItems: 'center',
            height: '100%',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', minWidth: '250px' }}>
            <img src={logo} alt="Phenikaa University" style={{ height: '48px' }} />
          </div>
          <div style={{ flex: 1, display: 'flex', justifyContent: 'flex-end', marginRight: '-16px' }}>
            <Menu
              mode="horizontal"
              selectedKeys={selectedKey ? [selectedKey] : []}
              items={antdMenuItems}
              onClick={onMenuClick}
              style={{
                borderBottom: 'none',
                width: '100%',
                justifyContent: 'flex-end',
                fontSize: '14px',
                fontWeight: 500,
              }}
            />
          </div>
        </div>
      </Header>

      <Content style={{ padding: '24px 0' }}>
        <div style={{ maxWidth: contentWidth, margin: '0 auto', padding: '0 20px' }}>
          <div
            style={{
              padding: 24,
              minHeight: 500,
              background: colorBgContainer,
              borderRadius: borderRadiusLG,
              boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)',
            }}
          >
            {children}
          </div>
        </div>
      </Content>

      <Footer style={{ textAlign: 'center', background: 'transparent', color: '#888' }}>Hệ thống xếp lịch Phenikaa ©2025</Footer>
    </Layout>
  );
};

export default AppLayout;
