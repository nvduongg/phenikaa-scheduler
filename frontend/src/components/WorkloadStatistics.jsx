import React, { useState, useEffect } from 'react';
import { Card, Table, Typography, Row, Col, Statistic, Tag } from 'antd';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import axiosClient from '../api/axiosClient';

const { Title } = Typography;

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
        { title: 'Lecturer Name', dataIndex: 'lecturerName', key: 'name', fixed: 'left', sorter: (a, b) => (a.lecturerName || '').localeCompare(b.lecturerName || ''), sortDirections: ['ascend','descend'] },
        { title: 'Email', dataIndex: 'email', key: 'email', responsive: ['md'], sorter: (a, b) => (a.email || '').localeCompare(b.email || ''), sortDirections: ['ascend','descend'] },
        { 
            title: 'Classes', 
            dataIndex: 'totalClasses', 
            key: 'classes', 
            align: 'center',
            sorter: (a, b) => (a.totalClasses || 0) - (b.totalClasses || 0),
            render: (val) => <Tag color="blue">{val}</Tag>
        },
        { 
            title: 'Theory Hours', 
            dataIndex: 'totalTheoryPeriods', 
            key: 'theory', 
            align: 'center',
            sorter: (a, b) => (a.totalTheoryPeriods || 0) - (b.totalTheoryPeriods || 0),
            render: (val) => <span style={{color: '#faad14'}}>{val}</span>
        },
        { 
            title: 'Practice Hours', 
            dataIndex: 'totalPracticePeriods', 
            key: 'practice', 
            align: 'center',
            sorter: (a, b) => (a.totalPracticePeriods || 0) - (b.totalPracticePeriods || 0),
            render: (val) => <span style={{color: '#52c41a'}}>{val}</span> 
        },
        { 
            title: 'Total Load', 
            dataIndex: 'totalPeriod', 
            key: 'total', 
            align: 'center',
            sorter: (a, b) => a.totalPeriod - b.totalPeriod,
            render: (val) => <b>{val}</b>
        },
    ];

    return (
        <div style={{ padding: 0 }}>
            <Title level={3}>Lecturer Workload Statistics</Title>
            
            {/* PHẦN 1: BIỂU ĐỒ (Chỉ lấy Top 15 người cao nhất để đỡ rối) */}
            <Card style={{ marginBottom: 24 }}>
                <Title level={5}>Top Workload (Periods/Semester)</Title>
                <div style={{ width: '100%', height: 350 }}>
                    <ResponsiveContainer>
                        <BarChart
                            data={data.slice(0, 15)} // Lấy 15 người đầu tiên
                            margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
                        >
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="lecturerName" angle={-45} textAnchor="end" height={80} interval={0} fontSize={12}/>
                            <YAxis label={{ value: 'Periods', angle: -90, position: 'insideLeft' }} />
                            <Tooltip />
                            <Legend verticalAlign="top"/>
                            <Bar dataKey="totalTheoryPeriods" name="Theory" stackId="a" fill="#faad14" />
                            <Bar dataKey="totalPracticePeriods" name="Practice" stackId="a" fill="#52c41a" />
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </Card>

            {/* PHẦN 2: BẢNG CHI TIẾT */}
            <Card>
                <Title level={5}>Detailed Data</Title>
                <Table 
                    columns={columns} 
                    dataSource={data} 
                    rowKey="lecturerId" 
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>
        </div>
    );
};

export default WorkloadStatistics;