import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Typography, message, Modal, Checkbox, Alert } from 'antd';
import { 
    UserOutlined, 
    LockOutlined, 
    QuestionCircleOutlined, 
    MailOutlined,
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

    // --- EFFECT: Add Font & Animation Styles ---
    useEffect(() => {
        // 1. Load Font Poppins
        if (typeof document !== 'undefined' && !document.getElementById('poppins-font')) {
            const link = document.createElement('link');
            link.id = 'poppins-font';
            link.rel = 'stylesheet';
            link.href = 'https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&display=swap';
            document.head.appendChild(link);
        }

        // 2. Add Keyframes for Fade In Animation
        const styleSheet = document.createElement("style");
        styleSheet.innerText = `
            @keyframes fadeInUp {
                from { opacity: 0; transform: translateY(20px); }
                to { opacity: 1; transform: translateY(0); }
            }
            .login-form-wrapper {
                animation: fadeInUp 0.8s ease-out forwards;
            }
        `;
        document.head.appendChild(styleSheet);

        return () => {
            if(styleSheet) document.head.removeChild(styleSheet);
        }
    }, []);

    // --- HANDLERS ---
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
            
            {/* --- LEFT SIDE: IMAGE WITH FADE EFFECT --- */}
            <div style={styles.leftSection}>
                {/* Lớp phủ Gradient để tạo hiệu ứng mờ dần sang phải */}
                <div style={styles.gradientOverlay}></div>
            </div>

            {/* --- RIGHT SIDE: CLEAN LOGIN FORM --- */}
            <div style={styles.rightSection}>
                <div className="login-form-wrapper" style={styles.formContainer}>
                    
                    {/* Header */}
                    <div style={{ textAlign: 'center', marginBottom: 40 }}>
                        <img src={logo} alt="Phenikaa Logo" style={styles.logo} />
                        <Title level={2} style={styles.title}>Hello Again!</Title>
                        <Text style={styles.subtitle}>Welcome back to Phenikaa Internal System</Text>
                    </div>

                    <Form
                        name="login_form"
                        onFinish={onFinish}
                        layout="vertical"
                        size="large"
                        initialValues={{ remember: true }}
                        requiredMark={false} // Ẩn dấu sao đỏ để nhìn sạch hơn
                    >
                        <Form.Item
                            name="username"
                            rules={[{ required: true, message: 'Please enter your ID!' }]}
                        >
                            <Input 
                                prefix={<UserOutlined style={{ color: '#003a70', opacity: 0.5 }} />} 
                                placeholder="Username / Staff ID" 
                                style={styles.input}
                            />
                        </Form.Item>

                        <Form.Item
                            name="password"
                            rules={[{ required: true, message: 'Please enter your password!' }]}
                        >
                            <Input.Password 
                                prefix={<LockOutlined style={{ color: '#003a70', opacity: 0.5 }} />} 
                                placeholder="Password" 
                                style={styles.input}
                            />
                        </Form.Item>

                        <div style={styles.optionsRow}>
                            <Form.Item name="remember" valuePropName="checked" noStyle>
                                <Checkbox style={{ color: '#666' }}>Remember me</Checkbox>
                            </Form.Item>

                            <Button 
                                type="link" 
                                onClick={() => setIsForgotModalOpen(true)}
                                style={styles.forgotBtn}
                            >
                                Recovery Password
                            </Button>
                        </div>

                        <Form.Item style={{ marginTop: 20 }}>
                            <Button 
                                type="primary" 
                                htmlType="submit" 
                                loading={loading}
                                block
                                style={styles.loginBtn}
                            >
                                Login
                            </Button>
                        </Form.Item>
                    </Form>
                    
                    <div style={styles.footer}>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                            © 2025 Phenikaa University. All Rights Reserved.
                        </Text>
                    </div>
                </div>
            </div>

            {/* --- MODAL (Giữ nguyên logic) --- */}
            <Modal
                title={<span><QuestionCircleOutlined /> Reset Password</span>}
                open={isForgotModalOpen}
                onCancel={() => setIsForgotModalOpen(false)}
                footer={null}
                centered
            >
                <Alert
                    message="Support Policy"
                    description="Please contact IT Department for password reset assistance."
                    type="info"
                    showIcon
                    style={{ marginBottom: 20 }}
                />
                <Form onFinish={handleSendResetRequest} layout="vertical">
                    <Form.Item name="username" label="Your ID" rules={[{ required: true }]}>
                        <Input prefix={<UserOutlined />} placeholder="e.g. admin_psc" />
                    </Form.Item>
                    <Form.Item name="email" label="Email (Optional)">
                        <Input prefix={<MailOutlined />} placeholder="name@phenikaa-uni.edu.vn" />
                    </Form.Item>
                    <div style={{ textAlign: 'right', marginTop: 10 }}>
                        <Button onClick={() => setIsForgotModalOpen(false)} style={{ marginRight: 10 }}>Close</Button>
                        <Button type="primary" htmlType="submit" loading={requestLoading}>Send Request</Button>
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
        fontFamily: "'Poppins', sans-serif",
        backgroundColor: '#ffffff',
    },
    // --- LEFT SIDE ---
    leftSection: {
        flex: '1.6', // Tăng tỷ lệ ảnh lên một chút
        position: 'relative',
        // Ảnh nền chất lượng cao
        backgroundImage: 'url("https://phenikaa-uni.edu.vn:3600/pu/vi/gioithieuvedaihocphenikaa/rectangle-65-2.png")', 
        backgroundSize: 'cover',
        backgroundPosition: 'center center',
        backgroundRepeat: 'no-repeat',
    },
    gradientOverlay: {
        position: 'absolute',
        top: 0, 
        left: 0, 
        right: 0, 
        bottom: 0,
        // MAGIC HERE: Gradient từ trong suốt (trái) sang trắng tinh (phải)
        // 0% -> 50%: Rõ nét
        // 50% -> 100%: Mờ dần vào nền trắng
        background: 'linear-gradient(90deg, rgba(255,255,255,0) 30%, rgba(255,255,255,0.6) 70%, #ffffff 100%)',
    },

    // --- RIGHT SIDE ---
    rightSection: {
        flex: '1',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#ffffff', // Nền trắng hòa với gradient
        zIndex: 1, // Đảm bảo nổi lên trên lớp viền mờ của ảnh nếu có
    },
    formContainer: {
        width: '100%',
        maxWidth: '420px',
        padding: '0 40px',
        display: 'flex',
        flexDirection: 'column',
    },
    logo: {
        height: 80,
        marginBottom: 20,
        filter: 'drop-shadow(0px 4px 6px rgba(0,0,0,0.1))', // Đổ bóng nhẹ cho logo nổi lên
    },
    title: {
        color: '#003a70', 
        marginBottom: 5, 
        fontWeight: 700,
        letterSpacing: '-0.5px'
    },
    subtitle: {
        fontSize: 15,
        color: '#8c8c8c',
    },
    input: {
        height: 55, // Input cao hơn, trông sang hơn
        borderRadius: '12px',
        fontSize: '16px',
        backgroundColor: '#f7f9fc', // Màu nền input hơi xanh xám cực nhạt
        border: '1px solid transparent',
        transition: 'all 0.3s',
        paddingLeft: 20,
    },
    // Tùy chỉnh CSS sâu hơn cho input khi focus có thể thêm trong file css global nếu muốn
    
    optionsRow: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 20,
    },
    forgotBtn: {
        padding: 0, 
        fontWeight: 600, 
        color: '#003a70',
        fontSize: '14px'
    },
    loginBtn: {
        height: 55,
        fontSize: '16px',
        fontWeight: 600,
        backgroundColor: '#003a70',
        borderColor: '#003a70',
        borderRadius: '12px',
        boxShadow: '0 10px 25px rgba(0, 58, 112, 0.3)', // Đổ bóng xanh theo màu nút
        transition: 'all 0.3s ease',
        letterSpacing: '0.5px',
    },
    footer: {
        textAlign: 'center', 
        marginTop: 50,
        opacity: 0.7
    }
};

export default LoginPage;