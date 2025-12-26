import React, { useState, useEffect } from 'react';
import { Upload, Button, Table, message, Card, Typography, Space, Tag, Tooltip } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, ClockCircleOutlined, EnvironmentOutlined, UserSwitchOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const OfferingManagement = () => {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);

    const fetchData = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get('/offerings');
            setData(response.data);
        } catch (error) {
            message.error('Failed to load data: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/offerings/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} uploaded successfully.`);
                fetchData(); 
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} upload failed.`);
            }
        },
    };

    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/offerings/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Offering_Plan_Template.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error('Failed to download template');
        }
    };

    // Hàm gọi API Auto Assign
    const handleAutoAssign = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.post('/offerings/auto-assign-lecturers');
            message.success(res.data);
            fetchData(); // Reload lại bảng để thấy kết quả
        } catch (error) {
            message.error("Auto-assign failed: " + error.message);
        } finally {
            setLoading(false);
        }
    };

    const columns = [
        {
            title: 'Class Code',
            dataIndex: 'code',
            key: 'code',
            render: (text) => <Text strong copyable>{text}</Text>,
        },
        {
            title: 'Course',
            dataIndex: 'course',
            key: 'course',
            render: (course) => (
                <div>
                    <div style={{ fontWeight: 500 }}>{course.name}</div>
                    <Tag color="cyan">{course.courseCode}</Tag>
                </div>
            )
        },
        {
            title: 'Size',
            dataIndex: 'plannedSize',
            key: 'plannedSize',
            align: 'center',
            width: 80,
            render: (size) => <Tag color="volcano">{size}</Tag>
        },
        {
            title: 'Assigned Lecturer',
            dataIndex: 'lecturer',
            key: 'lecturer',
            render: (lec) => lec ? (
                <Tag color="blue">{lec.fullName}</Tag>
            ) : (
                <Text type="secondary" italic>Auto-assign</Text>
            )
        },
        {
            title: 'Schedule (Output)', // Cột kết quả
            key: 'schedule',
            render: (_, record) => {
                if (record.status === 'PLANNED') {
                    return <Tag icon={<ClockCircleOutlined />} color="default">Pending Schedule</Tag>;
                }
                // Sau này khi có kết quả sẽ hiện ở đây
                return (
                    <Space direction="vertical" size={0}>
                        <Tag icon={<ClockCircleOutlined />} color="green">
                            {record.dayOfWeek === 8 ? 'Sun' : `Mon ${record.dayOfWeek}`}, Per {record.startPeriod}-{record.endPeriod}
                        </Tag>
                        <Tag icon={<EnvironmentOutlined />} color="geekblue">
                            {record.room ? record.room.name : 'No Room'}
                        </Tag>
                    </Space>
                );
            }
        }
    ];

    return (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Course Offering Management</Title>
                    <Text type="secondary">Input Demand for Timetabling</Text>
                </div>
                <Space>
                    {/* Thêm nút Auto Assign vào đây */}
                    <Button 
                        type="primary" 
                        icon={<UserSwitchOutlined />} 
                        onClick={handleAutoAssign}
                        loading={loading}
                    >
                        Auto Assign Lecturers
                    </Button>

                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="default" icon={<UploadOutlined />}>Import Plan</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchData} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    columns={columns} 
                    dataSource={data} 
                    rowKey="id"
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </Space>
    );
};

export default OfferingManagement;