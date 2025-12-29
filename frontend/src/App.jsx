import React, { useMemo, useState } from 'react';
import { Layout, Menu, Dropdown, Space, Typography, theme, Button } from 'antd';
import { 
    BankOutlined, 
    BookOutlined, 
    CalendarOutlined, 
    UserOutlined, 
    LogoutOutlined,
    SettingOutlined,
    PieChartOutlined
} from '@ant-design/icons';

import logo from './assets/Logo.png';

// Import Components
import SchoolManagement from './components/SchoolManagement';
import FacultyManagement from './components/FacultyManagement';
import CourseManagement from './components/CourseManagement';
import OfferingManagement from './components/OfferingManagement';
import MajorManagement from './components/MajorManagement';
import CohortManagement from './components/CohortManagement';
import AdminClassManagement from './components/AdminClassManagement';
import CurriculumManagement from './components/CurriculumManagement';
import RoadmapManagement from './components/RoadmapManagement';
import LecturerManagement from './components/LecturerManagement';
import ExpertiseManagement from './components/ExpertiseManagement';
import RoomManagement from './components/RoomManagement';
import TimetableManagement from './components/TimetableManagement';
import SemesterManagement from './components/SemesterManagement';
import LoginPage from './pages/LoginPage';
import WorkloadStatistics from './components/WorkloadStatistics';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
const { Header, Content, Footer } = Layout;
const { Text } = Typography;

const keyToPath = {
    '0': '/v1/schools',
    '1': '/v1/faculties',
    '2': '/v1/courses',
    '3': '/v1/offerings',
    '4': '/v1/timetable',
    '5': '/v1/majors',
    '6': '/v1/cohorts',
    '7': '/v1/admin-classes',
    '8': '/v1/curricula',
    '9': '/v1/roadmap',
    '10': '/v1/lecturers',
    '11': '/v1/expertise',
    '12': '/v1/rooms',
    '20': '/v1/reports/lecturer-workload',
    '99': '/v1/semester-settings',
};

const pathToKey = Object.fromEntries(Object.entries(keyToPath).map(([k, p]) => [p, k]));

// --- 1. CHUYỂN DỮ LIỆU TĨNH RA NGOÀI COMPONENT ---
// Để tránh việc khởi tạo lại mảng này mỗi lần render
const allMenuItems = [
    {
        key: 'grp_org',
        label: 'Organization',
        icon: <BankOutlined />,
        roles: ['ADMIN', 'ADMIN_SCHOOL'], // Chỉ Admin Đại học mới được sửa cấu trúc trường
        children: [
            { key: '0', label: 'Schools' },
            { key: '1', label: 'Faculties' },
            { key: '5', label: 'Majors' },
            { key: '6', label: 'Cohorts' },
            { key: '7', label: 'Admin Classes' },
        ],
    },
    {
      key: 'grp_res',
      label: 'Resources',
      icon: <UserOutlined />,
      // Ai cũng cần quản lý giảng viên của cấp mình
      roles: ['ADMIN', 'ADMIN_SCHOOL', 'ADMIN_FACULTY'], 
      children: [
          { key: '10', label: 'Lecturers' }, 
          { key: '11', label: 'Expertise' },
          { key: '12', label: 'Rooms', roles: ['ADMIN'] },
      ],
    },
    {
        key: 'grp_curr',
        label: 'Curriculum & Planning',
        icon: <BookOutlined />,
        roles: ['ADMIN', 'ADMIN_SCHOOL', 'ADMIN_FACULTY'],
        children: [
            // Khung chương trình: Chỉ cấp Trường trở lên mới xem/sửa
            { key: '8', label: 'Curricula', roles: ['ADMIN', 'ADMIN_SCHOOL'] },
            { key: '2', label: 'Courses', roles: ['ADMIN', 'ADMIN_SCHOOL', 'ADMIN_FACULTY'] },
            
            // Mở lớp: Cả 3 cấp đều cần vào (Khoa nhập, Trường duyệt, ĐH xếp)
            { key: '3', label: 'Course Offerings', roles: ['ADMIN', 'ADMIN_SCHOOL', 'ADMIN_FACULTY'] },
        ],
    },
    {
        key: '4',
        label: 'Timetable Management',
        icon: <CalendarOutlined />,
        roles: ['ADMIN'],
    },
    {
      key: 'grp_sys',
      label: 'System Settings',
      icon: <SettingOutlined />, 
      roles: ['ADMIN'], // Chỉ Super Admin
      children: [
          { key: '99', label: 'Semester Settings' },
      ],
   },
   {
      key: 'grp_stat',
      label: 'Reports & Statistics',
      icon: <PieChartOutlined />, // Import icon này từ antd
      roles: ['ADMIN', 'ADMIN_SCHOOL'], // Chỉ Admin Trường/ĐH mới xem được
      children: [
          { key: '20', label: 'Lecturer Workload' },
      ],
   },
];

