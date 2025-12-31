import React, { useState, useEffect } from 'react';
import { Card, Table, Typography, Tag, Space } from 'antd';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const WorkloadStatistics = () => {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            // Gọi API không cần tham số để lấy kỳ hiện tại
            const res = await axiosClient.get('/statistics/lecturer-workload');
            setData(res.data);
        } catch (error) {
            console.error("Fetch stats failed", error);
        } finally {
            setLoading(false);
        }
    };

    // Cấu hình cột cho bảng
    const columns = [
        { title: 'Tên giảng viên', dataIndex: 'lecturerName', key: 'name', fixed: 'left', sorter: (a, b) => (a.lecturerName || '').localeCompare(b.lecturerName || ''), sortDirections: ['ascend','descend'] },
        { title: 'Email', dataIndex: 'email', key: 'email', responsive: ['md'], sorter: (a, b) => (a.email || '').localeCompare(b.email || ''), sortDirections: ['ascend','descend'] },
        { 
            title: 'Số lớp', 
            dataIndex: 'totalClasses', 
            key: 'classes', 
            align: 'center',
            sorter: (a, b) => (a.totalClasses || 0) - (b.totalClasses || 0),
            render: (val) => <Tag color="blue">{val}</Tag>
        },
        { 
            title: 'Tiết LT', 
            dataIndex: 'totalTheoryPeriods', 
            key: 'theory', 
            align: 'center',
            sorter: (a, b) => (a.totalTheoryPeriods || 0) - (b.totalTheoryPeriods || 0),
            render: (val) => <span style={{color: '#faad14'}}>{val}</span>
        },
        { 
            title: 'Tiết TH', 
            dataIndex: 'totalPracticePeriods', 
            key: 'practice', 
            align: 'center',
            sorter: (a, b) => (a.totalPracticePeriods || 0) - (b.totalPracticePeriods || 0),
            render: (val) => <span style={{color: '#52c41a'}}>{val}</span> 
        },
        { 
            title: 'Tổng tải', 
            dataIndex: 'totalPeriod', 
            key: 'total', 
            align: 'center',
            sorter: (a, b) => a.totalPeriod - b.totalPeriod,
            render: (val) => <b>{val}</b>
        },
    ];

    return (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <div>
                <Title level={3} style={{ margin: 0 }}>Thống kê tải giảng dạy giảng viên</Title>
                <Text type="secondary">Tổng hợp theo học kỳ hiện hành</Text>
            </div>
            
            {/* PHẦN 1: BIỂU ĐỒ (Chỉ lấy Top 15 người cao nhất để đỡ rối) */}
            <Card style={{ marginBottom: 24 }}>
                <Title level={5}>Top tải giảng dạy (Tiết/học kỳ)</Title>
                <div style={{ width: '100%', height: 350 }}>
                    <ResponsiveContainer>
                        <BarChart
                            data={data.slice(0, 15)} // Lấy 15 người đầu tiên
                            margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
                        >
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="lecturerName" angle={-45} textAnchor="end" height={80} interval={0} fontSize={12}/>
                            <YAxis label={{ value: 'Tiết', angle: -90, position: 'insideLeft' }} />
                            <Tooltip />
                            <Legend verticalAlign="top"/>
                            <Bar dataKey="totalTheoryPeriods" name="Lý thuyết" stackId="a" fill="#faad14" />
                            <Bar dataKey="totalPracticePeriods" name="Thực hành" stackId="a" fill="#52c41a" />
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </Card>

            {/* PHẦN 2: BẢNG CHI TIẾT */}
            <Card>
                <Title level={5}>Dữ liệu chi tiết</Title>
                <Table 
                    columns={columns} 
                    dataSource={data} 
                    rowKey="lecturerId" 
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </Space>
    );
};

export default WorkloadStatistics;