import React, { useState, useEffect } from 'react';
import { Table, Button, Upload, Card, message, Typography, Space, Tag, Tooltip, Modal, Form, Input, InputNumber, Select, Popconfirm, Radio } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const CourseManagement = () => {
    const [courses, setCourses] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();
    const [faculties, setFaculties] = useState([]);
    const [schools, setSchools] = useState([]);
    const [managedBy, setManagedBy] = useState('faculty');

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
        fetchFaculties();
        fetchSchools();
    }, []);

    const fetchFaculties = async () => {
        try { const res = await axiosClient.get('/faculties'); setFaculties(res.data); } catch { /* empty */ }
    };

    const fetchSchools = async () => {
        try { const res = await axiosClient.get('/schools'); setSchools(res.data); } catch { /* empty */ }
    };

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/courses/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
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
            width: 150,
            sorter: (a, b) => (a.courseCode || '').localeCompare(b.courseCode || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong>{text}</Text>
        },
        {
            title: 'Course Name',
            dataIndex: 'name',
            key: 'name',
            sorter: (a, b) => (a.name || '').localeCompare(b.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <span style={{ fontWeight: 500 }}>{text}</span>
        },
        {
            title: 'Credits',
            key: 'credits',
            sorter: (a, b) => (a.credits || 0) - (b.credits || 0),
            sortDirections: ['ascend', 'descend'],
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
            title: 'Managing Unit',
            key: 'managingUnit',
            sorter: (a, b) => ((a.managingFaculty?.name || a.school?.name) || '').localeCompare((b.managingFaculty?.name || b.school?.name) || ''),
            sortDirections: ['ascend', 'descend'],
            render: (_, record) => {
                if (record.managingFaculty) return <Tag color="cyan">{record.managingFaculty.name}</Tag>;
                if (record.school) return <Tag color="purple">{record.school.name}</Tag>;
                return <Tag>Unassigned</Tag>;
            }
        }
        ,{
            title: 'Actions', key: 'actions', width: 180, render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>Edit</Button>
                    <Popconfirm title="Delete this course?" onConfirm={() => onDelete(record.id)}>
                        <Button danger size="small" icon={<DeleteOutlined />}>Delete</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    const openCreate = () => { 
        setEditing(null); 
        form.resetFields(); 
        setManagedBy('faculty');
        setModalVisible(true); 
    };

    const onEdit = (record) => {
        setEditing(record);
        const isSchool = !!record.school;
        setManagedBy(isSchool ? 'school' : 'faculty');
        form.setFieldsValue({ 
            courseCode: record.courseCode, 
            name: record.name, 
            credits: record.credits, 
            theoryCredits: record.theoryCredits, 
            practiceCredits: record.practiceCredits, 
            managedBy: isSchool ? 'school' : 'faculty',
            managingFaculty: record.managingFaculty?.id,
            school: record.school?.id
        });
        setModalVisible(true);
    };

    const onDelete = async (id) => { try { await axiosClient.delete(`/courses/${id}`); message.success('Deleted'); fetchCourses(); } catch { message.error('Delete failed'); } };

    const onFinish = async (values) => {
        try {
            const payload = { ...values };
            if (values.managedBy === 'faculty') {
                payload.managingFaculty = { id: values.managingFaculty };
                payload.school = null;
            } else {
                payload.school = { id: values.school };
                payload.managingFaculty = null;
            }
            delete payload.managedBy;
            
            if (editing) { await axiosClient.put(`/courses/${editing.id}`, payload); message.success('Updated'); }
            else { await axiosClient.post('/courses', payload); message.success('Created'); }
            setModalVisible(false); fetchCourses();
        } catch { message.error('Save failed'); }
    };

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
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>New</Button>
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

            <Modal title={editing ? 'Edit Course' : 'Create Course'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnClose>
                <Form form={form} layout="vertical" onFinish={onFinish}>
                    <Form.Item name="courseCode" label="Course Code" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="name" label="Course Name" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>
                    <Form.Item name="credits" label="Credits">
                        <InputNumber style={{ width: '100%' }} step={0.1} />
                    </Form.Item>
                    <Form.Item name="theoryCredits" label="Theory Credits">
                        <InputNumber style={{ width: '100%' }} step={0.1} />
                    </Form.Item>
                    <Form.Item name="practiceCredits" label="Practice Credits">
                        <InputNumber style={{ width: '100%' }} step={0.1} />
                    </Form.Item>
                    <Form.Item name="managedBy" label="Managed By" initialValue="faculty">
                        <Radio.Group onChange={e => setManagedBy(e.target.value)}>
                            <Radio value="faculty">Faculty</Radio>
                            <Radio value="school">School</Radio>
                        </Radio.Group>
                    </Form.Item>

                    {managedBy === 'faculty' ? (
                        <Form.Item name="managingFaculty" label="Faculty" rules={[{ required: true }]}>
                            <Select placeholder="Select Faculty">
                                {faculties.map(f => <Select.Option key={f.id} value={f.id}>{f.name}</Select.Option>)}
                            </Select>
                        </Form.Item>
                    ) : (
                        <Form.Item name="school" label="School" rules={[{ required: true }]}>
                            <Select placeholder="Select School">
                                {schools.map(s => <Select.Option key={s.id} value={s.id}>{s.name}</Select.Option>)}
                            </Select>
                        </Form.Item>
                    )}
                </Form>
            </Modal>
        </Space>
    );
};

export default CourseManagement;