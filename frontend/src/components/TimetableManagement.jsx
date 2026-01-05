import React, { useState, useEffect } from 'react';
import { Table, Button, Select, Typography, message, Tag, Space, Input, Tooltip, Modal, Card } from 'antd';
import { 
    DownloadOutlined,
    ThunderboltOutlined, 
    SearchOutlined, 
    ClockCircleOutlined,
    EnvironmentOutlined,
    UserOutlined,
    CalendarOutlined
} from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;
const { Option } = Select;

const TimetableManagement = () => {
    // Data States
    const [offerings, setOfferings] = useState([]);
    const [semesters, setSemesters] = useState([]);
    
    // UI States
    const [loading, setLoading] = useState(false);
    const [generating, setGenerating] = useState(false);
    const [semesterModalOpen, setSemesterModalOpen] = useState(false);
    const [updatingSemester, setUpdatingSemester] = useState(false);
    const [selectedSemesterId, setSelectedSemesterId] = useState(null);
    const [exporting, setExporting] = useState(false);

    // Filter States
    const [filters, setFilters] = useState({
        search: ''
    });

    // 1. Fetch Data
    const fetchData = async () => {
        setLoading(true);
        try {
            const offerRes = await axiosClient.get('/offerings');
            const offeringData = offerRes.data;
            setOfferings(offeringData);
        } catch {
            message.error("Không thể tải dữ liệu thời khóa biểu");
        } finally {
            setLoading(false);
        }
    };

    const fetchSemesters = async () => {
        try {
            const res = await axiosClient.get('/semesters');
            const list = res.data || [];
            setSemesters(list);
            const current = list.find(s => s.isCurrent);
            if (current) setSelectedSemesterId(current.id);
        } catch (e) {
            console.error('Failed to load semesters', e);
        }
    };

    useEffect(() => {
        fetchData();
        fetchSemesters();
    }, []);

    // 2. Trigger Algorithm
    const handleGenerate = async () => {
        setGenerating(true);
        try {
            const res = await axiosClient.post('/offerings/generate-schedule');
            message.success(res.data);
            fetchData();
        } catch {
            message.error("Xếp lịch thất bại");
        } finally {
            setGenerating(false);
        }
    };

    const handleSetCurrentSemester = async () => {
        if (!selectedSemesterId) return;
        setUpdatingSemester(true);
        try {
            await axiosClient.post(`/semesters/${selectedSemesterId}/set-current`);
            message.success('Đã cập nhật học kỳ hiện hành');
            setSemesterModalOpen(false);
            fetchSemesters();
            fetchData();
        } catch {
            message.error('Không thể cập nhật học kỳ hiện hành');
        } finally {
            setUpdatingSemester(false);
        }
    };

    const handleExport = async () => {
        setExporting(true);
        try {
            const res = await axiosClient.get('/timetable/export', {
                params: selectedSemesterId ? { semesterId: selectedSemesterId } : undefined,
                responseType: 'blob',
            });

            const disposition = res.headers?.['content-disposition'] || res.headers?.['Content-Disposition'];
            let filename = 'Timetable.xlsx';
            if (disposition && typeof disposition === 'string') {
                        const match = disposition.match(/filename\*?=(?:UTF-8''|")?([^;"\n]+)/i);
                if (match && match[1]) {
                    filename = decodeURIComponent(match[1].replace(/"/g, '').trim());
                }
            }

            const blobUrl = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = blobUrl;
            link.setAttribute('download', filename);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(blobUrl);
        } catch (e) {
            console.error(e);
            message.error('Xuất file thất bại');
        } finally {
            setExporting(false);
        }
    };

    // 3. Filter Logic (only search)
    const filteredData = offerings.filter(item => {
        if (!filters.search) return true;
        const keyword = filters.search.toLowerCase();
        const codeMatch = (item.code || '').toLowerCase().includes(keyword);
        const courseMatch = (item.course?.name || '').toLowerCase().includes(keyword);
        const classMatch = (item.targetClasses || '').toLowerCase().includes(keyword);
        return codeMatch || courseMatch || classMatch;
    });
    
    const activeSemester = semesters.find(s => s.isCurrent);

    // 4. Table Columns Definition
    const columns = [
        {
            title: 'Lớp học phần',
            align: 'left',
            dataIndex: 'code',
            key: 'code',
            width: 380,
            sorter: (a, b) => (a.code || '').localeCompare(b.code || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text, record) => (
                <div>
                    <Text strong style={{ color: '#1890ff' }}>{text}</Text>
                    {record.parent && (
                        <div style={{ fontSize: '11px', color: '#8c8c8c' }}>
                            <span style={{ marginRight: 4 }}>↳</span>
                            Lớp cha: {record.parent.code}
                        </div>
                    )}
                </div>
            )
        },
        {
            title: 'Loại',
            dataIndex: 'classType',
            key: 'classType',
            width: 80,
            align: 'center',
            sorter: (a, b) => (a.classType || '').localeCompare(b.classType || ''),
            sortDirections: ['ascend', 'descend'],
            render: (type) => {
                if (type === 'LT') return <Tag color="blue">LT</Tag>;
                if (type === 'TH') return <Tag color="cyan">TH</Tag>;
                if (type === 'ELN') return <Tag color="purple">ELN</Tag>;
                return <Tag>{type}</Tag>;
            }
        },
        {
            title: 'Học phần',
            key: 'course',
            width: 260,
            sorter: (a, b) => ((a.course?.name || '')).localeCompare(b.course?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (_, record) => (
                <div>
                    <div style={{ fontSize: '13px', fontWeight: 500 }}>{record.course?.name}</div>
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                        {record.course?.courseCode}
                    </Text>
                </div>
            )
        },
        {
            title: 'Lớp sinh viên',
            dataIndex: 'targetClasses',
            key: 'targetClasses',
            width: 200,
            sorter: (a, b) => (a.targetClasses || '').localeCompare(b.targetClasses || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => (
                <Tooltip title={text}>
                    <Text type="secondary" style={{ fontSize: '12px' }} ellipsis>
                        {text}
                    </Text>
                </Tooltip>
            )
        },
        {
            title: 'Học kỳ',
            dataIndex: ['semester', 'name'],
            key: 'semester',
            width: 100,
            sorter: (a, b) => (a.semester?.name || '').localeCompare(b.semester?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => text ? <Tag>{text}</Tag> : '-'
        },
        {
            title: 'Thời gian học',
            key: 'time',
            width: 200,
            sorter: (a, b) => {
                const da = a.dayOfWeek || 0;
                const db = b.dayOfWeek || 0;
                if (da !== db) return da - db;
                return (a.startPeriod || 0) - (b.startPeriod || 0);
            },
            sortDirections: ['ascend', 'descend'],
            render: (_, record) => {
                if (!record.dayOfWeek) return <Tag>Chưa xếp</Tag>;
                
                const days = {2: 'T2', 3: 'T3', 4: 'T4', 5: 'T5', 6: 'T6', 7: 'T7', 8: 'CN'};
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
            title: 'Địa điểm (Phòng)',
            dataIndex: ['room', 'name'],
            key: 'room',
            width: 130,
            sorter: (a, b) => (a.room?.name || '').localeCompare(b.room?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text, record) => text ? (
                <Tag color="geekblue" icon={<EnvironmentOutlined />}>
                    {text} <span style={{ opacity: 0.6 }}>({record.room.type})</span>
                </Tag>
            ) : <Text type="secondary">-</Text>
        },
        {
            title: 'Giảng viên',
            dataIndex: ['lecturer', 'fullName'],
            key: 'lecturer',
            width: 200,
            sorter: (a, b) => (a.lecturer?.fullName || '').localeCompare(b.lecturer?.fullName || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => text ? (
                <Text>{text}</Text>
            ) : <Text type="secondary" italic>Tự phân công</Text>
        },
        {
            title: 'Trạng thái',
            dataIndex: 'status',
            key: 'status',
            align: 'center',
            width: 100,
            sorter: (a, b) => (a.status || '').localeCompare(b.status || ''),
            sortDirections: ['ascend', 'descend'],
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
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
            {/* --- Header, Search & Actions --- */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Thời khóa biểu</Title>
                    <Text type="secondary">Quản lý và theo dõi các lớp đã xếp lịch</Text>
                </div>
                <Space size="middle">
                    <Input 
                        style={{ width: 280 }}
                        prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />} 
                        placeholder="Tìm theo lớp học phần, học phần hoặc lớp sinh viên" 
                        allowClear
                        onChange={(e) => setFilters({ ...filters, search: e.target.value })}
                    />
                    <Button 
                        icon={<CalendarOutlined />} 
                        onClick={() => setSemesterModalOpen(true)}
                    >
                        {activeSemester 
                            ? `${activeSemester.name} (${activeSemester.academicYear})` 
                            : 'Chọn học kỳ'}
                    </Button>
                    <Button 
                        type="primary" 
                        icon={<ThunderboltOutlined />} 
                        loading={generating}
                        onClick={handleGenerate}
                        style={{ fontWeight: 500 }}
                    >
                        Chạy xếp lịch tự động
                    </Button>
                    <Button icon={<DownloadOutlined />} loading={exporting} onClick={handleExport}>Xuất</Button>
                </Space>
            </div>

            {/* --- Data Table --- */}
            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    columns={columns} 
                    dataSource={filteredData} 
                    rowKey="id"
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>
            <Modal
                title="Chọn học kỳ hiện hành"
                open={semesterModalOpen}
                onOk={handleSetCurrentSemester}
                onCancel={() => setSemesterModalOpen(false)}
                confirmLoading={updatingSemester}
            >
                <Select
                    style={{ width: '100%' }}
                    placeholder="Chọn học kỳ"
                    value={selectedSemesterId}
                    onChange={setSelectedSemesterId}
                >
                    {semesters.map(s => (
                        <Option key={s.id} value={s.id}>
                            {s.name} ({s.academicYear}) {s.isCurrent ? '- ĐANG ÁP DỤNG' : ''}
                        </Option>
                    ))}
                </Select>
            </Modal>
        </Space>
    );
};

export default TimetableManagement;