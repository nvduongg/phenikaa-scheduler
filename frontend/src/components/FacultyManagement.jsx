import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Modal, Form, Input, Select, Popconfirm } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const FacultyManagement = () => {
    const [faculties, setFaculties] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();
    const [schools, setSchools] = useState([]);

    const fetchFaculties = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/faculties');
            setFaculties(res.data);
        } catch {
            message.error("Failed to fetch faculties");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchFaculties();
        fetchSchools();
    }, []);

    const fetchSchools = async () => {
        try {
            const res = await axiosClient.get('/schools');
            setSchools(res.data);
        } catch {
            // ignore
        }
    };

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/faculties/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchFaculties();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/faculties/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Faculty_Import_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error("Failed to download template");
        }
    };

    const columns = [
        {
            title: 'Faculty Code',
            dataIndex: 'code',
            key: 'code',
            width: 150,
            render: (text) => <Tag color="blue">{text}</Tag>
        },
        {
            title: 'Faculty Name',
            dataIndex: 'name',
            key: 'name',
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Affiliated School',
            dataIndex: ['school', 'name'],
            key: 'school',
            render: (text) => text ? <Tag color="purple">{text}</Tag> : <Text type="secondary">Phenikaa University (Direct)</Text>
        }
        ,{
            title: 'Actions',
            key: 'actions',
            width: 160,
            render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Edit</Button>
                    <Popconfirm title="Delete this faculty?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Delete</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => { setEditing(null); form.resetFields(); setModalVisible(true); };

    const onEdit = (record) => { setEditing(record); form.setFieldsValue({ name: record.name, code: record.code, school: record.school?.id }); setModalVisible(true); };

    const onDelete = async (id) => {
        try { await axiosClient.delete(`/faculties/${id}`); message.success('Deleted'); fetchFaculties(); } catch { message.error('Delete failed'); }
    };

    const onFinish = async (values) => {
        try {
            const payload = { ...values };
            if (values.school) payload.school = { id: values.school };
            if (editing) { await axiosClient.put(`/faculties/${editing.id}`, payload); message.success('Updated'); }
            else { await axiosClient.post('/faculties', payload); message.success('Created'); }
            setModalVisible(false); fetchFaculties();
        } catch { message.error('Save failed'); }
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Faculty Management</Title>
                    <Text type="secondary">Organizational structure of Faculties and Institutes</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>New</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchFaculties} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={faculties} 
                    loading={loading}
                    pagination={{ pageSize: 8,  }}
                />
            </Card>

            <Modal title={editing ? 'Edit Faculty' : 'Create Faculty'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="code" label="Faculty Code" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="name" label="Faculty Name" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="school" label="School (optional)">
                        <Select allowClear>
                            {schools.map(s => <Select.Option key={s.id} value={s.id}>{s.name}</Select.Option>)}
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
};

export default FacultyManagement;