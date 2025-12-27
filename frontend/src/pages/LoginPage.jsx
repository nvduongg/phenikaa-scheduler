import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Typography, message, Modal, Checkbox, Alert, Space } from 'antd';
import { 
    UserOutlined, 
    LockOutlined, 
    QuestionCircleOutlined, 
    SafetyOutlined, 
    MailOutlined,
    CheckCircleFilled,
    PhoneOutlined,
    GlobalOutlined
} from '@ant-design/icons';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import logo from '../assets/Logo.png'; 

const { Title, Text } = Typography;

const LoginPage = ({ onLoginSuccess }) => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    
    // Modal state
    const [isForgotModalOpen, setIsForgotModalOpen] = useState(false);
    const [requestLoading, setRequestLoading] = useState(false);

    // Load Poppins font once for this page
    useEffect(() => {
        if (typeof document !== 'undefined' && !document.getElementById('poppins-font')) {
            const link = document.createElement('link');
            link.id = 'poppins-font';
            link.rel = 'stylesheet';
            link.href = 'https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700;800&display=swap';
            document.head.appendChild(link);
        }
    }, []);

    // --- HANDLERS (Giữ nguyên logic cũ) ---
    const onFinish = async (values) => {
        setLoading(true);
        try {
            const res = await axios.post('http://localhost:8080/api/auth/login', values);
            message.success({ content: `Welcome back, ${res.data.fullName}!`, key: 'login' });
            if (onLoginSuccess) onLoginSuccess(res.data);
            navigate('/'); 
        } catch (error) {
            const errorMsg = error.response?.data?.message || "Login failed. Incorrect username or password.";
            message.error({ content: errorMsg, key: 'login' });
        } finally {
            setLoading(false);
        }
    };

    const handleSendResetRequest = () => {
        setRequestLoading(true);
        setTimeout(() => {
            message.success("Request sent successfully! Admin will contact you via email.");
            setRequestLoading(false);
            setIsForgotModalOpen(false);
        }, 1500);
    };

    return (
        <div style={styles.container}>
            
            {/* --- LEFT SIDE: ENHANCED BRANDING AREA --- */}
            <div style={styles.leftSection}>
                <div style={styles.overlay}>
                    {/* Top Brand Info */}
                    <div style={styles.brandContent}>
                        <div style={styles.brandHeader}>
                            <SafetyOutlined style={{ fontSize: 40, color: '#40a9ff', marginBottom: 20 }} />
                            <h1 style={styles.brandTitle}>Phenikaa University</h1>
                            <p style={styles.brandSubtitle}>Intelligent Timetabling & Resource Management System</p>
                        </div>

                        {/* Feature List (Bổ sung cho đỡ trống) */}
                        <div style={styles.featureList}>
                            <div style={styles.featureItem}>
                                <CheckCircleFilled style={{ color: '#52c41a', marginRight: 10, fontSize: 18 }} />
                                <Text style={styles.featureText}>Automated Course Scheduling</Text>
                            </div>
                            <div style={styles.featureItem}>
                                <CheckCircleFilled style={{ color: '#52c41a', marginRight: 10, fontSize: 18 }} />
                                <Text style={styles.featureText}>Smart Room Allocation</Text>
                            </div>
                            <div style={styles.featureItem}>
                                <CheckCircleFilled style={{ color: '#52c41a', marginRight: 10, fontSize: 18 }} />
                                <Text style={styles.featureText}>Lecturer Workload Tracking</Text>
                            </div>
                            <div style={styles.featureItem}>
                                <CheckCircleFilled style={{ color: '#52c41a', marginRight: 10, fontSize: 18 }} />
                                <Text style={styles.featureText}>Conflict Detection & Resolution</Text>
                            </div>
                        </div>
                    </div>

                    {/* Bottom Support Info (Bổ sung) */}
                    <div style={styles.supportInfo}>
                        <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 12, display: 'block', marginBottom: 8 }}>
                            Having trouble logging in? Contact IT Support:
                        </Text>
                        <Space size="large">
                            <span style={styles.contactItem}><PhoneOutlined /> Ext: 103 (IT Dept)</span>
                            <span style={styles.contactItem}><MailOutlined /> support@phenikaa-uni.edu.vn</span>
                            <span style={styles.contactItem}><GlobalOutlined /> it.phenikaa-uni.edu.vn</span>
                        </Space>
                    </div>
                </div>
            </div>

            {/* --- RIGHT SIDE: LOGIN FORM (Giữ nguyên) --- */}
            <div style={styles.rightSection}>
                <div style={styles.formContainer}>
                    <div style={{ textAlign: 'center', marginBottom: 40 }}>
                        <img src={logo} alt="Logo" style={{ height: 70, marginBottom: 15 }} />
                        <Title level={2} style={{ color: '#003a70', margin: 0, fontWeight: 700 }}>Welcome Back</Title>
                        <Text type="secondary" style={{ fontSize: 16 }}>Sign in to access your dashboard</Text>
                    </div>

                    <Form
                        name="login_form"
                        onFinish={onFinish}
                        layout="vertical"
                        size="large"
                        initialValues={{ remember: true }}
                    >
                        <Form.Item
                            label={<span style={{ fontWeight: 600, color: '#003a70' }}>Username / Staff ID</span>}
                            name="username"
                            rules={[{ required: true, message: 'Please input your Username!' }]}
                        >
                            <Input 
                                prefix={<UserOutlined style={{ color: '#bfbfbf' }} />} 
                                placeholder="e.g., admin_psc" 
                                style={styles.input}
                            />
                        </Form.Item>

                        <Form.Item
                            label={<span style={{ fontWeight: 600, color: '#003a70' }}>Password</span>}
                            name="password"
                            rules={[{ required: true, message: 'Please input your Password!' }]}
                        >
                            <Input.Password 
                                prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} 
                                placeholder="Enter your password" 
                                style={styles.input}
                            />
                        </Form.Item>

                        <div style={styles.optionsRow}>
                            <Form.Item name="remember" valuePropName="checked" noStyle>
                                <Checkbox>Remember me</Checkbox>
                            </Form.Item>

                            <Button 
                                type="link" 
                                onClick={() => setIsForgotModalOpen(true)}
                                style={{ padding: 0, fontWeight: 600, color: '#003a70' }}
                            >
                                Forgot password?
                            </Button>
                        </div>

                        <Form.Item>
                            <Button 
                                type="primary" 
                                htmlType="submit" 
                                loading={loading}
                                block
                                style={styles.loginBtn}
                            >
                                Sign In
                            </Button>
                        </Form.Item>
                    </Form>
                    
                    <div style={{ textAlign: 'center', marginTop: 40 }}>
                        <Text type="secondary" style={{ fontSize: 13 }}>
                            © 2025 Phenikaa University. Internal System v1.0
                        </Text>
                    </div>
                </div>
            </div>

            {/* --- MODAL (Giữ nguyên) --- */}
            <Modal
                title={<span><QuestionCircleOutlined /> Reset Password Request</span>}
                open={isForgotModalOpen}
                onCancel={() => setIsForgotModalOpen(false)}
                footer={null}
                centered
            >
                <Alert
                    message="Internal System Policy"
                    description="Self-service password reset is disabled. Please submit a request below, and the IT Department will contact you via your university email."
                    type="info"
                    showIcon
                    style={{ marginBottom: 20 }}
                />
                <Form onFinish={handleSendResetRequest} layout="vertical">
                    <Form.Item name="username" label="Username / Staff ID" rules={[{ required: true }]}>
                        <Input prefix={<UserOutlined />} placeholder="e.g. admin_psc" />
                    </Form.Item>
                    <Form.Item name="email" label="Contact Email (Optional)">
                        <Input prefix={<MailOutlined />} placeholder="name@phenikaa-uni.edu.vn" />
                    </Form.Item>
                    <div style={{ textAlign: 'right', marginTop: 10 }}>
                        <Button onClick={() => setIsForgotModalOpen(false)} style={{ marginRight: 10 }}>Cancel</Button>
                        <Button type="primary" htmlType="submit" loading={requestLoading}>Submit Request</Button>
                    </div>
                </Form>
            </Modal>
        </div>
    );
};

