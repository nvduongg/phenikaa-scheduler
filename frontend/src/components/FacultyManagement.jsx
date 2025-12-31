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
            message.error("Không thể tải danh sách khoa/viện");
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
                message.success(`Đã nhập ${info.file.name} thành công`);
                fetchFaculties();
            } else if (info.file.status === 'error') {
                message.error(`Nhập ${info.file.name} thất bại`);
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
            message.error("Không thể tải file mẫu");
        }
    };

    const columns = [
        {
            title: 'Mã khoa/viện',
            dataIndex: 'code',
            key: 'code',
            width: 150,
            sorter: (a, b) => (a.code || '').localeCompare(b.code || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="blue">{text}</Tag>
        },
        {
            title: 'Tên khoa/viện',
            dataIndex: 'name',
            key: 'name',
            sorter: (a, b) => (a.name || '').localeCompare(b.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Trường trực thuộc',
            dataIndex: ['school', 'name'],
            key: 'school',
            sorter: (a, b) => (a.school?.name || '').localeCompare(b.school?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => text ? <Tag color="purple">{text}</Tag> : <Text type="secondary">Đại học Phenikaa (Trực thuộc)</Text>
        }
        ,{
            title: 'Thao tác',
            key: 'actions',
            width: 160,
            render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Sửa</Button>
                    <Popconfirm title="Xóa khoa/viện này?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Xóa</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => { setEditing(null); form.resetFields(); setModalVisible(true); };

    const onEdit = (record) => { setEditing(record); form.setFieldsValue({ name: record.name, code: record.code, school: record.school?.id }); setModalVisible(true); };

    const onDelete = async (id) => {
        try { await axiosClient.delete(`/faculties/${id}`); message.success('Đã xóa'); fetchFaculties(); } catch { message.error('Xóa thất bại'); }
    };

    const onFinish = async (values) => {
        try {
            const payload = { ...values };
            if (values.school) payload.school = { id: values.school };
            if (editing) { await axiosClient.put(`/faculties/${editing.id}`, payload); message.success('Đã cập nhật'); }
            else { await axiosClient.post('/faculties', payload); message.success('Đã tạo'); }
            setModalVisible(false); fetchFaculties();
        } catch { message.error('Lưu thất bại'); }
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Quản lý khoa/viện</Title>
                    <Text type="secondary">Cơ cấu tổ chức các khoa và viện</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Mẫu
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm mới</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Nhập Excel</Button>
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

            <Modal title={editing ? 'Sửa khoa/viện' : 'Tạo khoa/viện'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="code" label="Mã khoa/viện" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="name" label="Tên khoa/viện" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="school" label="Trường (tuỳ chọn)">
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