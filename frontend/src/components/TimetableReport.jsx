import React, { useState } from 'react';
import { Card, Form, InputNumber, Checkbox, Select, Button, message, Table, Space, Typography } from 'antd';
import axiosClient from '../api/axiosClient';

const { Title } = Typography;
const { Option } = Select;

const TimetableReport = () => {
    const [loading, setLoading] = useState(false);
    const [reportData, setReportData] = useState(null);

    const handleFinish = async (values) => {
        setLoading(true);
        try {
            const res = await axiosClient.post('/statistics/timetable', values);
            setReportData(res.data || []);
            message.success('Report generated');
        } catch (e) {
            console.error(e);
            message.error('Failed to generate timetable report');
        } finally {
            setLoading(false);
        }
    };

    const deriveColumns = (data) => {
        if (!Array.isArray(data) || data.length === 0) return [];
        const keys = Object.keys(data[0]);
        return keys.map(k => ({ title: k, dataIndex: k, key: k }));
    };

    return (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <div>
                <Title level={3} style={{ margin: 0 }}>Timetable Report</Title>
                <div style={{ color: '#666' }}>Generate timetable quality and penalty reports</div>
            </div>

            <Card>
                <Form layout="vertical" onFinish={handleFinish} initialValues={{ penaltyWeight: 10, includeUnscheduled: true }}>
                    <Form.Item label="Penalty weight (points)" name="penaltyWeight">
                        <InputNumber min={0} max={1000} />
                    </Form.Item>

                    <Form.Item name="includePenalties" valuePropName="checked">
                        <Checkbox>Include penalty details</Checkbox>
                    </Form.Item>

                    <Form.Item name="includeUnscheduled" valuePropName="checked">
                        <Checkbox>Include unscheduled classes</Checkbox>
                    </Form.Item>

                    <Form.Item label="Conflict types to include" name="conflictTypes">
                        <Select mode="multiple" placeholder="Select conflict types">
                            <Option value="room">Room conflicts</Option>
                            <Option value="lecturer">Lecturer conflicts</Option>
                            <Option value="time">Time overlaps</Option>
                            <Option value="capacity">Capacity issues</Option>
                        </Select>
                    </Form.Item>

                    <Form.Item>
                        <Space>
                            <Button type="primary" htmlType="submit" loading={loading}>Generate Report</Button>
                            <Button onClick={() => { setReportData(null); }}>Clear</Button>
                        </Space>
                    </Form.Item>
                </Form>
            </Card>

            {reportData && Array.isArray(reportData) && (
                <Card>
                    <Table dataSource={reportData} columns={deriveColumns(reportData)} rowKey={(r, i) => r.id || i} pagination={{ pageSize: 10 }} />
                </Card>
            )}
        </Space>
    );
};

export default TimetableReport;
