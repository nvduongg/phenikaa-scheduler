import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Modal, Form, Input, Select, Popconfirm } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, UserOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const LecturerManagement = () => {
    const [lecturers, setLecturers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();
    const [faculties, setFaculties] = useState([]);
    
    // Expertise Selection State
    const [courses, setCourses] = useState([]);
    const [expertiseModalVisible, setExpertiseModalVisible] = useState(false);
    const [selectedCourseIds, setSelectedCourseIds] = useState([]);
    const [searchText, setSearchText] = useState('');

    // Fetch
    const fetchLecturers = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.get('/lecturers');
            setLecturers(res.data);
        } catch {
            message.error("Failed to fetch lecturers");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLecturers();
        fetchFaculties();
        fetchCourses();
    }, []);

    const fetchFaculties = async () => {
        try { const res = await axiosClient.get('/faculties'); setFaculties(res.data); } catch {}
    };

    const fetchCourses = async () => {
        try { const res = await axiosClient.get('/courses'); setCourses(res.data); } catch {}
    };

    // Upload
    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/lecturers/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
        showUploadList: false,
        onChange(info) {
            if (info.file.status === 'done') {
                message.success(`${info.file.name} imported successfully`);
                fetchLecturers();
            } else if (info.file.status === 'error') {
                message.error(`${info.file.name} import failed`);
            }
        },
    };

    // Template
    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/lecturers/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Lecturer_Import_Template.xlsx');
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
            title: 'Code',
            dataIndex: 'lecturerCode',
            key: 'code',
            width: 100,
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Full Name',
            dataIndex: 'fullName',
            key: 'name',
            render: (text) => (
                <Text>{text}</Text>
            )
        },
        {
            title: 'Email',
            dataIndex: 'email',
            key: 'email',
            render: (text) => <Text copyable>{text}</Text>
        },
        {
            title: 'Faculty',
            dataIndex: ['faculty', 'name'],
            key: 'faculty',
            render: (text) => <Tag color="purple">{text}</Tag>
        },
        {
            title: 'Expertise',
            dataIndex: 'teachingCourses',
            key: 'expertise',
            render: (courses) => (
                <Space size={[0, 4]} wrap>
                    {courses && courses.slice(0, 3).map(c => <Tag key={c.id}>{c.courseCode}</Tag>)}
                    {courses && courses.length > 3 && <Tag>+{courses.length - 3} more</Tag>}
                </Space>
            )
        }
        ,{
            title: 'Actions', key: 'actions', width: 180, render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Edit</Button>
                    <Popconfirm title="Delete this lecturer?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Delete</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => {
        setEditing(null); 
        form.resetFields(); 
        setSelectedCourseIds([]);
        setModalVisible(true); 
    };

    const onEdit = (record) => { 
        setEditing(record); 
        form.setFieldsValue({ 
            lecturerCode: record.lecturerCode, 
            fullName: record.fullName, 
            email: record.email, 
            faculty: record.faculty?.id 
        }); 
        setSelectedCourseIds(record.teachingCourses ? record.teachingCourses.map(c => c.id) : []);
        setModalVisible(true); 
    };

    const onDelete = async (id) => { try { await axiosClient.delete(`/lecturers/${id}`); message.success('Deleted'); fetchLecturers(); } catch { message.error('Delete failed'); } };

    const onFinish = async (values) => {
        try {
            const payload = { ...values };
            if (values.faculty) payload.faculty = { id: values.faculty };
            // Attach selected courses
            payload.teachingCourses = selectedCourseIds.map(id => ({ id }));
            
            if (editing) { await axiosClient.put(`/lecturers/${editing.id}`, payload); message.success('Updated'); }
            else { await axiosClient.post('/lecturers', payload); message.success('Created'); }
            setModalVisible(false); fetchLecturers();
        } catch { message.error('Save failed'); }
    };

    // Filtered courses for modal
    const filteredCourses = courses.filter(c => 
        c.courseCode.toLowerCase().includes(searchText.toLowerCase()) || 
        c.name.toLowerCase().includes(searchText.toLowerCase())
    );

    const courseColumns = [
        { title: 'Code', dataIndex: 'courseCode', width: 120 },
        { title: 'Name', dataIndex: 'name' },
        { title: 'Credits', dataIndex: 'credits', width: 80 },
    ];

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Lecturer Management</Title>
                    <Text type="secondary">Manage Academic Staff (Quản lý giảng viên)</Text>
                </div>
                <Space>
                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Template
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>New</Button>
                    <Upload {...uploadProps}>
                        <Button type="primary" icon={<UploadOutlined />}>Import Excel</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchLecturers} />
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

            <Modal title={editing ? 'Edit Lecturer' : 'Create Lecturer'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="lecturerCode" label="Lecturer Code" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="fullName" label="Full Name" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="email" label="Email">
                        <Input />
                    </Form.Item>
                    <Form.Item name="faculty" label="Faculty" rules={[{ required: true }]}>
                        <Select>
                            {faculties.map(f => <Select.Option key={f.id} value={f.id}>{f.name}</Select.Option>)}
                        </Select>
                    </Form.Item>
                    
                    <Form.Item label="Expertise (Teaching Courses)">
                        <Button type="dashed" onClick={() => setExpertiseModalVisible(true)} style={{ width: '100%', marginBottom: 8 }}>
                            Select Courses ({selectedCourseIds.length} selected)
                        </Button>
                        <div style={{ maxHeight: 100, overflowY: 'auto', border: '1px solid #f0f0f0', padding: 8, borderRadius: 4 }}>
                            {selectedCourseIds.length > 0 ? (
                                <Space size={[0, 4]} wrap>
                                    {courses.filter(c => selectedCourseIds.includes(c.id)).map(c => (
                                        <Tag closable onClose={() => setSelectedCourseIds(prev => prev.filter(id => id !== c.id))} key={c.id}>
                                            {c.courseCode}
                                        </Tag>
                                    ))}
                                </Space>
                            ) : <Text type="secondary">No courses selected</Text>}
                        </div>
                    </Form.Item>
                </Form>
            </Modal>

            {/* Expertise Selection Modal */}
            <Modal
                title="Select Expertise Courses"
                open={expertiseModalVisible}
                onOk={() => setExpertiseModalVisible(false)}
                onCancel={() => setExpertiseModalVisible(false)}
                width={800}
                bodyStyle={{ padding: 0 }}
            >
                <div style={{ padding: 16 }}>
                    <Input.Search 
                        placeholder="Search by code or name..." 
                        onChange={e => setSearchText(e.target.value)} 
                        style={{ marginBottom: 16 }}
                    />
                    <Table
                        rowKey="id"
                        columns={courseColumns}
                        dataSource={filteredCourses}
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

export default LecturerManagement;