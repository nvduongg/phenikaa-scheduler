import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const MajorManagement = () => {
    const [majors, setMajors] = useState([]);
    const [loading, setLoading] = useState(false);

    // 1. Fetch Majors
    const fetchMajors = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/majors');
            setMajors(res.data);
        } catch {
            message.error("Failed to fetch majors");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchMajors();
    }, []);

    // 2. Upload Config
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/majors/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchMajors();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    // 3. Download Template
    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/majors/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Major_Import_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error("Failed to download template");
        }
    };

    // 4. Columns
    const columns = [
        {
            title: 'Major Code',
            dataIndex: 'code',
            key: 'code',
            width: 150,
            render: (text) => <Tag color="geekblue">{text}</Tag>
        },
        {
            title: 'Major Name',
            dataIndex: 'name',
            key: 'name',
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Managing Faculty',
            dataIndex: ['faculty', 'name'], // Nested object access
            key: 'faculty',
            render: (text) => <Tag color="purple">{text}</Tag>
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Major Management</Title>
                    <Text type="secondary">Academic Majors / Specializations</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchMajors} />
                </Space>
            </div>

            {/* Table */}
            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={majors} 
                    loading={loading}
                    pagination={{ pageSize: 8 }}
                />
            </Card>
        </Space>
    );
};

export default MajorManagement;