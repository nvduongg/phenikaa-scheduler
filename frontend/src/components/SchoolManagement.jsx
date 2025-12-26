import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const SchoolManagement = () => {
    const [schools, setSchools] = useState([]);
    const [loading, setLoading] = useState(false);

    const fetchSchools = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/schools');
            setSchools(res.data);
        } catch {
            message.error("Failed to fetch schools");
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
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchSchools();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
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
            message.error("Failed to download template");
        }
    };

    const columns = [
        {
            title: 'School Code',
            dataIndex: 'code',
            key: 'code',
            width: 150,
            render: (text) => <Tag color="geekblue">{text}</Tag>
        },
        {
            title: 'School Name',
            dataIndex: 'name',
            key: 'name',
            render: (text) => <Text strong style={{ fontSize: '15px' }}>{text}</Text>
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>School Management</Title>
                    <Text type="secondary">List of Member Schools within Phenikaa University</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
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
        </Space>
    );
};

export default SchoolManagement;