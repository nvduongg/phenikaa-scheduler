import React, { useMemo, useState } from 'react';

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

import AppLayout from './layouts/AppLayout';
import {
    filterMenuByRole,
    findMenuKeyByPath,
    findMenuPathByKey,
    getDefaultMenuKeyForRole,
    MENU_CONFIG,
} from './layouts/menuConfig.jsx';

const App = () => {
    const location = useLocation();
    const navigate = useNavigate();
    // Lấy user từ localStorage khi load trang
    const [user, setUser] = useState(JSON.parse(localStorage.getItem('user')));
    
    // State cho Navigation
    const [selectedKeyFallback, setSelectedKeyFallback] = useState(() => {
        const storedUser = JSON.parse(localStorage.getItem('user'));
        const keyFromPath = findMenuKeyByPath(window.location.pathname, MENU_CONFIG);
        if (keyFromPath) return keyFromPath;
        return storedUser ? getDefaultMenuKeyForRole(storedUser.role) : '0';
    });
    const [currentCurriculum, setCurrentCurriculum] = useState(null);

    const selectedKey = useMemo(() => {
        return findMenuKeyByPath(location.pathname, MENU_CONFIG) || selectedKeyFallback;
    }, [location.pathname, selectedKeyFallback]);

    // --- 3. DÙNG useMemo ĐỂ TÍNH TOÁN MENU (THAY VÌ useEffect + useState) ---
    // authorizedMenuItems sẽ tự động cập nhật ngay khi user thay đổi
    const authorizedMenuItems = useMemo(() => {
        if (!user) return [];
        return filterMenuByRole(MENU_CONFIG, user.role);
    }, [user]); // Chỉ tính lại khi user thay đổi

    const CONTENT_WIDTH = '1400px';

    // --- LOGIC LOGIN / LOGOUT ---

    const handleLoginSuccess = (userData) => {
        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
        const nextKey = getDefaultMenuKeyForRole(userData.role);
        setSelectedKeyFallback(nextKey);
        const nextPath = findMenuPathByKey(nextKey, MENU_CONFIG);
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
        const nextPath = '/v1/roadmap';
        if (nextPath && nextPath !== location.pathname) navigate(nextPath);
    };

    const handleBackToCurricula = () => {
        setCurrentCurriculum(null);
        setSelectedKeyFallback('8');
        const nextPath = findMenuPathByKey('8', MENU_CONFIG);
        if (nextPath && nextPath !== location.pathname) navigate(nextPath);
    };


    const defaultPath =
        findMenuPathByKey(selectedKey, MENU_CONFIG) ||
        findMenuPathByKey(selectedKeyFallback, MENU_CONFIG) ||
        '/v1/schools';

    return (
        <AppLayout
            user={user}
            contentWidth={CONTENT_WIDTH}
            menuItems={authorizedMenuItems}
            selectedKey={selectedKey}
            onMenuClick={(e) => {
                setSelectedKeyFallback(e.key);
                setCurrentCurriculum(null);
                handleMenuClick(e);
            }}
            onLogout={handleLogout}
        >
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
                <Route
                    path="/v1/roadmap"
                    element={<RoadmapManagement targetCurriculum={currentCurriculum} onBack={handleBackToCurricula} />}
                />
                <Route path="/v1/courses" element={<CourseManagement />} />
                <Route path="/v1/offerings" element={<OfferingManagement user={user} />} />

                <Route path="/v1/semester-settings" element={<SemesterManagement />} />
                <Route
                    path="/v1/timetable"
                    element={user && user.role === 'ADMIN' ? <TimetableManagement /> : <div style={{ padding: 24 }}>Không có quyền truy cập</div>}
                />

                <Route path="/v1/reports/lecturer-workload" element={<WorkloadStatistics />} />

                <Route path="*" element={<Navigate to={defaultPath} replace />} />
            </Routes>
        </AppLayout>
    );
};

export default App;