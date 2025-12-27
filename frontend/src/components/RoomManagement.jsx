import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Modal, Form, Input, InputNumber, Popconfirm, Select } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, BankOutlined, DesktopOutlined, ReadOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;
const { Option } = Select;

const RoomManagement = () => {
    const [rooms, setRooms] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();

    // Fetch
    const fetchRooms = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/rooms');
            // Sort theo tên phòng
            const sorted = res.data.sort((a, b) => a.name.localeCompare(b.name));
            setRooms(sorted);
        } catch {
            message.error("Failed to fetch rooms");
        } finally {
            setLoading(false);
        }
    };

    // Download template
    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/rooms/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Room_Import_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error('Failed to download template');
        }
    };

    useEffect(() => {
        fetchRooms();
    }, []);

    // Upload
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/rooms/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchRooms();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    // Columns
    const columns = [
        {
            title: 'Room Name',
            dataIndex: 'name',
            key: 'name',
            align: 'center',
            render: (text) => <Text strong style={{ fontSize: '16px' }}>{text}</Text>
        },
        {
            title: 'Capacity',
            dataIndex: 'capacity',
            key: 'capacity',
            align: 'center',
            render: (cap) => <Tag color="geekblue" style={{ fontSize: '14px' }}>{cap}</Tag>
        },
        {
            title: 'Type',
            dataIndex: 'type',
            key: 'type',
            align: 'center',
            render: (type) => {
                let color = 'default';
                let icon = <ReadOutlined />;
                if (type === 'LAB') {
                    color = 'magenta';
                    icon = <DesktopOutlined />;
                } else if (type === 'HALL') {
                    color = 'gold';
                    icon = <BankOutlined />;
                } else if (type === 'ONLINE') {
                    color = 'purple';
                    icon = <DesktopOutlined />;
                } else {
                    color = 'cyan'; // THEORY
                }
                return (
                    <Tag color={color} icon={icon}>
                        {type}
                    </Tag>
                );
            }
        },
        {
            title: 'Actions',
            key: 'actions',
            width: 160,
            align: 'center',
            render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Edit</Button>
                    <Popconfirm title="Delete this room?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Delete</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => { setEditing(null); form.resetFields(); setModalVisible(true); };
    const onEdit = (record) => { setEditing(record); form.setFieldsValue({ name: record.name, capacity: record.capacity, type: record.type }); setModalVisible(true); };
    const onDelete = async (id) => { try { await axiosClient.delete(`/rooms/${id}`); message.success('Deleted'); fetchRooms(); } catch { message.error('Delete failed'); } };

    const onFinish = async (values) => {
        try {
            if (editing) { await axiosClient.put(`/rooms/${editing.id}`, values); message.success('Updated'); }
            else { await axiosClient.post('/rooms', values); message.success('Created'); }
            setModalVisible(false); fetchRooms();
        } catch { message.error('Save failed'); }
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Room Management</Title>
                    <Text type="secondary">Classrooms & Laboratories (Phòng học & Phòng máy)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>New</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchRooms} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={rooms} 
                    loading={loading}
                    pagination={{ pageSize: 8 }}
                />
            </Card>

            <Modal title={editing ? 'Edit Room' : 'Create Room'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="name" label="Room Name" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="capacity" label="Capacity" rules={[{ required: true }]}>
                        <InputNumber min={1} style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="type" label="Type" rules={[{ required: true }]}>
                        <Select placeholder="Select room type">
                            <Option value="THEORY">THEORY (Classroom)</Option>
                            <Option value="LAB">LAB (Computer Lab)</Option>
                            <Option value="HALL">HALL (Hall / Field)</Option>
                            <Option value="ONLINE">ONLINE (Virtual)</Option>
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
};

export default RoomManagement;