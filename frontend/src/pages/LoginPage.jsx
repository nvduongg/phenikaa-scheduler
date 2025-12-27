import React from 'react';
import { Form, Input, Button, Card, message, Typography } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import axios from 'axios';
// Bỏ import useNavigate vì ta xử lý state ở App cha

const { Title } = Typography;

const LoginPage = ({ onLoginSuccess }) => { // Nhận prop từ App

    const onFinish = async (values) => {
        try {
            const res = await axios.post('http://localhost:8080/api/auth/login', values);
            
            message.success("Login successful!");
            
            // Gọi hàm callback để báo cho App biết đã login xong
            if (onLoginSuccess) {
                onLoginSuccess(res.data); 
            }
            
        } catch (error) {
            message.error("Login failed: " + (error.response?.data || "Server Error"));
        }
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#f0f2f5' }}>
            <Card style={{ width: 400, boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
                <div style={{ textAlign: 'center', marginBottom: 24 }}>
                    <Title level={3} style={{ color: '#1890ff' }}>Phenikaa Scheduler</Title>
                    <Typography.Text type="secondary">System Login</Typography.Text>
                </div>
                
                <Form name="login" onFinish={onFinish} size="large" layout="vertical">
                    <Form.Item name="username" rules={[{ required: true, message: 'Please input username!' }]}>
                        <Input prefix={<UserOutlined />} placeholder="Username" />
                    </Form.Item>

                    <Form.Item name="password" rules={[{ required: true, message: 'Please input password!' }]}>
                        <Input.Password prefix={<LockOutlined />} placeholder="Password" />
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit" block>
                            Log in
                        </Button>
                    </Form.Item>
                </Form>
            </Card>
        </div>
    );
};

export default LoginPage;