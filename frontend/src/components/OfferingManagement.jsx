import React, { useState, useEffect } from 'react';
import { Upload, Button, Table, message, Card, Typography, Space, Tag, Tooltip, Modal, Select } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, ClockCircleOutlined, EnvironmentOutlined, UserSwitchOutlined, NodeIndexOutlined, EditOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const OfferingManagement = ({ user }) => {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    
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

    useEffect(() => {
        fetchData();
        fetchLecturers();
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

    const columns = [
        {
            title: 'Class Code',
            dataIndex: 'code',
            key: 'code',
            width: 230,
            render: (text) => <Text strong copyable>{text}</Text>,
        },
        // --- CỘT MỚI: TYPE & PARENT ---
        {
            title: 'Type',
            key: 'type',
            width: 150,
            align: 'center',
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
            title: 'Schedule (Output)',
            key: 'schedule',
            render: (_, record) => {
                if (record.status === 'PLANNED') {
                    return <Tag icon={<ClockCircleOutlined />} color="default">Pending Schedule</Tag>;
                }
                if (record.status === 'ERROR') {
                    return <Tag color="red">Conflict Error</Tag>;
                }
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
        </Space>
    );
};

export default OfferingManagement;