import React, { useState, useEffect } from 'react';
import { Table, Button, Select, Typography, message, Tag, Space, Input, Row, Col, Card, Tooltip } from 'antd';
import { 
    ReloadOutlined, 
    ThunderboltOutlined, 
    SearchOutlined, 
    FilterOutlined,
    ClockCircleOutlined,
    EnvironmentOutlined,
    UserOutlined
} from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;
const { Option } = Select;

const TimetableView = () => {
    // Data States
    const [offerings, setOfferings] = useState([]);
    const [rooms, setRooms] = useState([]);
    const [lecturers, setLecturers] = useState([]);
    
    // UI States
    const [loading, setLoading] = useState(false);
    const [generating, setGenerating] = useState(false);
    const [algorithm, setAlgorithm] = useState('GA');

    // Filter States
    const [filters, setFilters] = useState({
        search: '',
        room: null,
        lecturer: null,
        day: null,
        status: null,
        classType: null
    });

    // 1. Fetch Data
    const fetchData = async () => {
        setLoading(true);
        try {
            const [offerRes, roomRes, lecRes] = await Promise.all([
                axiosClient.get('/offerings'),
                axiosClient.get('/rooms'),
                axiosClient.get('/lecturers')
            ]);
            setOfferings(offerRes.data);
            setRooms(roomRes.data);
            setLecturers(lecRes.data);
        } catch {
            message.error("Failed to load schedule data");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    // 2. Trigger Algorithm
    const handleGenerate = async () => {
        setGenerating(true);
        try {
            const res = await axiosClient.post('/offerings/generate-schedule', null, {
                params: { algorithm }
            });
            message.success(res.data);
            fetchData();
        } catch {
            message.error("Scheduling failed");
        } finally {
            setGenerating(false);
        }
    };

    // 3. Filter Logic
    const filteredData = offerings.filter(item => {
        // Search by Course Name or Class Code
        const matchSearch = filters.search === '' || 
            item.code.toLowerCase().includes(filters.search.toLowerCase()) || 
            item.course.name.toLowerCase().includes(filters.search.toLowerCase());

        // Filter by Room
        const matchRoom = !filters.room || (item.room && item.room.id === filters.room);

        // Filter by Lecturer
        const matchLecturer = !filters.lecturer || (item.lecturer && item.lecturer.id === filters.lecturer);

        // Filter by Day
        const matchDay = !filters.day || item.dayOfWeek === filters.day;

        // Filter by Status
        const matchStatus = !filters.status || item.status === filters.status;

        // Filter by Class Type
        const matchType = !filters.classType || item.classType === filters.classType;

        return matchSearch && matchRoom && matchLecturer && matchDay && matchStatus && matchType;
    });

    // 4. Table Columns Definition
    const columns = [
        {
            title: 'Class Info',
            dataIndex: 'code',
            key: 'code',
            width: 280,
            render: (text, record) => (
                <div>
                    <Space>
                        <Text strong style={{ color: '#1890ff' }}>{text}</Text>
                        {record.classType === 'LT' && <Tag color="blue">LT</Tag>}
                        {record.classType === 'TH' && <Tag color="cyan">TH</Tag>}
                        {record.classType === 'ELN' && <Tag color="purple">ELN</Tag>}
                    </Space>
                    <div style={{ fontSize: '13px', fontWeight: 500 }}>{record.course.name}</div>
                    <Text type="secondary" style={{ fontSize: '12px' }}>{record.targetClasses}</Text>
                    {record.parent && (
                        <div style={{ fontSize: '12px', color: '#8c8c8c' }}>
                            <span style={{ marginRight: 4 }}>↳</span>
                            Parent: {record.parent.code}
                        </div>
                    )}
                </div>
            )
        },
        {
            title: 'Semester',
            dataIndex: ['semester', 'name'],
            key: 'semester',
            width: 100,
            render: (text) => text ? <Tag>{text}</Tag> : '-'
        },
        {
            title: 'Schedule Time',
            key: 'time',
            width: 200,
            render: (_, record) => {
                if (!record.dayOfWeek) return <Tag>Unscheduled</Tag>;
                
                const days = {2: 'Mon', 3: 'Tue', 4: 'Wed', 5: 'Thu', 6: 'Fri', 7: 'Sat', 8: 'Sun'};
                const dayName = days[record.dayOfWeek];
                let color = 'blue';
                if (record.dayOfWeek === 8) color = 'orange'; // CN

                return (
                    <Space direction="vertical" size={0}>
                        <Tag color={color} icon={<ClockCircleOutlined />}>
                            {dayName}, Tiết {record.startPeriod} - {record.endPeriod}
                        </Tag>
                    </Space>
                );
            }
        },
        {
            title: 'Location (Room)',
            dataIndex: ['room', 'name'],
            key: 'room',
            width: 150,
            render: (text, record) => text ? (
                <Tag color="geekblue" icon={<EnvironmentOutlined />}>
                    {text} <span style={{ opacity: 0.6 }}>({record.room.type})</span>
                </Tag>
            ) : <Text type="secondary">-</Text>
        },
        {
            title: 'Lecturer',
            dataIndex: ['lecturer', 'fullName'],
            key: 'lecturer',
            render: (text) => text ? (
                <Space>
                    <UserOutlined style={{ color: '#52c41a' }} /> {text}
                </Space>
            ) : <Text type="secondary" italic>Auto-assign</Text>
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            align: 'center',
            width: 120,
            render: (status, record) => {
                let color = 'default';
                if (status === 'SCHEDULED') color = 'success';
                if (status === 'ERROR') color = 'error';
                if (status === 'PLANNED') color = 'processing';
                return (
                    <Tooltip title={record.statusMessage}>
                        <Tag color={color} style={{ cursor: 'help' }}>{status}</Tag>
                    </Tooltip>
                );
            }
        }
    ];

    return (
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {/* --- Header & Actions --- */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Master Timetable</Title>
                    <Text type="secondary">Manage and Monitor all scheduled classes</Text>
                </div>
                <Space>
                    <Select 
                        value={algorithm}
                        style={{ width: 190 }}
                        onChange={setAlgorithm}
                    >
                        <Option value="GA">Genetic Algorithm (GA)</Option>
                        <Option value="HEURISTIC">Heuristic Greedy</Option>
                    </Select>
                    <Button 
                        type="primary" 
                        icon={<ThunderboltOutlined />} 
                        loading={generating}
                        onClick={handleGenerate}
                        style={{ fontWeight: 500 }}
                    >
                        Run Auto-Schedule
                    </Button>
                    <Button icon={<ReloadOutlined />} onClick={fetchData}>Refresh</Button>
                </Space>
            </div>

            {/* --- Filter Toolbar --- */}
            <Card bodyStyle={{ padding: '16px' }} style={{ background: '#fcfcfc' }}>
                <Row gutter={[16, 16]} align="middle">
                    <Col xs={24} sm={6}>
                        <Input 
                            prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />} 
                            placeholder="Search Class Code / Course Name..." 
                            allowClear
                            onChange={(e) => setFilters({ ...filters, search: e.target.value })}
                        />
                    </Col>
                    <Col xs={24} sm={4}>
                        <Select 
                            placeholder="Filter by Room" 
                            style={{ width: '100%' }} 
                            allowClear
                            showSearch
                            optionFilterProp="children"
                            onChange={(val) => setFilters({ ...filters, room: val })}
                        >
                            {rooms.map(r => <Option key={r.id} value={r.id}>{r.name}</Option>)}
                        </Select>
                    </Col>
                    <Col xs={24} sm={5}>
                        <Select 
                            placeholder="Filter by Lecturer" 
                            style={{ width: '100%' }} 
                            allowClear
                            showSearch
                            optionFilterProp="children"
                            onChange={(val) => setFilters({ ...filters, lecturer: val })}
                        >
                            {lecturers.map(l => <Option key={l.id} value={l.id}>{l.fullName}</Option>)}
                        </Select>
                    </Col>
                    <Col xs={12} sm={4}>
                        <Select 
                            placeholder="Day" 
                            style={{ width: '100%' }} 
                            allowClear
                            onChange={(val) => setFilters({ ...filters, day: val })}
                        >
                            <Option value={2}>Monday</Option>
                            <Option value={3}>Tuesday</Option>
                            <Option value={4}>Wednesday</Option>
                            <Option value={5}>Thursday</Option>
                            <Option value={6}>Friday</Option>
                            <Option value={7}>Saturday</Option>
                            <Option value={8}>Sunday</Option>
                        </Select>
                    </Col>
                    <Col xs={12} sm={4}>
                        <Select 
                            placeholder="Status" 
                            style={{ width: '100%' }} 
                            allowClear
                            onChange={(val) => setFilters({ ...filters, status: val })}
                        >
                            <Option value="SCHEDULED">Scheduled (OK)</Option>
                            <Option value="ERROR">Error (Conflict)</Option>
                            <Option value="PLANNED">Pending</Option>
                        </Select>
                    </Col>
                    <Col xs={12} sm={4}>
                        <Select 
                            placeholder="Type" 
                            style={{ width: '100%' }} 
                            allowClear
                            onChange={(val) => setFilters({ ...filters, classType: val })}
                        >
                            <Option value="LT">Lý thuyết (LT)</Option>
                            <Option value="TH">Thực hành (TH)</Option>
                            <Option value="ELN">E-Learning (ELN)</Option>
                            <Option value="ALL">All Types</Option>
                        </Select>
                    </Col>
                </Row>
            </Card>

            {/* --- Data Table --- */}
            <Table 
                columns={columns} 
                dataSource={filteredData} 
                rowKey="id"
                loading={loading}
                pagination={{ 
                    pageSize: 10, 
                    showSizeChanger: true, 
                    showTotal: (total) => `Total ${total} classes` 
                }}
                size="middle"
                bordered
            />
        </Space>
    );
};

export default TimetableView;