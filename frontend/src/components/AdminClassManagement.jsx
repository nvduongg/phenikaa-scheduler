import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const AdminClassManagement = () => {
    const [classes, setClasses] = useState([]);
    const [loading, setLoading] = useState(false);

    // Fetch
    const fetchClasses = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/admin-classes');
            setClasses(res.data);
        } catch {
            message.error("Failed to fetch classes");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchClasses();
    }, []);

    // Upload
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/admin-classes/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchClasses();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    // Template
    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/admin-classes/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Admin_Class_Import_Template.xlsx');
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
            title: 'Class Name',
            dataIndex: 'name',
            key: 'name',
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Major',
            dataIndex: ['major', 'name'],
            key: 'major',
            render: (text) => <Tag color="geekblue">{text}</Tag>
        },
        {
            title: 'Cohort',
            dataIndex: ['cohort', 'name'],
            key: 'cohort',
            align: 'center',
            width: 100,
            render: (text) => <Tag color="orange">{text}</Tag>
        },
        {
            title: 'Size',
            dataIndex: 'size',
            key: 'size',
            align: 'center',
            width: 100,
            render: (size) => <Tag color="default">{size}</Tag>
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Administrative Classes</Title>
                    <Text type="secondary">Student Classes (Lớp biên chế / Nhóm KS)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchClasses} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={classes} 
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </Space>
    );
};

export default AdminClassManagement;