// --- STYLES ---
const styles = {
    container: {
        display: 'flex',
        height: '100vh',
        width: '100vw',
        overflow: 'hidden',
        fontFamily: "'Poppins', sans-serif", // Sử dụng Poppins cho đồng nhất
    },
    // --- LEFT SIDE ---
    leftSection: {
        flex: '1.3',
        position: 'relative',
        backgroundImage: 'url("https://phenikaa-uni.edu.vn/img/phenikaa-campus.jpg")', // Ảnh nền
        backgroundSize: 'cover',
        backgroundPosition: 'center',
    },
    overlay: {
        position: 'absolute',
        top: 0, left: 0, right: 0, bottom: 0,
        // Gradient xanh đậm dần xuống dưới để text dễ đọc
        background: 'linear-gradient(135deg, rgba(0, 58, 112, 0.9) 0%, rgba(0, 25, 60, 0.95) 100%)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center', // Căn giữa nội dung theo chiều dọc
        alignItems: 'center', // Căn giữa theo chiều ngang
        padding: '80px 60px',
        gap: '40px',
    },
    brandContent: {
        color: 'white',
        textAlign: 'center',
    },
    brandHeader: {
        marginBottom: 40,
    },
    brandTitle: {
        fontSize: '52px',
        fontWeight: '800',
        marginBottom: '15px',
        lineHeight: 1.1,
        letterSpacing: '-1px',
        background: 'linear-gradient(to right, #ffffff, #d9d9d9)',
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
    },
    brandSubtitle: {
        fontSize: '18px',
        fontWeight: 300,
        opacity: 0.85,
        maxWidth: '620px',
    },
    // Danh sách tính năng (Lấp đầy khoảng trống)
    featureList: {
        display: 'flex',
        flexDirection: 'column',
        gap: '20px',
        marginTop: '20px',
    },
    featureItem: {
        display: 'flex',
        alignItems: 'center',
        background: 'rgba(255, 255, 255, 0.1)', // Hiệu ứng kính
        padding: '15px 20px',
        borderRadius: '12px',
        backdropFilter: 'blur(5px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        maxWidth: '450px',
        transition: 'transform 0.3s ease',
        cursor: 'default',
    },
    featureText: {
        color: 'white',
        fontSize: '16px',
        fontWeight: 500,
    },
    // Footer Support Info
    supportInfo: {
        borderTop: '1px solid rgba(255,255,255,0.2)',
        paddingTop: 20,
        textAlign: 'center',
    },
    contactItem: {
        color: 'rgba(255,255,255,0.85)',
        fontSize: '14px',
        fontWeight: 500,
        display: 'inline-flex',
        alignItems: 'center',
        gap: '8px',
    },

    // --- RIGHT SIDE ---
    rightSection: {
        flex: '1',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#ffffff',
    },
    formContainer: {
        width: '100%',
        maxWidth: '450px',
        padding: '40px',
    },
    input: {
        height: 50,
        borderRadius: 8,
        fontSize: '16px',
        backgroundColor: '#f9f9f9', // Màu nền input hơi xám nhẹ
        border: '1px solid #e0e0e0',
    },
    optionsRow: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 30,
    },
    loginBtn: {
        height: 55, // Nút to hơn
        fontSize: 18,
        fontWeight: 'bold',
        backgroundColor: '#003a70',
        borderColor: '#003a70',
        borderRadius: 8,
        boxShadow: '0 8px 20px rgba(0, 58, 112, 0.2)',
        transition: 'all 0.3s ease',
    }
};

export default LoginPage;