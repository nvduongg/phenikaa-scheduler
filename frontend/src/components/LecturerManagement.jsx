import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, UserOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const LecturerManagement = () => {
    const [lecturers, setLecturers] = useState([]);
    const [loading, setLoading] = useState(false);

    // Fetch
    const fetchLecturers = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/lecturers');
            setLecturers(res.data);
        } catch {
            message.error("Failed to fetch lecturers");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLecturers();
    }, []);

    // Upload
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/lecturers/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchLecturers();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    // Template
    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/lecturers/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Lecturer_Import_Template.xlsx');
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
            title: 'Code',
            dataIndex: 'lecturerCode',
            key: 'code',
            width: 100,
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Full Name',
            dataIndex: 'fullName',
            key: 'name',
            render: (text) => (
                <Text>{text}</Text>
            )
        },
        {
            title: 'Email',
            dataIndex: 'email',
            key: 'email',
            render: (text) => <Text copyable>{text}</Text>
        },
        {
            title: 'Faculty',
            dataIndex: ['faculty', 'name'],
            key: 'faculty',
            render: (text) => <Tag color="purple">{text}</Tag>
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Lecturer Management</Title>
                    <Text type="secondary">Manage Academic Staff (Quản lý giảng viên)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchLecturers} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={lecturers} 
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </Space>
    );
};

export default LecturerManagement;