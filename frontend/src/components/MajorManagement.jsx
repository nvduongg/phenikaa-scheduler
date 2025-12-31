import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Modal, Form, Input, Select, Popconfirm } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const MajorManagement = () => {
    const [majors, setMajors] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();
    const [faculties, setFaculties] = useState([]);

    // 1. Fetch Majors
    const fetchMajors = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/majors');
            setMajors(res.data);
        } catch {
            message.error("Không thể tải danh sách ngành/chuyên ngành");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchMajors();
        fetchFaculties();
    }, []);

    const fetchFaculties = async () => {
        try { const res = await axiosClient.get('/faculties'); setFaculties(res.data); } catch { /* empty */ };
    };

    // 2. Upload Config
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/majors/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`Đã nhập ${info.file.name} thành công`);
                fetchMajors();
            } else if (info.file.status === 'error') {
                message.error(`Nhập ${info.file.name} thất bại`);
            }
        },
    };

    // 3. Download Template
    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/majors/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Major_Import_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error("Không thể tải file mẫu");
        }
    };

    // 4. Columns
    const columns = [
        {
            title: 'Mã ngành',
            dataIndex: 'code',
            key: 'code',
            width: 150,
            sorter: (a, b) => (a.code || '').localeCompare(b.code || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="geekblue">{text}</Tag>
        },
        {
            title: 'Tên ngành',
            dataIndex: 'name',
            key: 'name',
            sorter: (a, b) => (a.name || '').localeCompare(b.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Khoa quản lý',
            dataIndex: ['faculty', 'name'], // Nested object access
            key: 'faculty',
            sorter: (a, b) => (a.faculty?.name || '').localeCompare(b.faculty?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="purple">{text}</Tag>
        }
        ,{
            title: 'Thao tác', key: 'actions', width: 150, render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Sửa</Button>
                    <Popconfirm title="Xóa ngành này?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Xóa</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => { setEditing(null); form.resetFields(); setModalVisible(true); };

    const onEdit = (record) => { setEditing(record); form.setFieldsValue({ code: record.code, name: record.name, faculty: record.faculty?.id }); setModalVisible(true); };

    const onDelete = async (id) => { try { await axiosClient.delete(`/majors/${id}`); message.success('Đã xóa'); fetchMajors(); } catch { message.error('Xóa thất bại'); } };

    const onFinish = async (values) => {
        try {
            const payload = { ...values };
            if (values.faculty) payload.faculty = { id: values.faculty };
            if (editing) { await axiosClient.put(`/majors/${editing.id}`, payload); message.success('Đã cập nhật'); }
            else { await axiosClient.post('/majors', payload); message.success('Đã tạo'); }
            setModalVisible(false); fetchMajors();
        } catch { message.error('Lưu thất bại'); }
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Quản lý ngành/chuyên ngành</Title>
                    <Text type="secondary">Danh mục ngành/chuyên ngành đào tạo</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Mẫu
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm mới</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Nhập Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchMajors} />
                </Space>
            </div>

            {/* Table */}
            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={majors} 
                    loading={loading}
                    pagination={{ pageSize: 8 }}
                />
            </Card>

            <Modal title={editing ? 'Sửa ngành' : 'Tạo ngành'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="code" label="Mã ngành" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="name" label="Tên ngành" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="faculty" label="Khoa/viện" rules={[{ required: true }]}>
                        <Select>
                            {faculties.map(f => <Select.Option key={f.id} value={f.id}>{f.name}</Select.Option>)}
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
};

export default MajorManagement;