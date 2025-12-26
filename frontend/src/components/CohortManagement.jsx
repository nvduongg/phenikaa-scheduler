import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const CohortManagement = () => {
    const [cohorts, setCohorts] = useState([]);
    const [loading, setLoading] = useState(false);

    // Fetch
    const fetchCohorts = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/cohorts');
            // Sắp xếp theo tên để K18 lên trên K17 nếu muốn, hoặc ngược lại
            const sorted = res.data.sort((a, b) => a.name.localeCompare(b.name));
            setCohorts(sorted);
        } catch {
            message.error("Failed to fetch cohorts");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCohorts();
    }, []);

    // Upload
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/cohorts/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchCohorts();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    // Template
    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/cohorts/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Cohort_Import_Template.xlsx');
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
            title: 'Cohort Name',
            dataIndex: 'name',
            key: 'name',
            align: 'center',
            render: (text) => <Tag color="geekblue" style={{ fontSize: '14px', padding: '5px 10px' }}>{text}</Tag>
        },
        {
            title: 'Academic Period',
            key: 'period',
            align: 'center',
            render: (_, record) => (
                <Text strong>
                    {record.startYear} - {record.endYear}
                </Text>
            )
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Cohort Management</Title>
                    <Text type="secondary">Student Intakes (Khóa sinh viên)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchCohorts} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={cohorts} 
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </Space>
    );
};

export default CohortManagement;