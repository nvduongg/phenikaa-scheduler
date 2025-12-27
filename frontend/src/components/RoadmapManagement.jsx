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
                // Parse semester string to compare first semester
                const semA = parseInt(a.semesterIndex.split(',')[0]);
                const semB = parseInt(b.semesterIndex.split(',')[0]);
                
                if (semA === semB) {
                    return a.course.name.localeCompare(b.course.name);
                }
                return semA - semB;
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

    const groupedDetails = React.useMemo(() => {
        const groups = {};
        details.forEach(item => {
            const sem = item.semesterIndex;
            if (!groups[sem]) groups[sem] = [];
            groups[sem].push(item);
        });
        return groups;
    }, [details]);

    const sortedSemesters = React.useMemo(() => {
        return Object.keys(groupedDetails).sort((a, b) => {
            const semA = parseInt(a.split(',')[0]);
            const semB = parseInt(b.split(',')[0]);
            return semA - semB;
        });
    }, [groupedDetails]);

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/curriculum-details/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
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
            width: 120,
            render: (sem) => {
                const sems = sem.toString().split(',');
                return (
                    <Space size={4}>
                        {sems.map(s => <Tag color="blue" key={s}>Sem {s.trim()}</Tag>)}
                    </Space>
                );
            }
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

            {loading ? (
                <Card loading />
            ) : (
                <Space direction="vertical" style={{ width: '100%' }} size="middle">
                    {sortedSemesters.length > 0 ? (
                        sortedSemesters.map(sem => (
                            <Card key={sem} title={`Semester ${sem}`} size="small" type="inner">
                                <Table 
                                    rowKey="id"
                                    columns={columns.filter(c => c.key !== 'sem')} 
                                    dataSource={groupedDetails[sem]} 
                                    pagination={false}
                                    size="small"
                                />
                            </Card>
                        ))
                    ) : (
                        <Card><Text type="secondary">No roadmap data found.</Text></Card>
                    )}
                </Space>
            )}
        </Space>
    );
};

export default RoadmapManagement;