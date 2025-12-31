import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Modal, Form, Input, Select, InputNumber, Popconfirm } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const AdminClassManagement = () => {
    const [classes, setClasses] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();
    const [majors, setMajors] = useState([]);
    const [cohorts, setCohorts] = useState([]);

    // Fetch
    const fetchClasses = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/admin-classes');
            setClasses(res.data);
        } catch {
            message.error("Không thể tải danh sách lớp hành chính");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchClasses();
        fetchMajors();
        fetchCohorts();
    }, []);

    const fetchMajors = async () => { try { const res = await axiosClient.get('/majors'); setMajors(res.data); } catch {} };
    const fetchCohorts = async () => { try { const res = await axiosClient.get('/cohorts'); setCohorts(res.data); } catch {} };

    // Upload
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/admin-classes/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`Đã nhập ${info.file.name} thành công`);
                fetchClasses();
            } else if (info.file.status === 'error') {
                message.error(`Nhập ${info.file.name} thất bại`);
            }
        },
    };

    // Template
    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/admin-classes/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Admin_Class_Import_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error("Không thể tải file mẫu");
        }
    };

    // Columns
    const columns = [
        {
            title: 'Tên lớp',
            dataIndex: 'name',
            key: 'name',
            sorter: (a, b) => (a.name || '').localeCompare(b.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Ngành',
            dataIndex: ['major', 'name'],
            key: 'major',
            sorter: (a, b) => (a.major?.name || '').localeCompare(b.major?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="geekblue">{text}</Tag>
        },
        {
            title: 'Khóa',
            dataIndex: ['cohort', 'name'],
            key: 'cohort',
            align: 'center',
            width: 100,
            sorter: (a, b) => (a.cohort?.name || '').localeCompare(b.cohort?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="orange">{text}</Tag>
        },
        {
            title: 'Sĩ số',
            dataIndex: 'size',
            key: 'size',
            align: 'center',
            width: 100,
            sorter: (a, b) => (a.size || 0) - (b.size || 0),
            sortDirections: ['ascend', 'descend'],
            render: (size) => <Tag color="default">{size}</Tag>
        }
        ,{
            title: 'Thao tác', key: 'actions', width: 180, render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Sửa</Button>
                    <Popconfirm title="Xóa lớp này?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Xóa</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => { setEditing(null); form.resetFields(); setModalVisible(true); };

    const onEdit = (record) => { setEditing(record); form.setFieldsValue({ name: record.name, code: record.code, major: record.major?.id, cohort: record.cohort?.id, size: record.size }); setModalVisible(true); };

    const onDelete = async (id) => { try { await axiosClient.delete(`/admin-classes/${id}`); message.success('Đã xóa'); fetchClasses(); } catch { message.error('Xóa thất bại'); } };

    const onFinish = async (values) => {
        try {
            const payload = { ...values };
            if (values.major) payload.major = { id: values.major };
            if (values.cohort) payload.cohort = { id: values.cohort };
            if (editing) { await axiosClient.put(`/admin-classes/${editing.id}`, payload); message.success('Đã cập nhật'); }
            else { await axiosClient.post('/admin-classes', payload); message.success('Đã tạo'); }
            setModalVisible(false); fetchClasses();
        } catch { message.error('Lưu thất bại'); }
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Quản lý lớp hành chính</Title>
                    <Text type="secondary">Lớp sinh viên (lớp biên chế / nhóm khóa)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Mẫu
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm mới</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Nhập Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchClasses} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={classes} 
                    loading={loading}
                    pagination={{ pageSize: 8 }}
                />
            </Card>

            <Modal title={editing ? 'Sửa lớp' : 'Tạo lớp'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="name" label="Tên lớp" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="code" label="Mã lớp">
                        <Input />
                    </Form.Item>
                    <Form.Item name="major" label="Ngành" rules={[{ required: true }]}>
                        <Select>
                            {majors.map(m => <Select.Option key={m.id} value={m.id}>{m.name}</Select.Option>)}
                        </Select>
                    </Form.Item>
                    <Form.Item name="cohort" label="Khóa" rules={[{ required: true }]}>
                        <Select>
                            {cohorts.map(c => <Select.Option key={c.id} value={c.id}>{c.name}</Select.Option>)}
                        </Select>
                    </Form.Item>
                    <Form.Item name="size" label="Sĩ số">
                        <InputNumber style={{ width: '100%' }} />
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
};

export default AdminClassManagement;