// --- 2. CHUYỂN HÀM UTILITY RA NGOÀI ---
const filterMenuByRole = (items, role) => {
    return items
        .filter(item => {
            // Nếu item không quy định roles -> Ai cũng thấy
            // Nếu có quy định -> Check xem role hiện tại có trong list không
            return !item.roles || item.roles.includes(role);
        })
        .map(item => {
            // Nếu có con, lọc tiếp các con
            if (item.children) {
                return { ...item, children: filterMenuByRole(item.children, role) };
            }
            return item;
        })
        .filter(item => {
            // Sau khi lọc con, nếu Group đó rỗng thì ẩn luôn Group cha
            if (item.children && item.children.length === 0) return false;
            return true;
        });
};

const App = () => {
    const location = useLocation();
    const navigate = useNavigate();
    // Lấy user từ localStorage khi load trang
    const [user, setUser] = useState(JSON.parse(localStorage.getItem('user')));
    
    // State cho Navigation
    const [selectedKeyFallback, setSelectedKeyFallback] = useState(() => {
        const storedUser = JSON.parse(localStorage.getItem('user'));
        const keyFromPath = pathToKey[window.location.pathname];
        if (keyFromPath) return keyFromPath;
        if (storedUser && storedUser.role === 'ADMIN_FACULTY') return '3';
        return '0';
    });
    const [currentCurriculum, setCurrentCurriculum] = useState(null);

    const selectedKey = useMemo(() => {
        return pathToKey[location.pathname] || selectedKeyFallback;
    }, [location.pathname, selectedKeyFallback]);

    // --- 3. DÙNG useMemo ĐỂ TÍNH TOÁN MENU (THAY VÌ useEffect + useState) ---
    // authorizedMenuItems sẽ tự động cập nhật ngay khi user thay đổi
    const authorizedMenuItems = useMemo(() => {
        if (!user) return [];
        return filterMenuByRole(allMenuItems, user.role);
    }, [user]); // Chỉ tính lại khi user thay đổi

    const { token: { colorBgContainer, borderRadiusLG } } = theme.useToken();
    const CONTENT_WIDTH = '1400px';

    // --- LOGIC LOGIN / LOGOUT ---

    const handleLoginSuccess = (userData) => {
        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
        const nextKey = userData.role === 'ADMIN_FACULTY' ? '3' : '0';
        setSelectedKeyFallback(nextKey);
        const nextPath = keyToPath[nextKey];
        if (nextPath) navigate(nextPath);
    };

    const handleLogout = () => {
        localStorage.removeItem('user');
        window.location.reload();
    };

    // Nếu chưa đăng nhập
    if (!user) {
        return <LoginPage onLoginSuccess={handleLoginSuccess} />;
    }

    // --- LOGIC RENDER CONTENT ---
    
    const handleMenuClick = (e) => {
        if (e.key === 'logout') {
            handleLogout();
        }
    };

    const handleNavigateToRoadmap = (curriculum) => {
        setCurrentCurriculum(curriculum);
        setSelectedKeyFallback('9');
        const nextPath = keyToPath['9'];
        if (nextPath && nextPath !== location.pathname) navigate(nextPath);
    };

    const handleBackToCurricula = () => {
        setCurrentCurriculum(null);
        setSelectedKeyFallback('8');
        const nextPath = keyToPath['8'];
        if (nextPath && nextPath !== location.pathname) navigate(nextPath);
    };

    const defaultPath = keyToPath[selectedKey] || keyToPath[selectedKeyFallback] || '/v1/schools';

    const userMenu = {
        items: [
            { key: 'profile', label: 'My Profile', icon: <UserOutlined /> },
            { key: 'logout', label: 'Logout', icon: <LogoutOutlined />, danger: true },
        ],
        onClick: handleMenuClick,
    };

    return (
        <Layout style={{ minHeight: '100vh', background: '#f0f2f5' }}>
            <Header style={{ height: '40px', lineHeight: '40px', background: '#003a70', padding: 0, color: 'white', zIndex: 2 }}>
                <div style={{ maxWidth: CONTENT_WIDTH, margin: '0 auto', padding: '0 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', height: '100%' }}>
                    <Text style={{ color: 'white', fontSize: '12px', fontWeight: 'bold', letterSpacing: '0.5px' }}>
                        UNIVERSITY TIMETABLING SYSTEM
                    </Text>
                    <Space size="middle">
                        <Dropdown menu={userMenu} placement="bottomRight">
                            <a onClick={(e) => e.preventDefault()} style={{ cursor: 'pointer' }}>
                                <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: '12px', fontWeight: 'bold' }}>
                                    Welcome, {user.fullName || user.username} ({user.role})
                                </Text>
                            </a>
                        </Dropdown>
                    </Space>
                </div>
            </Header>

            <Header style={{ background: colorBgContainer, padding: 0, boxShadow: '0 2px 8px #f0f1f2', height: '64px', zIndex: 1 }}>
                <div style={{ maxWidth: CONTENT_WIDTH, margin: '0 auto', padding: '0 20px', display: 'flex', alignItems: 'center', height: '100%' }}>
                    <div style={{ display: 'flex', alignItems: 'center', minWidth: '250px' }}>
                        <img src={logo} alt="Phenikaa University" style={{ height: '48px' }} />
                    </div>
                    <div style={{ flex: 1, display: 'flex', justifyContent: 'flex-end', marginRight: '-16px' }}>
                        <Menu
                            mode="horizontal"
                            selectedKeys={[selectedKey]}
                            // Sử dụng danh sách menu đã lọc
                            items={authorizedMenuItems}
                            onClick={(e) => {
                                setSelectedKeyFallback(e.key);
                                setCurrentCurriculum(null);
                                const nextPath = keyToPath[e.key];
                                if (nextPath && nextPath !== location.pathname) {
                                    navigate(nextPath);
                                }
                            }}
                            style={{ borderBottom: 'none', width: '100%', justifyContent: 'flex-end', fontSize: '14px', fontWeight: 500 }}
                        />
                    </div>
                </div>
            </Header>

            <Content style={{ padding: '24px 0' }}>
                <div style={{ maxWidth: CONTENT_WIDTH, margin: '0 auto', padding: '0 20px' }}>
                    <div style={{ padding: 24, minHeight: 500, background: colorBgContainer, borderRadius: borderRadiusLG, boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)' }}>
                        <Routes>
                            <Route path="/" element={<Navigate to={defaultPath} replace />} />

                            <Route path="/v1/schools" element={<SchoolManagement />} />
                            <Route path="/v1/faculties" element={<FacultyManagement />} />
                            <Route path="/v1/majors" element={<MajorManagement />} />
                            <Route path="/v1/cohorts" element={<CohortManagement />} />
                            <Route path="/v1/admin-classes" element={<AdminClassManagement />} />

                            <Route path="/v1/lecturers" element={<LecturerManagement />} />
                            <Route path="/v1/expertise" element={<ExpertiseManagement />} />
                            <Route path="/v1/rooms" element={<RoomManagement />} />

                            <Route path="/v1/curricula" element={<CurriculumManagement onNavigate={handleNavigateToRoadmap} />} />
                            <Route path="/v1/roadmap" element={<RoadmapManagement targetCurriculum={currentCurriculum} onBack={handleBackToCurricula} />} />
                            <Route path="/v1/courses" element={<CourseManagement />} />
                            <Route path="/v1/offerings" element={<OfferingManagement user={user} />} />

                            <Route path="/v1/semester-settings" element={<SemesterManagement />} />
                            <Route path="/v1/timetable" element={user && user.role === 'ADMIN' ? <TimetableManagement /> : <div style={{padding:24}}>Access denied</div>} />

                            <Route path="/v1/reports/lecturer-workload" element={<WorkloadStatistics />} />

                            <Route path="*" element={<Navigate to={defaultPath} replace />} />
                        </Routes>
                    </div>
                </div>
            </Content>

            <Footer style={{ textAlign: 'center', background: 'transparent', color: '#888' }}>
                Phenikaa University Scheduler ©2025
            </Footer>
        </Layout>
    );
};

export default App;