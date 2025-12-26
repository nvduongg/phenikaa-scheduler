import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, BankOutlined, DesktopOutlined, ReadOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const RoomManagement = () => {
    const [rooms, setRooms] = useState([]);
    const [loading, setLoading] = useState(false);

    // Fetch
    const fetchRooms = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/rooms');
            // Sort theo tên phòng
            const sorted = res.data.sort((a, b) => a.name.localeCompare(b.name));
            setRooms(sorted);
        } catch {
            message.error("Failed to fetch rooms");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchRooms();
    }, []);

    // Upload
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/rooms/import',
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchRooms();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    // Template
    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/rooms/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Room_Import_Template.xlsx');
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
            title: 'Room Name',
            dataIndex: 'name',
            key: 'name',
            width: 150,
            render: (text) => <Text strong style={{ fontSize: '16px' }}>{text}</Text>
        },
        {
            title: 'Capacity',
            dataIndex: 'capacity',
            key: 'capacity',
            align: 'center',
            width: 120,
            render: (cap) => <Tag color="geekblue" style={{ fontSize: '14px' }}>{cap} Seats</Tag>
        },
        {
            title: 'Type',
            dataIndex: 'type',
            key: 'type',
            render: (type) => {
                let color = 'default';
                let icon = <ReadOutlined />;
                if (type === 'LAB') {
                    color = 'magenta';
                    icon = <DesktopOutlined />;
                } else if (type === 'HALL') {
                    color = 'gold';
                    icon = <BankOutlined />;
                } else {
                    color = 'cyan'; // THEORY
                }
                
                return (
                    <Tag color={color} icon={icon}>
                        {type}
                    </Tag>
                );
            }
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Room Management</Title>
                    <Text type="secondary">Classrooms & Laboratories (Phòng học & Phòng máy)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchRooms} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    rowKey="id"
                    columns={columns} 
                    dataSource={rooms} 
                    loading={loading}
                    pagination={{ pageSize: 15 }}
                />
            </Card>
        </Space>
    );
};

export default RoomManagement;