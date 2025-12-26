import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Breadcrumb } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, ArrowLeftOutlined, BookOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

// Nhận props từ App.jsx
const RoadmapManagement = ({ targetCurriculum, onBack }) => {
    const [details, setDetails] = useState([]);
    const [loading, setLoading] = useState(false);

    const fetchDetails = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/curriculum-details');
            let data = res.data;

            // Nếu có targetCurriculum, lọc dữ liệu chỉ của CTĐT đó
            if (targetCurriculum) {
                data = data.filter(d => d.curriculum.id === targetCurriculum.id);
            }

            // Sort theo Kỳ -> Tên Môn
            const sorted = data.sort((a, b) => {
                if (a.semesterIndex === b.semesterIndex) {
                    return a.course.name.localeCompare(b.course.name);
                }
                return a.semesterIndex - b.semesterIndex;
            });

            setDetails(sorted);
        } catch {
            message.error("Failed to fetch roadmap details");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchDetails();
    }, [targetCurriculum]); // Chạy lại khi target thay đổi

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/curriculum-details/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchDetails();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/curriculum-details/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Roadmap_Import_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error("Failed to download template");
        }
    };

    const columns = [
        {
            title: 'Semester',
            dataIndex: 'semesterIndex',
            key: 'sem',
            align: 'center',
            width: 100,
            render: (sem) => <Tag color="blue">Sem {sem}</Tag>
        },
        {
            title: 'Course Code',
            dataIndex: ['course', 'courseCode'],
            key: 'code',
            render: (text) => <Text code>{text}</Text>
        },
        {
            title: 'Course Name',
            dataIndex: ['course', 'name'],
            key: 'courseName',
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Credits',
            dataIndex: ['course', 'credits'],
            key: 'credits',
            align: 'center',
            render: (cr) => <Tag color="default">{cr} TC</Tag>
        }
    ];

    // Header riêng nếu đang xem chi tiết 1 CTĐT
    const renderHeaderTitle = () => {
        if (targetCurriculum) {
            return (
                <div>
                    <Button type="link" icon={<ArrowLeftOutlined />} onClick={onBack} style={{ paddingLeft: 0 }}>
                        Back to Curricula
                    </Button>
                    <Title level={3} style={{ margin: 0 }}>
                        Roadmap: <span style={{ color: '#0054a6' }}>{targetCurriculum.name}</span>
                    </Title>
                    <Text type="secondary">Detailed course sequence for {targetCurriculum.cohort.name} - {targetCurriculum.major.name}</Text>
                </div>
            );
        }
        return (
            <div>
                <Title level={3} style={{ margin: 0 }}>Roadmap Management</Title>
                <Text type="secondary">All Course Sequences</Text>
            </div>
        );
    };

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                {renderHeaderTitle()}
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchDetails} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={details} 
                    loading={loading}
                    pagination={{ pageSize: 15 }}
                />
            </Card>
        </Space>
    );
};

export default RoadmapManagement;