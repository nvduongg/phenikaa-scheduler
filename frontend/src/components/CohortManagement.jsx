import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Modal, Form, Input, InputNumber, Popconfirm } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const CohortManagement = () => {
    const [cohorts, setCohorts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();

    // Fetch
    const fetchCohorts = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/cohorts');
            // Sắp xếp theo tên để K18 lên trên K17 nếu muốn, hoặc ngược lại
            const sorted = res.data.sort((a, b) => a.name.localeCompare(b.name));
            setCohorts(sorted);
        } catch {
            message.error("Failed to fetch cohorts");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCohorts();
    }, []);

    const openCreate = () => { setEditing(null); form.resetFields(); setModalVisible(true); };

    const onEdit = (record) => { setEditing(record); form.setFieldsValue({ name: record.name, startYear: record.startYear, endYear: record.endYear }); setModalVisible(true); };

    const onDelete = async (id) => { try { await axiosClient.delete(`/cohorts/${id}`); message.success('Deleted'); fetchCohorts(); } catch { message.error('Delete failed'); } };

    const onFinish = async (values) => {
        try {
            if (editing) { await axiosClient.put(`/cohorts/${editing.id}`, values); message.success('Updated'); }
            else { await axiosClient.post('/cohorts', values); message.success('Created'); }
            setModalVisible(false); fetchCohorts();
        } catch { message.error('Save failed'); }
    };

    // Upload
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/cohorts/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchCohorts();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    // Template
    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/cohorts/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Cohort_Import_Template.xlsx');
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
            title: 'Cohort Name',
            dataIndex: 'name',
            key: 'name',
            align: 'center',
            render: (text) => <Tag color="geekblue" style={{ fontSize: '14px', padding: '5px 10px' }}>{text}</Tag>
        },
        {
            title: 'Academic Period',
            key: 'period',
            align: 'center',
            render: (_, record) => (
                <Text strong>
                    {record.startYear} - {record.endYear}
                </Text>
            )
        }
        ,{
            title: 'Actions', key: 'actions', width: 150, render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Edit</Button>
                    <Popconfirm title="Delete this cohort?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Delete</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Cohort Management</Title>
                    <Text type="secondary">Student Intakes (Khóa sinh viên)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>New</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchCohorts} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={cohorts} 
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>

            <Modal title={editing ? 'Edit Cohort' : 'Create Cohort'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="name" label="Cohort Name" rules={[{ required: true }]}>
                        <Input placeholder="e.g. K17" />
                    </Form.Item>
                    <Form.Item name="startYear" label="Start Year">
                        <InputNumber style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="endYear" label="End Year">
                        <InputNumber style={{ width: '100%' }} />
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
};

export default CohortManagement;