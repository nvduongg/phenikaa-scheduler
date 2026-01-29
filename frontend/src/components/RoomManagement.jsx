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
            message.error("Không thể tải danh sách phòng");
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
            message.error('Không thể tải file mẫu');
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
                message.success(`Đã nhập ${info.file.name} thành công`);
                fetchRooms();
            } else if (info.file.status === 'error') {
                message.error(`Nhập ${info.file.name} thất bại`);
            }
        },
    };

    // Columns
    const columns = [
        {
            title: 'Tên phòng',
            dataIndex: 'name',
            key: 'name',
            align: 'center',
            sorter: (a, b) => (a.name || '').localeCompare(b.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong style={{ fontSize: '16px' }}>{text}</Text>
        },
        {
            title: 'Sức chứa',
            dataIndex: 'capacity',
            key: 'capacity',
            align: 'center',
            sorter: (a, b) => (a.capacity || 0) - (b.capacity || 0),
            sortDirections: ['ascend', 'descend'],
            render: (cap) => <Tag color="geekblue" style={{ fontSize: '14px' }}>{cap}</Tag>
        },
        {
            title: 'Loại',
            dataIndex: 'type',
            key: 'type',
            align: 'center',
            sorter: (a, b) => (a.type || '').localeCompare(b.type || ''),
            sortDirections: ['ascend', 'descend'],
            render: (type) => {
                let color = 'default';
                let icon = <ReadOutlined />;
                if (type === 'PC') {
                    color = 'magenta';
                    icon = <DesktopOutlined />;
                } else if (type === 'LAB') {
                    color = 'orange';
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
            title: 'Thao tác',
            key: 'actions',
            width: 160,
            align: 'center',
            render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Sửa</Button>
                    <Popconfirm title="Xóa phòng này?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Xóa</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => { setEditing(null); form.resetFields(); setModalVisible(true); };
    const onEdit = (record) => { setEditing(record); form.setFieldsValue({ name: record.name, capacity: record.capacity, type: record.type }); setModalVisible(true); };
    const onDelete = async (id) => { try { await axiosClient.delete(`/rooms/${id}`); message.success('Đã xóa'); fetchRooms(); } catch { message.error('Xóa thất bại'); } };

    const onFinish = async (values) => {
        try {
            if (editing) { await axiosClient.put(`/rooms/${editing.id}`, values); message.success('Đã cập nhật'); }
            else { await axiosClient.post('/rooms', values); message.success('Đã tạo'); }
            setModalVisible(false); fetchRooms();
        } catch { message.error('Lưu thất bại'); }
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Quản lý phòng học</Title>
                    <Text type="secondary">Phòng học & phòng máy</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Mẫu
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm mới</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Nhập Excel</Button>
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

            <Modal title={editing ? 'Sửa phòng' : 'Tạo phòng'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="name" label="Tên phòng" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="capacity" label="Sức chứa" rules={[{ required: true }]}>
                        <InputNumber min={1} style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="type" label="Loại" rules={[{ required: true }]}>
                        <Select placeholder="Chọn loại phòng">
                            <Option value="THEORY">THEORY (Phòng học)</Option>
                            <Option value="PC">PC (Phòng máy tính)</Option>
                            <Option value="LAB">LAB (Phòng thí nghiệm/thực hành)</Option>
                            <Option value="HALL">HALL (Hội trường/Sân bãi)</Option>
                            <Option value="ONLINE">ONLINE (Trực tuyến)</Option>
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
};

export default RoomManagement;