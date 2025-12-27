import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Select, Switch, message, Card, Typography, Space, Tag } from 'antd';
import { PlusOutlined, CheckCircleOutlined, SyncOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;
const { Option } = Select;

const SemesterManagement = () => {
    const [semesters, setSemesters] = useState([]);
    const [loading, setLoading] = useState(false);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [form] = Form.useForm();

    const fetchSemesters = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/semesters');
            // Sort kỳ mới nhất lên đầu
            setSemesters(res.data.sort((a, b) => b.id - a.id));
        } catch {
            message.error("Failed to load semesters");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchSemesters();
    }, []);

    const handleCreate = async (values) => {
        try {
            await axiosClient.post('/semesters', values);
            message.success("Semester created");
            setIsModalOpen(false);
            form.resetFields();
            fetchSemesters();
        } catch {
            message.error("Failed to create semester");
        }
    };

    const handleSetCurrent = async (id) => {
        try {
            await axiosClient.post(`/semesters/${id}/set-current`);
            message.success("Active semester updated");
            fetchSemesters();
        } catch {
            message.error("Update failed");
        }
    };

    const columns = [
        {
            title: 'Semester Name',
            dataIndex: 'name',
            key: 'name',
            sorter: (a, b) => (a.name || '').localeCompare(b.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Academic Year',
            dataIndex: 'academicYear',
            key: 'year',
            sorter: (a, b) => (a.academicYear || '').localeCompare(b.academicYear || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="blue">{text}</Tag>
        },
        {
            title: 'Status',
            dataIndex: 'isCurrent',
            key: 'status',
            align: 'center',
            render: (isCurrent, record) => (
                isCurrent ? 
                <Tag color="success" icon={<CheckCircleOutlined />}>ACTIVE</Tag> : 
                <Button type="link" size="small" onClick={() => handleSetCurrent(record.id)}>Set Active</Button>
            )
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Semester Management</Title>
                    <Text type="secondary">Define academic terms and set the active scheduling period</Text>
                </div>
                <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalOpen(true)}>
                    New Semester
                </Button>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={semesters} 
                    loading={loading}
                    pagination={false}
                />
            </Card>

            <Modal title="Create New Semester" open={isModalOpen} onCancel={() => setIsModalOpen(false)} onOk={() => form.submit()}>
                <Form form={form} layout="vertical" onFinish={handleCreate}>
                    <Form.Item name="name" label="Semester Name" rules={[{ required: true }]}>
                        <Input placeholder="e.g., Hoc ky 1 Nam 2025-2026" />
                    </Form.Item>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="academicYear" label="Academic Year" rules={[{ required: true }]}>
                                <Input placeholder="2025-2026" />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="term" label="Term" rules={[{ required: true }]}>
                                <Select>
                                    <Option value={1}>Term 1</Option>
                                    <Option value={2}>Term 2</Option>
                                    <Option value={3}>Summer Term</Option>
                                </Select>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Form.Item name="isCurrent" valuePropName="checked" label="Set as Active Semester?">
                        <Switch />
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
};
// Nhớ import Row, Col từ antd ở trên đầu file
import { Row, Col } from 'antd';

export default SemesterManagement;