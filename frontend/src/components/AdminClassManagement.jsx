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
            message.error("Failed to fetch classes");
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
                message.success(`${info.file.name} imported successfully`);
                fetchClasses();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
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
            message.error("Failed to download template");
        }
    };

    // Columns
    const columns = [
        {
            title: 'Class Name',
            dataIndex: 'name',
            key: 'name',
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Major',
            dataIndex: ['major', 'name'],
            key: 'major',
            render: (text) => <Tag color="geekblue">{text}</Tag>
        },
        {
            title: 'Cohort',
            dataIndex: ['cohort', 'name'],
            key: 'cohort',
            align: 'center',
            width: 100,
            render: (text) => <Tag color="orange">{text}</Tag>
        },
        {
            title: 'Size',
            dataIndex: 'size',
            key: 'size',
            align: 'center',
            width: 100,
            render: (size) => <Tag color="default">{size}</Tag>
        }
        ,{
            title: 'Actions', key: 'actions', width: 180, render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Edit</Button>
                    <Popconfirm title="Delete this class?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Delete</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => { setEditing(null); form.resetFields(); setModalVisible(true); };

    const onEdit = (record) => { setEditing(record); form.setFieldsValue({ name: record.name, code: record.code, major: record.major?.id, cohort: record.cohort?.id, size: record.size }); setModalVisible(true); };

    const onDelete = async (id) => { try { await axiosClient.delete(`/admin-classes/${id}`); message.success('Deleted'); fetchClasses(); } catch { message.error('Delete failed'); } };

    const onFinish = async (values) => {
        try {
            const payload = { ...values };
            if (values.major) payload.major = { id: values.major };
            if (values.cohort) payload.cohort = { id: values.cohort };
            if (editing) { await axiosClient.put(`/admin-classes/${editing.id}`, payload); message.success('Updated'); }
            else { await axiosClient.post('/admin-classes', payload); message.success('Created'); }
            setModalVisible(false); fetchClasses();
        } catch { message.error('Save failed'); }
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Administrative Classes</Title>
                    <Text type="secondary">Student Classes (Lớp biên chế / Nhóm KS)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>New</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
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

            <Modal title={editing ? 'Edit Class' : 'Create Class'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="name" label="Class Name" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="code" label="Class Code">
                        <Input />
                    </Form.Item>
                    <Form.Item name="major" label="Major" rules={[{ required: true }]}>
                        <Select>
                            {majors.map(m => <Select.Option key={m.id} value={m.id}>{m.name}</Select.Option>)}
                        </Select>
                    </Form.Item>
                    <Form.Item name="cohort" label="Cohort" rules={[{ required: true }]}>
                        <Select>
                            {cohorts.map(c => <Select.Option key={c.id} value={c.id}>{c.name}</Select.Option>)}
                        </Select>
                    </Form.Item>
                    <Form.Item name="size" label="Size">
                        <InputNumber style={{ width: '100%' }} />
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
};

export default AdminClassManagement;