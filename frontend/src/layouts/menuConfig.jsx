import React from 'react';
import {
  BankOutlined,
  BookOutlined,
  CalendarOutlined,
  PieChartOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons';

export const MENU_CONFIG = [
  {
    key: 'grp_org',
    label: 'Tổ chức',
    icon: <BankOutlined />,
    roles: ['ADMIN', 'ADMIN_SCHOOL'],
    children: [
      { key: '0', label: 'Trường thành viên', path: '/v1/schools' },
      { key: '1', label: 'Khoa/Viện', path: '/v1/faculties' },
      { key: '5', label: 'Ngành/Chuyên ngành', path: '/v1/majors' },
      { key: '6', label: 'Khóa sinh viên', path: '/v1/cohorts' },
      { key: '7', label: 'Lớp hành chính', path: '/v1/admin-classes' },
    ],
  },
  {
    key: 'grp_res',
    label: 'Nguồn lực',
    icon: <UserOutlined />,
    roles: ['ADMIN', 'ADMIN_SCHOOL', 'ADMIN_FACULTY'],
    children: [
      { key: '10', label: 'Giảng viên', path: '/v1/lecturers' },
      { key: '11', label: 'Chuyên môn', path: '/v1/expertise' },
      { key: '12', label: 'Phòng học', path: '/v1/rooms', roles: ['ADMIN'] },
    ],
  },
  {
    key: 'grp_curr',
    label: 'CTĐT & Kế hoạch',
    icon: <BookOutlined />,
    roles: ['ADMIN', 'ADMIN_SCHOOL', 'ADMIN_FACULTY'],
    children: [
      { key: '8', label: 'Khung CTĐT', path: '/v1/curricula', roles: ['ADMIN', 'ADMIN_SCHOOL'] },
      { key: '2', label: 'Học phần', path: '/v1/courses', roles: ['ADMIN', 'ADMIN_SCHOOL', 'ADMIN_FACULTY'] },
      { key: '3', label: 'Mở lớp học phần', path: '/v1/offerings', roles: ['ADMIN', 'ADMIN_SCHOOL', 'ADMIN_FACULTY'] },
    ],
  },
  {
    key: '4',
    label: 'Xếp thời khóa biểu',
    icon: <CalendarOutlined />,
    roles: ['ADMIN'],
    path: '/v1/timetable',
  },
  {
    key: 'grp_sys',
    label: 'Cài đặt hệ thống',
    icon: <SettingOutlined />,
    roles: ['ADMIN'],
    children: [{ key: '99', label: 'Thiết lập học kỳ', path: '/v1/semester-settings' }],
  },
  {
    key: 'grp_stat',
    label: 'Báo cáo & Thống kê',
    icon: <PieChartOutlined />,
    roles: ['ADMIN', 'ADMIN_SCHOOL'],
    children: [{ key: '20', label: 'Tải giảng dạy', path: '/v1/reports/lecturer-workload' }],
  },
];

export const filterMenuByRole = (items, role) => {
  return items
    .filter((item) => !item.roles || item.roles.includes(role))
    .map((item) => {
      if (item.children) {
        return { ...item, children: filterMenuByRole(item.children, role) };
      }
      return item;
    })
    .filter((item) => {
      if (item.children && item.children.length === 0) return false;
      return true;
    });
};

export const flattenMenuLeaves = (items) => {
  const leaves = [];
  const walk = (nodes) => {
    for (const node of nodes) {
      if (node.children) {
        walk(node.children);
        continue;
      }
      if (node.path) {
        leaves.push({ key: node.key, path: node.path });
      }
    }
  };
  walk(items);
  return leaves;
};

export const findMenuKeyByPath = (pathname, items) => {
  const leaves = flattenMenuLeaves(items);
  const match = leaves.find((l) => l.path === pathname);
  return match ? match.key : null;
};

export const findMenuPathByKey = (key, items) => {
  const leaves = flattenMenuLeaves(items);
  const match = leaves.find((l) => l.key === key);
  return match ? match.path : null;
};

export const getDefaultMenuKeyForRole = (role) => {
  if (role === 'ADMIN_FACULTY') return '3';
  return '0';
};
