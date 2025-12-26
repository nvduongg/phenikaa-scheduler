import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, UserOutlined, BookOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const ExpertiseManagement = () => {
    const [lecturers, setLecturers] = useState([]); // Chúng ta dùng lại API lecturers vì nó đã kèm list courses
    const [loading, setLoading] = useState(false);

    const fetchExpertise = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/lecturers');
            // Lọc ra những giảng viên có chuyên môn để hiển thị lên đầu (tùy chọn)
            setLecturers(res.data);
        } catch {
            message.error("Failed to fetch expertise data");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchExpertise();
    }, []);

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/expertise/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchExpertise();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/expertise/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Expertise_Import_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error("Failed to download template");
        }
    };

    const columns = [
        {
            title: 'Lecturer',
            key: 'lecturer',
            width: 250,
            render: (record) => (
                <div>
                    <div style={{ fontWeight: 600 }}>{record.fullName}</div>
                    <div style={{ fontSize: '12px', color: '#888' }}>{record.lecturerCode}</div>
                </div>
            )
        },
        {
            title: 'Faculty',
            dataIndex: ['faculty', 'name'],
            key: 'faculty',
            width: 200,
            render: (text) => <Tag color="purple">{text}</Tag>
        },
        {
            title: 'Teaching Capabilities (Expertise)',
            dataIndex: 'teachingCourses',
            key: 'courses',
            render: (courses) => (
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                    {courses && courses.length > 0 ? (
                        courses.map(course => (
                            <Tag key={course.courseCode} color="blue" icon={<BookOutlined />}>
                                {course.name} <span style={{ opacity: 0.6 }}>({course.courseCode})</span>
                            </Tag>
                        ))
                    ) : (
                        <Text type="secondary" italic>No expertise assigned yet</Text>
                    )}
                </div>
            )
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Expertise Management</Title>
                    <Text type="secondary">Assign courses to lecturers (Phân công chuyên môn)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Mapping</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchExpertise} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={lecturers} 
                    loading={loading}
                    pagination={{ pageSize: 8 }}
                />
            </Card>
        </Space>
    );
};

export default ExpertiseManagement;