import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Typography,
  Button,
  Space,
  message,
  Modal,
  Form,
  Input,
  Select,
  Upload,
  Popconfirm,
} from 'antd';
import { ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined, UploadOutlined, DownloadOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const ROLE_OPTIONS = [
  { value: 'ADMIN', label: 'Quản trị Đại học' },
  { value: 'ADMIN_SCHOOL', label: 'Quản trị Trường' },
  { value: 'ADMIN_FACULTY', label: 'Giáo vụ Khoa' },
];

const roleColor = (role) => {
  if (role === 'ADMIN') return 'red';
  if (role === 'ADMIN_SCHOOL') return 'blue';
  if (role === 'ADMIN_FACULTY') return 'green';
  return 'default';
};

const roleLabel = (role) => {
  const match = ROLE_OPTIONS.find((r) => r.value === role);
  return match ? match.label : role;
};

const UserManagement = ({ user }) => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [schools, setSchools] = useState([]);
  const [faculties, setFaculties] = useState([]);
  const [form] = Form.useForm();

  const currentRole = user?.role;
  const isAdmin = currentRole === 'ADMIN';
  const isAdminSchool = currentRole === 'ADMIN_SCHOOL';
  const isAdminFaculty = currentRole === 'ADMIN_FACULTY';

  const allowedRoleOptions = useMemo(() => {
    if (isAdmin) return ROLE_OPTIONS;
    if (isAdminSchool) return ROLE_OPTIONS.filter((r) => r.value !== 'ADMIN');
    return ROLE_OPTIONS.filter((r) => r.value === 'ADMIN_FACULTY');
  }, [isAdmin, isAdminSchool]);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await axiosClient.get('/users');
      setUsers(res.data || []);
    } catch {
      message.error('Không thể tải danh sách người dùng');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchSchools = useCallback(async () => {
    try {
      const res = await axiosClient.get('/schools');
      setSchools(res.data || []);
    } catch {
      setSchools([]);
    }
  }, []);

  const fetchFaculties = useCallback(async () => {
    try {
      if (isAdminFaculty && user?.facultyId) {
        const res = await axiosClient.get(`/faculties/${user.facultyId}`);
        setFaculties(res.data ? [res.data] : []);
        return;
      }
      const res = await axiosClient.get('/faculties');
      setFaculties(res.data || []);
    } catch {
      setFaculties([]);
    }
  }, [isAdminFaculty, user?.facultyId]);

  useEffect(() => {
    fetchUsers();
    fetchSchools();
    fetchFaculties();
  }, [fetchUsers, fetchSchools, fetchFaculties]);

  const openCreate = () => {
    setEditing(null);
    const defaultRole = isAdmin ? 'ADMIN_FACULTY' : (isAdminSchool ? 'ADMIN_FACULTY' : 'ADMIN_FACULTY');
    form.setFieldsValue({
      username: '',
      password: '',
      fullName: '',
      role: defaultRole,
      schoolId: schools.length === 1 ? schools[0].id : undefined,
      facultyId: isAdminFaculty ? user?.facultyId : undefined,
    });
    setModalVisible(true);
  };

  const openEdit = (record) => {
    setEditing(record);
    form.setFieldsValue({
      username: record.username,
      password: '',
      fullName: record.fullName,
      role: record.role,
      schoolId: record.schoolId,
      facultyId: record.facultyId,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      await axiosClient.delete(`/users/${id}`);
      message.success('Đã xóa người dùng');
      fetchUsers();
    } catch {
      message.error('Xóa thất bại');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);

      const payload = {
        username: values.username,
        password: values.password || undefined,
        fullName: values.fullName || undefined,
        role: values.role,
        schoolId: values.schoolId || undefined,
        facultyId: values.facultyId || undefined,
      };

      if (editing) {
        // username immutable
        delete payload.username;
        if (!payload.password) delete payload.password;
        await axiosClient.put(`/users/${editing.id}`, payload);
        message.success('Đã cập nhật người dùng');
      } else {
        await axiosClient.post('/users', payload);
        message.success('Đã tạo người dùng');
      }

      setModalVisible(false);
      setEditing(null);
      fetchUsers();
    } catch {
      // validateFields throws
    } finally {
      setSaving(false);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      const response = await axiosClient.get('/users/template', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'User_Import_Template.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch {
      message.error('Không thể tải file mẫu');
    }
  };

  const uploadProps = {
    name: 'file',
    action: 'http://localhost:8080/api/v1/users/import',
    headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
    showUploadList: false,
    onChange(info) {
      if (info.file.status === 'done') {
        message.success(`Đã nhập ${info.file.name} thành công`);
        fetchUsers();
      } else if (info.file.status === 'error') {
        message.error(`Nhập ${info.file.name} thất bại`);
      }
    },
  };

  const columns = [
    {
      title: 'Tài khoản',
      dataIndex: 'username',
      key: 'username',
      sorter: (a, b) => (a.username || '').localeCompare(b.username || ''),
      render: (t) => <Text strong>{t}</Text>,
    },
    {
      title: 'Họ tên',
      dataIndex: 'fullName',
      key: 'fullName',
      sorter: (a, b) => (a.fullName || '').localeCompare(b.fullName || ''),
      render: (t) => (t ? t : <Text type="secondary">(Chưa có)</Text>),
    },
    {
      title: 'Khoa/Viện',
      key: 'faculty',
      render: (_, r) => (r.facultyName ? <Tag color="purple">{r.facultyName}</Tag> : <Text type="secondary">(Không)</Text>),
    },
    {
      title: 'Trường',
      key: 'school',
      render: (_, r) => (r.schoolName ? <Tag color="geekblue">{r.schoolName}</Tag> : <Text type="secondary">(Không)</Text>),
    },
    {
      title: 'Quyền',
      key: 'role',
      width: 180,
      render: (_, r) => <Tag color={roleColor(r.role)}>{roleLabel(r.role)}</Tag>,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 160,
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
            Sửa
          </Button>
          <Popconfirm title="Xóa người dùng này?" onConfirm={() => handleDelete(record.id)}>
            <Button danger size="small" icon={<DeleteOutlined />}>
              Xóa
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card
      bordered={false}
      title={<Title level={4} style={{ margin: 0 }}>Quản lý người dùng</Title>}
      extra={
        <Space>
          <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
            File mẫu
          </Button>
          <Upload {...uploadProps}>
            <Button icon={<UploadOutlined />}>
              Import
            </Button>
          </Upload>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Thêm
          </Button>
          <Button icon={<ReloadOutlined />} onClick={fetchUsers} loading={loading}>
            Tải lại
          </Button>
        </Space>
      }
    >
      <Table
        rowKey="id"
        loading={loading}
        dataSource={users}
        columns={columns}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editing ? 'Cập nhật người dùng' : 'Thêm người dùng'}
        open={modalVisible}
        onCancel={() => { setModalVisible(false); setEditing(null); }}
        onOk={handleSubmit}
        okText={editing ? 'Lưu' : 'Tạo'}
        confirmLoading={saving}
      >
        <Form layout="vertical" form={form}>
          <Form.Item
            label="Tài khoản"
            name="username"
            rules={[{ required: true, message: 'Vui lòng nhập tài khoản' }]}
          >
            <Input disabled={!!editing} />
          </Form.Item>

          <Form.Item
            label={editing ? 'Mật khẩu mới (để trống nếu không đổi)' : 'Mật khẩu'}
            name="password"
            rules={editing ? [] : [{ required: true, message: 'Vui lòng nhập mật khẩu' }]}
          >
            <Input.Password />
          </Form.Item>

          <Form.Item label="Họ tên" name="fullName">
            <Input />
          </Form.Item>

          <Form.Item
            label="Quyền"
            name="role"
            rules={[{ required: true, message: 'Vui lòng chọn quyền' }]}
          >
            <Select
              options={allowedRoleOptions}
              disabled={isAdminFaculty || (editing && editing.id === user?.id)}
            />
          </Form.Item>

          <Form.Item shouldUpdate>
            {() => {
              const role = form.getFieldValue('role');

              if (role === 'ADMIN_SCHOOL') {
                return (
                  <Form.Item
                    label="Trường"
                    name="schoolId"
                    rules={[{ required: true, message: 'Vui lòng chọn trường' }]}
                  >
                    <Select
                      options={(schools || []).map((s) => ({ value: s.id, label: s.name }))}
                      disabled={isAdminSchool}
                      placeholder="Chọn trường"
                    />
                  </Form.Item>
                );
              }

              if (role === 'ADMIN_FACULTY') {
                return (
                  <Form.Item
                    label="Khoa/Viện"
                    name="facultyId"
                    rules={[{ required: true, message: 'Vui lòng chọn khoa/viện' }]}
                  >
                    <Select
                      options={(faculties || []).map((f) => ({ value: f.id, label: f.name }))}
                      disabled={isAdminFaculty}
                      placeholder="Chọn khoa/viện"
                    />
                  </Form.Item>
                );
              }

              return null;
            }}
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default UserManagement;
