import React, { useState } from 'react';
import { Layout, Menu, Dropdown, Space, Typography, theme, Button } from 'antd';
import { 
    BankOutlined, 
    BookOutlined, 
    CalendarOutlined, 
    UserOutlined, 
    LogoutOutlined,
    SettingOutlined
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
import TimetableView from './components/TimetableView';
import SemesterManagement from './components/SemesterManagement';

const { Header, Content, Footer } = Layout;
const { Text } = Typography;

const App = () => {
    const [selectedKey, setSelectedKey] = useState('0');
    const [currentCurriculum, setCurrentCurriculum] = useState(null);
    
    const { token: { colorBgContainer, borderRadiusLG } } = theme.useToken();

    // Định nghĩa độ rộng chuẩn cho toàn bộ trang web để mọi thứ thẳng hàng
    const CONTENT_WIDTH = '1400px'; 

    const menuItems = [
        {
            key: 'grp_org',
            label: 'Organization',
            icon: <BankOutlined />,
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
          label: 'Resources', // Nhóm mới
          icon: <UserOutlined />,
          children: [
              { key: '10', label: 'Lecturers' }, // <-- Thêm vào đây (Key 10)
              { key: '11', label: 'Expertise (Mapping)' },
              { key: '12', label: 'Rooms' },
          ],
        },
        {
            key: 'grp_curr',
            label: 'Curriculum & Planning',
            icon: <BookOutlined />,
            children: [
                { key: '8', label: 'Curricula (Programs)' },
                { key: '2', label: 'Courses' },
                { key: '3', label: 'Course Offerings' },
            ],
        },
        {
            key: '4',
            label: 'Timetable View',
            icon: <CalendarOutlined />,
        },
        {
          key: 'grp_sys',
          label: 'System & Settings',
          icon: <SettingOutlined />, // Import icon này
          children: [
              { key: '99', label: 'Semester Settings' },
          ],
       },
    ];

    const userMenu = {
        items: [
            { key: 'profile', label: 'My Profile', icon: <UserOutlined /> },
            { key: 'logout', label: 'Logout', icon: <LogoutOutlined />, danger: true },
        ],
    };

    const handleNavigateToRoadmap = (curriculum) => {
        setCurrentCurriculum(curriculum);
        setSelectedKey('9');
    };

    const handleBackToCurricula = () => {
        setCurrentCurriculum(null);
        setSelectedKey('8');
    };

    const renderContent = () => {
        switch (selectedKey) {
            case '0': return <SchoolManagement />;
            case '1': return <FacultyManagement />;
            case '5': return <MajorManagement />;
            case '6': return <CohortManagement />;
            case '7': return <AdminClassManagement />;

            case '10': return <LecturerManagement />;
            case '11': return <ExpertiseManagement />;
            case '12': return <RoomManagement />;

            case '8': return <CurriculumManagement onNavigate={handleNavigateToRoadmap} />;
            case '9': return <RoadmapManagement targetCurriculum={currentCurriculum} onBack={handleBackToCurricula} />;
            case '2': return <CourseManagement />;
            case '3': return <OfferingManagement />;

            case '99': return <SemesterManagement />;
            case '4': return <TimetableView />;
        }
    };

    return (
        <Layout style={{ minHeight: '100vh', background: '#f0f2f5' }}>
            {/* --- HEADER 1: Top Bar (System Name & User) --- */}
            <Header style={{ 
                height: '40px', 
                lineHeight: '40px', 
                background: '#003a70', 
                padding: 0, // Reset padding mặc định của Antd
                color: 'white',
                zIndex: 2
            }}>
                {/* Container căn giữa */}
                <div style={{ 
                    maxWidth: CONTENT_WIDTH, 
                    margin: '0 auto', 
                    padding: '0 20px', 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center',
                    height: '100%'
                }}>
                    <Text style={{ color: 'white', fontSize: '12px', fontWeight: 'bold', letterSpacing: '0.5px' }}>
                        UNIVERSITY TIMETABLING SYSTEM
                    </Text>
                    <Space size="middle">
                        <Dropdown menu={userMenu} placement="bottomRight">
                            <a onClick={(e) => e.preventDefault()} style={{ cursor: 'pointer' }}>
                                <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: '12px', fontWeight: 'bold' }}>
                                    Welcome, Admin
                                </Text>
                            </a>
                        </Dropdown>
                    </Space>
                </div>
            </Header>

            {/* --- HEADER 2: Main Navigation (Logo & Menu) --- */}
            <Header style={{ 
                background: colorBgContainer, 
                padding: 0, 
                boxShadow: '0 2px 8px #f0f1f2', 
                height: '64px',
                zIndex: 1
            }}>
                {/* Container căn giữa - Thẳng hàng với Header trên và Content dưới */}
                <div style={{ 
                    maxWidth: CONTENT_WIDTH, 
                    margin: '0 auto', 
                    padding: '0 20px', 
                    display: 'flex', 
                    alignItems: 'center',
                    height: '100%'
                }}>
                    {/* LOGO (Bên Trái) */}
                    <div style={{ display: 'flex', alignItems: 'center', minWidth: '250px' }}>
                        <img src={logo} alt="Phenikaa University" style={{ height: '48px' }} />
                    </div>

                    {/* MENU (Đẩy sang Bên Phải) */}
                    {/* flex: 1 và justifyContent: 'flex-end' sẽ đẩy menu sang phải */}
                    <div style={{ flex: 1, display: 'flex', justifyContent: 'flex-end', marginRight: '-16px' }}>
                        <Menu
                            mode="horizontal"
                            defaultSelectedKeys={['0']}
                            selectedKeys={[selectedKey]}
                            items={menuItems}
                            onClick={(e) => {
                                setSelectedKey(e.key);
                                setCurrentCurriculum(null);
                            }}
                            style={{ 
                                borderBottom: 'none', 
                                width: '100%', 
                                justifyContent: 'flex-end', // Quan trọng: Căn các item trong menu sang phải
                                fontSize: '14px',
                                fontWeight: 500
                            }}
                        />
                    </div>
                </div>
            </Header>

            {/* --- CONTENT AREA --- */}
            <Content style={{ padding: '24px 0' }}>
                {/* Container căn giữa - Thẳng hàng với Headers */}
                <div style={{ 
                    maxWidth: CONTENT_WIDTH, 
                    margin: '0 auto', 
                    padding: '0 20px' // Padding ngang để nội dung không dính sát mép khi màn hình nhỏ
                }}>
                    <div style={{ 
                        padding: 24, 
                        minHeight: 500, 
                        background: colorBgContainer, 
                        borderRadius: borderRadiusLG,
                        boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)' 
                    }}>
                        {renderContent()}
                    </div>
                </div>
            </Content>

            <Footer style={{ textAlign: 'center', background: 'transparent', color: '#888' }}>
                Phenikaa University Scheduler ©2025 Created by IT Student
            </Footer>
        </Layout>
    );
};

export default App;