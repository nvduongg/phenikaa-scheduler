import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Tooltip } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, PartitionOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

// Nhận prop onNavigate từ App.jsx
const CurriculumManagement = ({ onNavigate }) => {
    const [curricula, setCurricula] = useState([]);
    const [loading, setLoading] = useState(false);

    const fetchCurricula = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/curricula');
            setCurricula(res.data);
        } catch {
            message.error("Failed to fetch curricula");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCurricula();
    }, []);

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/curricula/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchCurricula();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/curricula/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Curriculum_Import_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error("Failed to download template");
        }
    };

    const columns = [
        {
            title: 'Curriculum Name',
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
            title: 'Action',
            key: 'action',
            align: 'center',
            width: 150,
            render: (_, record) => (
                <Tooltip title="View Roadmap Details">
                    <Button 
                        type="primary" 
                        ghost 
                        icon={<PartitionOutlined />} 
                        size="small"
                        onClick={() => onNavigate(record)} // Gọi hàm điều hướng
                    >
                        Roadmap
                    </Button>
                </Tooltip>
            )
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Curriculum Management</Title>
                    <Text type="secondary">Study Programs Framework (Khung chương trình)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchCurricula} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={curricula} 
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </Space>
    );
};

export default CurriculumManagement;