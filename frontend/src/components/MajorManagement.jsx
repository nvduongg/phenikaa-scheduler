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
            message.error("Failed to fetch majors");
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
                message.success(`${info.file.name} imported successfully`);
                fetchMajors();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
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
            message.error("Failed to download template");
        }
    };

    // 4. Columns
    const columns = [
        {
            title: 'Major Code',
            dataIndex: 'code',
            key: 'code',
            width: 150,
            render: (text) => <Tag color="geekblue">{text}</Tag>
        },
        {
            title: 'Major Name',
            dataIndex: 'name',
            key: 'name',
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Managing Faculty',
            dataIndex: ['faculty', 'name'], // Nested object access
            key: 'faculty',
            render: (text) => <Tag color="purple">{text}</Tag>
        }
        ,{
            title: 'Actions', key: 'actions', width: 150, render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Edit</Button>
                    <Popconfirm title="Delete this major?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Delete</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => { setEditing(null); form.resetFields(); setModalVisible(true); };

    const onEdit = (record) => { setEditing(record); form.setFieldsValue({ code: record.code, name: record.name, faculty: record.faculty?.id }); setModalVisible(true); };

    const onDelete = async (id) => { try { await axiosClient.delete(`/majors/${id}`); message.success('Deleted'); fetchMajors(); } catch { message.error('Delete failed'); } };

    const onFinish = async (values) => {
        try {
            const payload = { ...values };
            if (values.faculty) payload.faculty = { id: values.faculty };
            if (editing) { await axiosClient.put(`/majors/${editing.id}`, payload); message.success('Updated'); }
            else { await axiosClient.post('/majors', payload); message.success('Created'); }
            setModalVisible(false); fetchMajors();
        } catch { message.error('Save failed'); }
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Major Management</Title>
                    <Text type="secondary">Academic Majors / Specializations</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>New</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
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

            <Modal title={editing ? 'Edit Major' : 'Create Major'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="code" label="Major Code" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="name" label="Major Name" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="faculty" label="Faculty" rules={[{ required: true }]}>
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