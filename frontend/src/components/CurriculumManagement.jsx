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
            message.error("Không thể tải danh sách khung CTĐT");
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
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`Đã nhập ${info.file.name} thành công`);
                fetchCurricula();
            } else if (info.file.status === 'error') {
                message.error(`Nhập ${info.file.name} thất bại`);
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
            message.error("Không thể tải file mẫu");
        }
    };

    const columns = [
        {
            title: 'Tên khung CTĐT',
            dataIndex: 'name',
            key: 'name',
            sorter: (a, b) => (a.name || '').localeCompare(b.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Ngành',
            dataIndex: ['major', 'name'],
            key: 'major',
            sorter: (a, b) => (a.major?.name || '').localeCompare(b.major?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="geekblue">{text}</Tag>
        },
        {
            title: 'Khóa',
            dataIndex: ['cohort', 'name'],
            key: 'cohort',
            align: 'center',
            width: 100,
            sorter: (a, b) => (a.cohort?.name || '').localeCompare(b.cohort?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="orange">{text}</Tag>
        },
        {
            title: 'Thao tác',
            key: 'action',
            align: 'center',
            width: 150,
            render: (_, record) => (
                <Tooltip title="Xem chi tiết lộ trình">
                    <Button 
                        type="primary" 
                        ghost 
                        icon={<PartitionOutlined />} 
                        size="small"
                        onClick={() => onNavigate(record)} // Gọi hàm điều hướng
                    >
                        Lộ trình
                    </Button>
                </Tooltip>
            )
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Quản lý khung CTĐT</Title>
                    <Text type="secondary">Khung chương trình đào tạo</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Mẫu
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Nhập Excel</Button>
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