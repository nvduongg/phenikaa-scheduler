import React, { useState, useEffect } from 'react';
import { Upload, Button, Table, message, Card, Typography, Space, Tag, Tooltip, Modal, Select, Form, Input, Popconfirm } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, UserSwitchOutlined, NodeIndexOutlined, EditOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const OfferingManagement = ({ user }) => {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [courses, setCourses] = useState([]);
    const [adminClasses, setAdminClasses] = useState([]);
    const [parentOptions, setParentOptions] = useState([]);

    // CRUD modal state
    const [editModalVisible, setEditModalVisible] = useState(false);
    const [editingOffering, setEditingOffering] = useState(null);
    const [form] = Form.useForm();
    
    // Assign Lecturer State
    const [lecturers, setLecturers] = useState([]);
    const [assignModalVisible, setAssignModalVisible] = useState(false);
    const [selectedOffering, setSelectedOffering] = useState(null);
    const [selectedLecturerId, setSelectedLecturerId] = useState(null);

    const fetchData = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get('/offerings');
            // Sắp xếp để lớp Mẹ (LT) nằm gần lớp Con (TH) cho dễ nhìn
            const sortedData = response.data.sort((a, b) => a.code.localeCompare(b.code));
            setData(sortedData);
        } catch (error) {
            message.error('Failed to load data: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const fetchLecturers = async () => {
        try {
            const res = await axiosClient.get('/lecturers');
            setLecturers(res.data);
        } catch (error) {
            console.error("Failed to fetch lecturers", error);
        }
    };

    const fetchCourses = async () => {
        try {
            const res = await axiosClient.get('/courses');
            setCourses(res.data);
        } catch (error) {
            console.error('Failed to fetch courses', error);
        }
    };

    const fetchAdminClasses = async () => {
        try {
            const res = await axiosClient.get('/admin-classes');
            setAdminClasses(res.data);
        } catch (error) {
            console.error('Failed to fetch admin classes', error);
        }
    };

    useEffect(() => {
        fetchData();
        fetchLecturers();
        fetchCourses();
        fetchAdminClasses();
    }, []);

    const uploadProps = {
        name: 'file',
        action: 'http://localhost:8080/api/v1/offerings/import',
        headers: { Authorization: `Bearer ${JSON.parse(localStorage.getItem('user'))?.token}` },
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
            link.setAttribute('download', 'Offering_Plan_Template_v2.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error('Failed to download template');
        }
    };

    const handleAutoAssign = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.post('/offerings/auto-assign-lecturers');
            message.success(res.data);
            fetchData(); 
        } catch (error) {
            message.error("Auto-assign failed: " + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleOpenAssignModal = (record) => {
        setSelectedOffering(record);
        setSelectedLecturerId(record.lecturer ? record.lecturer.id : null);
        setAssignModalVisible(true);
    };

    const handleAssignLecturer = async () => {
        try {
            await axiosClient.put(`/offerings/${selectedOffering.id}/lecturer`, null, {
                params: { lecturerId: selectedLecturerId }
            });
            message.success("Assigned lecturer successfully");
            setAssignModalVisible(false);
            fetchData();
        } catch {
            message.error("Failed to assign lecturer");
        }
    };

    const openCreateModal = () => {
        setEditingOffering(null);
        form.resetFields();
        setParentOptions([]);
        setEditModalVisible(true);
    };

    const openEditModal = (record) => {
        setEditingOffering(record);
        const targetCodes = (record.targetClasses || '')
            .split(';')
            .map(s => s.trim())
            .filter(Boolean);
        form.setFieldsValue({
            code: record.code,
            courseId: record.course?.id,
            plannedSize: record.plannedSize,
            targetClasses: targetCodes,
            classType: record.classType || 'ALL',
            parentCode: record.parent?.code,
        });
        // Build parent options: same course, LT/ALL/ELN, exclude current
        const opts = data
            .filter(off =>
                off.course &&
                record.course &&
                off.course.id === record.course.id &&
                ['LT', 'ALL', 'ELN'].includes((off.classType || 'ALL').toUpperCase()) &&
                off.id !== record.id
            )
            .map(off => ({
                value: off.code,
                label: `${off.code}${off.targetClasses ? ` - ${off.targetClasses}` : ''}`,
            }));
        setParentOptions(opts);
        setEditModalVisible(true);
    };

    const handleSaveOffering = async () => {
        try {
            const values = await form.validateFields();
            const payload = {
                code: values.code,
                plannedSize: values.plannedSize,
                targetClasses: Array.isArray(values.targetClasses)
                    ? values.targetClasses.join(';')
                    : (values.targetClasses || ''),
                classType: values.classType,
                course: { id: values.courseId },
            };

            if (values.parentCode) {
                payload.parent = { code: values.parentCode };
            }

            if (editingOffering) {
                await axiosClient.put(`/offerings/${editingOffering.id}`, payload);
                message.success('Updated offering successfully');
            } else {
                await axiosClient.post('/offerings', payload);
                message.success('Created offering successfully');
            }

            setEditModalVisible(false);
            fetchData();
        } catch (error) {
            if (error?.errorFields) return; // validation error
            message.error('Failed to save offering');
        }
    };

    const handleDeleteOffering = async (record) => {
        try {
            await axiosClient.delete(`/offerings/${record.id}`);
            message.success('Deleted offering');
            fetchData();
        } catch {
            message.error('Failed to delete offering');
        }
    };

    const columns = [
        {
            title: 'Class Code',
            dataIndex: 'code',
            key: 'code',
            width: 230,
            sorter: (a, b) => (a.code || '').localeCompare(b.code || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong copyable>{text}</Text>,
        },
        // --- CỘT MỚI: TYPE & PARENT ---
        {
            title: 'Type',
            key: 'type',
            width: 150,
            align: 'center',
            sorter: (a, b) => ((a.classType || '')).localeCompare(b.classType || ''),
            sortDirections: ['ascend', 'descend'],
            render: (_, record) => {
                let color = 'default';
                let typeText = record.classType || 'ALL';

                if (typeText === 'LT') color = 'purple';
                if (typeText === 'TH') color = 'orange';
                if (typeText === 'ELN') color = 'cyan';

                return (
                    <Space direction="vertical" size={0}>
                        <Tag color={color} style={{ fontWeight: 600 }}>{typeText}</Tag>
                        
                        {/* Nếu là lớp TH và có Parent, hiển thị mã Parent */}
                        {typeText === 'TH' && record.parent && (
                            <Tooltip title={`Parent Class: ${record.parent.code}`}>
                                <div style={{ fontSize: '11px', color: '#888', marginTop: 4 }}>
                                    <NodeIndexOutlined /> Parent: {record.parent.code.split('-').pop()}...
                                </div>
                            </Tooltip>
                        )}
                    </Space>
                );
            }
        },
        // -----------------------------
        {
            title: 'Course',
            dataIndex: 'course',
            key: 'course',
            sorter: (a, b) => ((a.course?.name || '')).localeCompare(b.course?.name || ''),
            sortDirections: ['ascend', 'descend'],
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
            sorter: (a, b) => (a.plannedSize || 0) - (b.plannedSize || 0),
            sortDirections: ['ascend', 'descend'],
            render: (size) => <Tag color="volcano">{size}</Tag>
        },
        {
            title: 'Assigned Lecturer',
            dataIndex: 'lecturer',
            key: 'lecturer',
            sorter: (a, b) => ((a.lecturer?.fullName || '')).localeCompare(b.lecturer?.fullName || ''),
            sortDirections: ['ascend', 'descend'],
            render: (lecturer, record) => {
                // Logic check quyền
                const myFacultyId = user?.facultyId;
                const managingFacultyId = record.course.managingFaculty?.id;
                
                // Có được quyền gán GV không?
                // Chỉ được gán khi môn học thuộc khoa của mình
                const canAssign = (user?.role === 'ADMIN_TRUONG') || 
                                  (myFacultyId === managingFacultyId);

                return (
                    <Space>
                        {lecturer ? <Tag color="blue">{lecturer.fullName}</Tag> : <Text type="secondary">Chưa gán</Text>}
                        
                        {/* Chỉ hiện nút Gán/Sửa nếu có quyền */}
                        {canAssign && (
                            <Button size="small" icon={<EditOutlined />} onClick={() => handleOpenAssignModal(record)}>
                                {lecturer ? 'Đổi GV' : 'Gán GV'}
                            </Button>
                        )}
                        
                        {!canAssign && !lecturer && (
                            <Tooltip title={`Chờ khoa ${record.course.managingFaculty?.name} phân công`}>
                                <Tag color="warning">Chờ Khoa khác</Tag>
                            </Tooltip>
                        )}
                    </Space>
                );
            }
        },
        {
            title: 'Actions',
            key: 'actions',
            width: 160,
            render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => openEditModal(record)}>
                        Edit
                    </Button>
                    <Popconfirm
                        title="Delete offering"
                        description={`Are you sure to delete ${record.code}?`}
                        onConfirm={() => handleDeleteOffering(record)}
                    >
                        <Button size="small" danger icon={<DeleteOutlined />}>Delete</Button>
                    </Popconfirm>
                </Space>
            )
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
                    <Button
                        type="primary"
                        icon={<PlusOutlined />}
                        onClick={openCreateModal}
                    >
                        New Offering
                    </Button>
                    <Button 
                        type="primary" 
                        danger // Đổi màu đỏ cho nổi bật
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

            <Modal
                title={`Assign Lecturer for ${selectedOffering?.code}`}
                open={assignModalVisible}
                onOk={handleAssignLecturer}
                onCancel={() => setAssignModalVisible(false)}
            >
                <Text>Select Lecturer:</Text>
                <Select
                    style={{ width: '100%', marginTop: 8 }}
                    placeholder="Select a lecturer"
                    value={selectedLecturerId}
                    onChange={setSelectedLecturerId}
                    showSearch
                    optionFilterProp="children"
                    allowClear
                    filterOption={(input, option) =>
                        (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                    }
                >
                    {lecturers.map(lec => (
                        <Select.Option key={lec.id} value={lec.id}>
                            {lec.fullName} ({lec.lecturerCode}) - {lec.faculty?.name}
                        </Select.Option>
                    ))}
                </Select>
            </Modal>

            <Modal
                title={editingOffering ? `Edit Offering: ${editingOffering.code}` : 'New Offering'}
                open={editModalVisible}
                onOk={handleSaveOffering}
                onCancel={() => setEditModalVisible(false)}
                destroyOnClose
            >
                <Form form={form} layout="vertical">
                    <Form.Item
                        label="Class Code"
                        name="code"
                        rules={[{ required: true, message: 'Please input class code' }]}
                    >
                        <Input placeholder="2025_JAVA_01" />
                    </Form.Item>

                    <Form.Item
                        label="Course"
                        name="courseId"
                        rules={[{ required: true, message: 'Please select course' }]}
                    >
                        <Select
                            showSearch
                            placeholder="Select course"
                            optionFilterProp="children"
                            filterOption={(input, option) =>
                                (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                            }
                            onChange={(val) => {
                                // When course changes, rebuild parent options for that course
                                const opts = data
                                    .filter(off =>
                                        off.course &&
                                        off.course.id === val &&
                                        ['LT', 'ALL', 'ELN'].includes((off.classType || 'ALL').toUpperCase()) &&
                                        (!editingOffering || off.id !== editingOffering.id)
                                    )
                                    .map(off => ({
                                        value: off.code,
                                        label: `${off.code}${off.targetClasses ? ` - ${off.targetClasses}` : ''}`,
                                    }));
                                setParentOptions(opts);
                                // Reset parent selection when course changes
                                form.setFieldsValue({ parentCode: undefined });
                            }}
                        >
                            {courses.map(c => (
                                <Select.Option key={c.id} value={c.id}>
                                    {c.courseCode} - {c.name}
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>

                    <Form.Item
                        label="Planned Size"
                        name="plannedSize"
                        rules={[{ required: true, message: 'Please input planned size' }]}
                    >
                        <Input type="number" min={1} placeholder="60" />
                    </Form.Item>

                    <Form.Item
                        label="Target Classes"
                        name="targetClasses"
                    >
                        <Select
                            mode="multiple"
                            allowClear
                            placeholder="Select target admin classes"
                            showSearch
                            optionFilterProp="children"
                            filterOption={(input, option) =>
                                (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                            }
                        >
                            {adminClasses.map(cls => (
                                <Select.Option key={cls.id} value={cls.name}>
                                    {cls.name} {cls.code ? `(${cls.code})` : ''}
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>

                    <Form.Item
                        label="Class Type"
                        name="classType"
                        initialValue="ALL"
                    >
                        <Select>
                            <Select.Option value="ALL">ALL</Select.Option>
                            <Select.Option value="LT">LT</Select.Option>
                            <Select.Option value="TH">TH</Select.Option>
                            <Select.Option value="ELN">ELN</Select.Option>
                        </Select>
                    </Form.Item>

                    <Form.Item
                        label="Parent Class Code (for TH)"
                        name="parentCode"
                    >
                        <Select
                            allowClear
                            placeholder="Select parent LT/ALL/ELN class"
                            showSearch
                            optionFilterProp="children"
                            filterOption={(input, option) =>
                                (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                            }
                        >
                            {parentOptions.map(opt => (
                                <Select.Option key={opt.value} value={opt.value}>
                                    {opt.label}
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
};

export default OfferingManagement;