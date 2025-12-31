import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Modal, Form, Input, Popconfirm } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const SchoolManagement = () => {
    const [schools, setSchools] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();

    const fetchSchools = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/schools');
            setSchools(res.data);
        } catch {
            message.error("Không thể tải danh sách trường");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchSchools();
    }, []);

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/schools/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`Đã nhập ${info.file.name} thành công`);
                fetchSchools();
            } else if (info.file.status === 'error') {
                message.error(`Nhập ${info.file.name} thất bại`);
            }
        },
    };

    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/schools/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'School_Import_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error("Không thể tải file mẫu");
        }
    };

    const columns = [
        {
            title: 'Mã trường',
            dataIndex: 'code',
            key: 'code',
            width: 150,
            sorter: (a, b) => (a.code || '').localeCompare(b.code || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="geekblue">{text}</Tag>
        },
        {
            title: 'Tên trường',
            dataIndex: 'name',
            key: 'name',
            sorter: (a, b) => (a.name || '').localeCompare(b.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong style={{ fontSize: '15px' }}>{text}</Text>
        }
        ,{
            title: 'Thao tác',
            key: 'actions',
            width: 150,
            render: (_, record) => (
                <Space>
                    <Button icon={<EditOutlined />} size="small" onClick={() => onEdit(record)}>Sửa</Button>
                    <Popconfirm title="Xóa trường này?" onConfirm={() => onDelete(record.id)}>
                        <Button danger icon={<DeleteOutlined />} size="small">Xóa</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => {
        setEditing(null);
        form.resetFields();
        setModalVisible(true);
    };

    const onEdit = (record) => {
        setEditing(record);
        form.setFieldsValue({ name: record.name, code: record.code });
        setModalVisible(true);
    };

    const onDelete = async (id) => {
        try {
            await axiosClient.delete(`/schools/${id}`);
            message.success('Đã xóa');
            fetchSchools();
        } catch {
            message.error('Xóa thất bại');
        }
    };

    const onFinish = async (values) => {
        try {
            if (editing) {
                await axiosClient.put(`/schools/${editing.id}`, values);
                message.success('Đã cập nhật');
            } else {
                await axiosClient.post('/schools', values);
                message.success('Đã tạo');
            }
            setModalVisible(false);
            fetchSchools();
        } catch (e) {
            message.error('Lưu thất bại');
        }
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Quản lý trường thành viên</Title>
                    <Text type="secondary">Danh sách các trường thành viên thuộc Đại học Phenikaa</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Mẫu
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm mới</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Nhập Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchSchools} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={schools} 
                    loading={loading}
                    pagination={{ pageSize: 8 }}
                />
            </Card>

            <Modal
                title={editing ? 'Sửa trường' : 'Tạo trường'}
                open={modalVisible}
                onCancel={() => setModalVisible(false)}
                onOk={() => form.submit()}
                destroyOnClose
            >
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="code" label="Mã trường" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="name" label="Tên trường" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
};

export default SchoolManagement;