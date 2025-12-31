import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Avatar, Modal, Select, Form, Tooltip, Input } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, UserOutlined, BookOutlined, EditOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;
const { Option } = Select;

const ExpertiseManagement = () => {
    const [lecturers, setLecturers] = useState([]);
    const [allCourses, setAllCourses] = useState([]); // List toàn bộ môn để chọn
    const [loading, setLoading] = useState(false);
    
    // State cho Modal Edit
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentLecturer, setCurrentLecturer] = useState(null);
    
    // Search & Selection
    const [searchText, setSearchText] = useState('');
    const [selectedCourseIds, setSelectedCourseIds] = useState([]);

    // Fetch Lecturers
    const fetchExpertise = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/lecturers');
            setLecturers(res.data);
        } catch {
            message.error("Không thể tải dữ liệu chuyên môn");
        } finally {
            setLoading(false);
        }
    };

    // Fetch All Courses (Để nạp vào dropdown chọn)
    const fetchCourses = async () => {
        try {
            const res = await axiosClient.get('/courses');
            setAllCourses(res.data);
        } catch {
            console.error("Failed to load courses");
        }
    };

    useEffect(() => {
        fetchExpertise();
        fetchCourses();
    }, []);

    // Xử lý khi bấm nút Edit
    const handleEdit = (record) => {
        setCurrentLecturer(record);
        // Lấy danh sách ID môn hiện tại
        const currentIds = record.teachingCourses.map(c => c.id);
        setSelectedCourseIds(currentIds);
        setSearchText('');
        setIsModalOpen(true);
    };

    // Xử lý lưu thay đổi
    const handleSave = async () => {
        try {
            // Map IDs to Codes
            const codes = allCourses
                .filter(c => selectedCourseIds.includes(c.id))
                .map(c => c.courseCode);

            await axiosClient.put(`/lecturers/${currentLecturer.id}/expertise`, codes);
            message.success("Đã cập nhật chuyên môn cho " + currentLecturer.fullName);
            setIsModalOpen(false);
            fetchExpertise(); 
        } catch {
            message.error("Cập nhật thất bại");
        }
    };

    // ... (Giữ nguyên phần Upload và Template cũ) ...
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/expertise/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`Đã nhập ${info.file.name} thành công`);
                fetchExpertise();
            } else if (info.file.status === 'error') {
                message.error(`Nhập ${info.file.name} thất bại`);
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
            message.error("Không thể tải file mẫu");
        }
    };
    // ... (Hết phần cũ) ...

    const columns = [
        {
            title: 'Giảng viên',
            key: 'lecturer',
            width: 250,
            sorter: (a, b) => (a.fullName || '').localeCompare(b.fullName || ''),
            sortDirections: ['ascend', 'descend'],
            render: (record) => (
                <Space>
                    <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#1890ff' }} />
                    <div>
                        <div style={{ fontWeight: 600 }}>{record.fullName}</div>
                        <div style={{ fontSize: '12px', color: '#888' }}>{record.lecturerCode}</div>
                    </div>
                </Space>
            )
        },
        {
            title: 'Khoa/Viện',
            dataIndex: ['faculty', 'name'],
            key: 'faculty',
            width: 150,
            sorter: (a, b) => (a.faculty?.name || '').localeCompare(b.faculty?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Tag color="purple">{text}</Tag>
        },
        {
            title: 'Năng lực giảng dạy',
            dataIndex: 'teachingCourses',
            key: 'courses',
            sorter: (a, b) => ( (a.teachingCourses?.length || 0) - (b.teachingCourses?.length || 0) ),
            sortDirections: ['ascend', 'descend'],
            render: (courses) => (
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                    {courses && courses.map(course => (
                        <Tag key={course.courseCode} color="blue">
                            {course.name}
                        </Tag>
                    ))}
                </div>
            )
        },
        {
            title: 'Thao tác',
            key: 'action',
            width: 80,
            align: 'center',
            render: (_, record) => (
                <Tooltip title="Sửa chuyên môn">
                    <Button 
                        type="text" 
                        icon={<EditOutlined />} 
                        onClick={() => handleEdit(record)} // Gọi hàm mở modal
                    />
                </Tooltip>
            )
        }
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Quản lý chuyên môn</Title>
                    <Text type="secondary">Phân công học phần cho giảng viên</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Mẫu
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Nhập phân công</Button>
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

            {/* Modal Edit Expertise */}
            <Modal
                title={`Sửa chuyên môn: ${currentLecturer?.fullName || ''}`}
                open={isModalOpen}
                onCancel={() => setIsModalOpen(false)}
                onOk={handleSave}
                width={800}
                bodyStyle={{ padding: 0 }}
            >
                <div style={{ padding: 16 }}>
                    <Input.Search 
                        placeholder="Tìm theo mã hoặc tên..." 
                        onChange={e => setSearchText(e.target.value)} 
                        style={{ marginBottom: 16 }}
                    />
                    <Table
                        rowKey="id"
                        columns={[
                            { title: 'Mã', dataIndex: 'courseCode', width: 120 },
                            { title: 'Tên', dataIndex: 'name' },
                            { title: 'TC', dataIndex: 'credits', width: 80 },
                        ]}
                        dataSource={allCourses.filter(c => 
                            c.courseCode.toLowerCase().includes(searchText.toLowerCase()) || 
                            c.name.toLowerCase().includes(searchText.toLowerCase())
                        )}
                        size="small"
                        pagination={{ pageSize: 10 }}
                        scroll={{ y: 400 }}
                        rowSelection={{
                            selectedRowKeys: selectedCourseIds,
                            onChange: (keys) => setSelectedCourseIds(keys),
                            preserveSelectedRowKeys: true
                        }}
                    />
                </div>
            </Modal>
        </Space>
    );
};

export default ExpertiseManagement;