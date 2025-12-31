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
            message.error("Không thể tải danh sách học kỳ");
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
            message.success("Đã tạo học kỳ");
            setIsModalOpen(false);
            form.resetFields();
            fetchSemesters();
        } catch {
            message.error("Không thể tạo học kỳ");
        }
    };

    const handleSetCurrent = async (id) => {
        try {
            await axiosClient.post(`/semesters/${id}/set-current`);
            message.success("Đã cập nhật học kỳ hiện hành");
            fetchSemesters();
        } catch {
            message.error("Cập nhật thất bại");
        }
    };

    const columns = [
        {
            title: 'Tên học kỳ',
            dataIndex: 'name',
            key: 'name',
            sorter: (a, b) => (a.name || '').localeCompare(b.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Năm học',
            dataIndex: 'academicYear',
            key: 'year',
            sorter: (a, b) => (a.academicYear || '').localeCompare(b.academicYear || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="blue">{text}</Tag>
        },
        {
            title: 'Trạng thái',
            dataIndex: 'isCurrent',
            key: 'status',
            align: 'center',
            render: (isCurrent, record) => (
                isCurrent ? 
                <Tag color="success" icon={<CheckCircleOutlined />}>ĐANG ÁP DỤNG</Tag> : 
                <Button type="link" size="small" onClick={() => handleSetCurrent(record.id)}>Đặt làm hiện hành</Button>
            )
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Quản lý học kỳ</Title>
                    <Text type="secondary">Thiết lập học kỳ và chọn học kỳ dùng để xếp lịch</Text>
                </div>
                <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalOpen(true)}>
                    Thêm học kỳ
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

            <Modal title="Tạo học kỳ mới" open={isModalOpen} onCancel={() => setIsModalOpen(false)} onOk={() => form.submit()}>
                <Form form={form} layout="vertical" onFinish={handleCreate}>
                    <Form.Item name="name" label="Tên học kỳ" rules={[{ required: true }]}>
                        <Input placeholder="Ví dụ: Học kỳ 1 năm 2025-2026" />
                    </Form.Item>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item name="academicYear" label="Năm học" rules={[{ required: true }]}>
                                <Input placeholder="Ví dụ: 2025-2026" />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item name="term" label="Học kỳ" rules={[{ required: true }]}>
                                <Select>
                                    <Option value={1}>Học kỳ 1</Option>
                                    <Option value={2}>Học kỳ 2</Option>
                                    <Option value={3}>Học kỳ hè</Option>
                                </Select>
                            </Form.Item>
                        </Col>
                    </Row>
                    <Form.Item name="isCurrent" valuePropName="checked" label="Đặt làm học kỳ hiện hành?">
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