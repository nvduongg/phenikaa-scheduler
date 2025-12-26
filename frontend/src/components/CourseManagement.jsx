import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Tooltip } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const CourseManagement = () => {
    const [courses, setCourses] = useState([]);
    const [loading, setLoading] = useState(false);

    const fetchCourses = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/courses');
            setCourses(res.data);
        } catch {
            message.error("Failed to fetch courses");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCourses();
    }, []);

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/courses/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchCourses();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/courses/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Course_Import_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error("Failed to download template");
        }
    };

    const columns = [
        {
            title: 'Course Code',
            dataIndex: 'courseCode',
            key: 'courseCode',
            width: 120,
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Course Name',
            dataIndex: 'name',
            key: 'name',
            render: (text) => <span style={{ fontWeight: 500 }}>{text}</span>
        },
        {
            title: 'Credits',
            key: 'credits',
            render: (_, record) => (
                <Space>
                    <Tag color="blue">{record.credits} Credits</Tag>
                    <Tooltip title="Theory / Practice">
                        <Tag color="default">{record.theoryCredits} LT / {record.practiceCredits} TH</Tag>
                    </Tooltip>
                </Space>
            )
        },
        {
            title: 'Managing Faculty',
            dataIndex: ['managingFaculty', 'name'],
            key: 'faculty',
            render: (text) => <Tag color="cyan">{text || 'Unassigned'}</Tag>
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Course Management</Title>
                    <Text type="secondary">Master data of all courses/subjects</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchCourses} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="courseCode"
                    columns={columns} 
                    dataSource={courses} 
                    loading={loading}
                    pagination={{ pageSize: 8 }}
                />
            </Card>
        </Space>
    );
};

export default CourseManagement;