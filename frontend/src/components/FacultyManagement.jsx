import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const FacultyManagement = () => {
    const [faculties, setFaculties] = useState([]);
    const [loading, setLoading] = useState(false);

    const fetchFaculties = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/faculties');
            setFaculties(res.data);
        } catch {
            message.error("Failed to fetch faculties");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchFaculties();
    }, []);

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/faculties/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchFaculties();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
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
            message.error("Failed to download template");
        }
    };

    const columns = [
        {
            title: 'Faculty Code',
            dataIndex: 'code',
            key: 'code',
            width: 150,
            render: (text) => <Tag color="blue">{text}</Tag>
        },
        {
            title: 'Faculty Name',
            dataIndex: 'name',
            key: 'name',
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Affiliated School',
            dataIndex: ['school', 'name'],
            key: 'school',
            render: (text) => text ? <Tag color="purple">{text}</Tag> : <Text type="secondary">Phenikaa University (Direct)</Text>
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Faculty Management</Title>
                    <Text type="secondary">Organizational structure of Faculties and Institutes</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
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
                    pagination={{ pageSize: 8 }}
                />
            </Card>
        </Space>
    );
};

export default FacultyManagement;