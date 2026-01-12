import React, { useState, useEffect } from 'react';
import { Upload, Button, Table, message, Card, Typography, Space, Tag, Tooltip, Modal, Select, Form, Input, Popconfirm, Row, Col } from 'antd';
import { UploadOutlined, DownloadOutlined, ReloadOutlined, UserSwitchOutlined, NodeIndexOutlined, EditOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import axiosClient from '../api/axiosClient';

const { Title, Text } = Typography;

const OfferingManagement = ({ user }) => {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [courses, setCourses] = useState([]);
    const [adminClasses, setAdminClasses] = useState([]);
    const [parentOptions, setParentOptions] = useState([]);

    // CRUD modal state
    const [editModalVisible, setEditModalVisible] = useState(false);
    const [editingOffering, setEditingOffering] = useState(null);
    const [form] = Form.useForm();
    
    // Smart form state
    const [selectedCourse, setSelectedCourse] = useState(null);
    const [previewOfferings, setPreviewOfferings] = useState([]); // Preview lớp sẽ tạo
    const [numTheoryClasses, setNumTheoryClasses] = useState(1); // Số lớp LT/ELN/COURSERA
    const [theoryClassType, setTheoryClassType] = useState('LT'); // LT | ELN | COURSERA
    const [numPracticalClasses, setNumPracticalClasses] = useState(0); // Số lớp TH
    const [theoryRequiredRoomType, setTheoryRequiredRoomType] = useState('NONE'); // NONE | PC | LAB
    const [practicalRequiredRoomType, setPracticalRequiredRoomType] = useState('NONE'); // NONE | PC | LAB
    const [totalPlannedSize, setTotalPlannedSize] = useState(60); // Tổng sĩ số (toàn bộ học phần)
    
    // Assign Lecturer State
    const [lecturers, setLecturers] = useState([]);
    const [assignModalVisible, setAssignModalVisible] = useState(false);
    const [selectedOffering, setSelectedOffering] = useState(null);
    const [selectedLecturerId, setSelectedLecturerId] = useState(null);

    const fetchData = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get('/offerings');
            // Sắp xếp để lớp Mẹ (LT) nằm gần lớp Con (TH) cho dễ nhìn
            const sortedData = response.data.sort((a, b) => a.code.localeCompare(b.code));
            setData(sortedData);
        } catch (error) {
            message.error('Không thể tải dữ liệu: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const fetchLecturers = async () => {
        try {
            const res = await axiosClient.get('/lecturers');
            setLecturers(res.data);
        } catch (error) {
            console.error("Failed to fetch lecturers", error);
        }
    };

    const fetchCourses = async () => {
        try {
            const res = await axiosClient.get('/courses');
            setCourses(res.data);
        } catch (error) {
            console.error('Failed to fetch courses', error);
        }
    };

    const fetchAdminClasses = async () => {
        try {
            const res = await axiosClient.get('/admin-classes');
            setAdminClasses(res.data);
        } catch (error) {
            console.error('Failed to fetch admin classes', error);
        }
    };

    useEffect(() => {
        fetchData();
        fetchLecturers();
        fetchCourses();
        fetchAdminClasses();
    }, []);

    const handleUploadRequest = async ({ file, onSuccess, onError }) => {
        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await axiosClient.post('/offerings/import', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                }
            });
            message.success(`Đã tải lên ${file.name} thành công.`);
            onSuccess(response.data);
            fetchData();
        } catch (error) {
            message.error(`Tải lên ${file.name} thất bại: ${error.response?.data || error.message}`);
            onError(error);
        }
    };

    const uploadProps = {
        name: 'file',
        showUploadList: false,
        customRequest: handleUploadRequest,
    };

    const handleDownloadTemplate = async () => {
        try {
            const response = await axiosClient.get('/offerings/template', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'Offering_Plan_Template_v2.xlsx');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            message.error('Không thể tải file mẫu');
        }
    };

    const handleAutoAssign = async () => {
        setLoading(true);
        try {
            const res = await axiosClient.post('/offerings/auto-assign-lecturers');
            message.success(res.data);
            fetchData(); 
        } catch (error) {
            message.error("Tự phân công thất bại: " + error.message);
        } finally {
            setLoading(false);
        }
    };

    // === Helper functions để tạo smart offering ===
    const detectDefaultClassType = (course) => {
        // Nếu là khoa máy tính → mặc định ELN, không thì LT
        if (course?.school?.code?.toUpperCase() === 'PSC') return 'ELN';
        if (course?.managingFaculty?.school?.code?.toUpperCase() === 'PSC') return 'ELN';
        return 'LT';
    };

    const generateClassCode = (courseCode, classType, index, group = 'N01') => {
        // Định dạng hợp lệ theo backend validator:
        // CourseCode(Group[.Suffix])
        // Ví dụ: CSE703002(N01.LT), CSE703002(N01.ELN), CSE703002(N01.COURSERA), CSE703002(N01.TH1)
        const upperType = (classType || 'LT').toUpperCase();
        let suffix;
        if (upperType === 'TH') {
            suffix = `TH${index + 1}`;
        } else if (upperType === 'ELN') {
            suffix = 'ELN';
        } else if (upperType === 'COURSERA') {
            suffix = 'COURSERA';
        } else if (upperType === 'LT') {
            suffix = 'LT';
        } else {
            // Không có suffix → ALL
            return `${courseCode}(${group})`;
        }
        return `${courseCode}(${group}.${suffix})`;
    };

    const formatGroup = (i) => `N${String(i + 1).padStart(2, '0')}`;

    const splitTotalSize = (total, parts) => {
        const safeParts = Number(parts) || 0;
        const safeTotal = Math.max(0, Number(total) || 0);
        if (safeParts <= 0) return [];
        const base = Math.floor(safeTotal / safeParts);
        const remainder = safeTotal % safeParts;
        return Array.from({ length: safeParts }, (_, i) => base + (i < remainder ? 1 : 0));
    };

    const generatePreviewOfferings = (course, numLT, numTH, roomTypeForLT, roomTypeForTH, theoryType, totalSize) => {
        if (!course) return [];
        
        const defaultType = (theoryType || detectDefaultClassType(course)).toUpperCase();
        const offerings = [];

        const theorySizes = splitTotalSize(totalSize, numLT);
        const practicalSizes = splitTotalSize(totalSize, numTH);
        
        // Tạo nhiều lớp LT/ELN/COURSERA theo numLT, nhóm tăng dần N01, N02, ...
        for (let i = 0; i < numLT; i++) {
            const group = formatGroup(i);
            offerings.push({
                code: generateClassCode(course.courseCode, defaultType, i, group),
                classType: defaultType,
                courseCode: course.courseCode,
                group,
                isNew: true,
                isPrimary: true,
                requiredRoomType: roomTypeForLT !== 'NONE' && defaultType === 'LT' ? roomTypeForLT : null,
                plannedSize: theorySizes[i] ?? null,
            });
        }
        
        // Tạo TH (nếu có) và phân bổ đều vào các nhóm LT/ELN/COURSERA (N01, N02, ...)
        // Ví dụ: 2 lớp lý thuyết + 4 lớp TH => N01.TH1,N01.TH2,N02.TH1,N02.TH2
        const thIndexByGroup = {};
        for (let i = 0; i < numTH; i++) {
            const parentIndex = offerings.length > 0 ? (i % offerings.length) : null;
            const parentCodeForTH = parentIndex !== null ? offerings[parentIndex]?.code : null;
            const parentGroupForTH = parentIndex !== null ? offerings[parentIndex]?.group : formatGroup(0);
            thIndexByGroup[parentGroupForTH] = (thIndexByGroup[parentGroupForTH] ?? 0) + 1;
            const thIndexInGroup = thIndexByGroup[parentGroupForTH] - 1;
            offerings.push({
                code: generateClassCode(course.courseCode, 'TH', thIndexInGroup, parentGroupForTH),
                classType: 'TH',
                courseCode: course.courseCode,
                parentCode: parentCodeForTH,
                group: parentGroupForTH,
                isNew: true,
                isPrimary: false,
                plannedSize: practicalSizes[i] ?? null,
                requiredRoomType: roomTypeForTH !== 'NONE' ? roomTypeForTH : null,
            });
        }
        
        return offerings;
    };

    const handleCourseChange = (courseId) => {
        const course = courses.find(c => c.id === courseId);
        setSelectedCourse(course);
        setNumTheoryClasses(1);
        setNumPracticalClasses(0); // Reset khi đổi course
        setTheoryRequiredRoomType('NONE');
        setPracticalRequiredRoomType('NONE');
        const detected = detectDefaultClassType(course);
        setTheoryClassType(detected);
        setPreviewOfferings(generatePreviewOfferings(course, 1, 0, 'NONE', 'NONE', detected, totalPlannedSize));
        form.setFieldsValue({
            numTheoryClasses: 1,
            numPracticalClasses: 0,
            classType: detected,
        });
    };

    const handleNumTHChange = (num) => {
        setNumPracticalClasses(num);
        if (selectedCourse) {
            setPreviewOfferings(generatePreviewOfferings(selectedCourse, numTheoryClasses, num, theoryRequiredRoomType, practicalRequiredRoomType, theoryClassType, totalPlannedSize));
        }
    };

    const handleNumLTChange = (num) => {
        setNumTheoryClasses(num);
        if (selectedCourse) {
            setPreviewOfferings(generatePreviewOfferings(selectedCourse, num, numPracticalClasses, theoryRequiredRoomType, practicalRequiredRoomType, theoryClassType, totalPlannedSize));
        }
    };

    const handleTheoryRoomTypeChange = (val) => {
        setTheoryRequiredRoomType(val);
        if (selectedCourse) {
            setPreviewOfferings(generatePreviewOfferings(selectedCourse, numTheoryClasses, numPracticalClasses, val, practicalRequiredRoomType, theoryClassType, totalPlannedSize));
        }
    };

    const handlePracticalRoomTypeChange = (val) => {
        setPracticalRequiredRoomType(val);
        if (selectedCourse) {
            setPreviewOfferings(generatePreviewOfferings(selectedCourse, numTheoryClasses, numPracticalClasses, theoryRequiredRoomType, val, theoryClassType, totalPlannedSize));
        }
    };

    const handleTheoryTypeChange = (val) => {
        setTheoryClassType(val);
        if (selectedCourse) {
            setPreviewOfferings(generatePreviewOfferings(selectedCourse, numTheoryClasses, numPracticalClasses, theoryRequiredRoomType, practicalRequiredRoomType, val, totalPlannedSize));
        }
    };

    const handleTotalPlannedSizeChange = (val) => {
        const num = Math.max(1, Number(val) || 1);
        setTotalPlannedSize(num);
        if (selectedCourse) {
            setPreviewOfferings(generatePreviewOfferings(selectedCourse, numTheoryClasses, numPracticalClasses, theoryRequiredRoomType, practicalRequiredRoomType, theoryClassType, num));
        }
    };

    const handleOpenAssignModal = (record) => {
        setSelectedOffering(record);
        setSelectedLecturerId(record.lecturer ? record.lecturer.id : null);
        setAssignModalVisible(true);
    };

    const handleAssignLecturer = async () => {
        try {
            await axiosClient.put(`/offerings/${selectedOffering.id}/lecturer`, null, {
                params: { lecturerId: selectedLecturerId }
            });
            message.success("Đã gán giảng viên thành công");
            setAssignModalVisible(false);
            fetchData();
        } catch {
            message.error("Gán giảng viên thất bại");
        }
    };

    const openCreateModal = () => {
        setEditingOffering(null);
        setSelectedCourse(null);
        setNumTheoryClasses(1);
        setNumPracticalClasses(0);
        setTheoryRequiredRoomType('NONE');
        setPracticalRequiredRoomType('NONE');
        setTotalPlannedSize(60);
        setPreviewOfferings([]);
        form.resetFields();
        setParentOptions([]);
        setEditModalVisible(true);
    };

    const openEditModal = (record) => {
        setEditingOffering(record);
        const targetCodes = (record.targetClasses || '')
            .split(';')
            .map(s => s.trim())
            .filter(Boolean);
        form.setFieldsValue({
            code: record.code,
            courseId: record.course?.id,
            plannedSize: record.plannedSize,
            targetClasses: targetCodes,
            classType: record.classType || 'ALL',
            parentCode: record.parent?.code,
        });
        // Build parent options: same course, LT/ALL/ELN, exclude current
        const opts = data
            .filter(off =>
                off.course &&
                record.course &&
                off.course.id === record.course.id &&
                ['LT', 'ALL', 'ELN', 'COURSERA'].includes((off.classType || 'ALL').toUpperCase()) &&
                off.id !== record.id
            )
            .map(off => ({
                value: off.code,
                label: `${off.code}${off.targetClasses ? ` - ${off.targetClasses}` : ''}`,
            }));
        setParentOptions(opts);
        setEditModalVisible(true);
    };

    const handleSaveOffering = async () => {
        try {
            const values = await form.validateFields();
            
            // Nếu đang edit, giữ logic cũ
            if (editingOffering) {
                const payload = {
                    code: values.code,
                    plannedSize: values.plannedSize,
                    targetClasses: Array.isArray(values.targetClasses)
                        ? values.targetClasses.join(';')
                        : (values.targetClasses || ''),
                    classType: values.classType,
                    course: { id: values.courseId },
                };
                if (values.parentCode) {
                    payload.parent = { code: values.parentCode };
                }
                await axiosClient.put(`/offerings/${editingOffering.id}`, payload);
                message.success('Đã cập nhật mở lớp');
            } else {
                // Nếu tạo mới và có TH → batch create
                if (previewOfferings.length > 0) {
                    // Lấy target classes từ form
                    const targetClasses = Array.isArray(values.targetClasses)
                        ? values.targetClasses.join(';')
                        : (values.targetClasses || '');
                    
                    const theoryOfferings = previewOfferings.filter(o => o.isPrimary);
                    const practicalOfferings = previewOfferings.filter(o => !o.isPrimary);
                    
                    // Tạo các lớp LT/ELN/COURSERA
                    for (const tOffering of theoryOfferings) {
                        const tPayload = {
                            code: tOffering.code,
                            plannedSize: tOffering.plannedSize ?? values.plannedSize ?? 60,
                            targetClasses: targetClasses,
                            classType: tOffering.classType,
                            course: { id: values.courseId },
                            ...(tOffering.requiredRoomType ? { requiredRoomType: tOffering.requiredRoomType } : {}),
                        };
                        await axiosClient.post('/offerings', tPayload);
                    }
                    
                    // Tạo lớp TH (nếu có)
                    for (const pOffering of practicalOfferings) {
                        const thPayload = {
                            code: pOffering.code,
                            plannedSize: pOffering.plannedSize ?? values.plannedSize ?? 60,
                            targetClasses: targetClasses,
                            classType: 'TH',
                            course: { id: values.courseId },
                            ...(pOffering.parentCode ? { parent: { code: pOffering.parentCode } } : {}),
                        };
                        await axiosClient.post('/offerings', thPayload);
                    }
                    message.success(`Đã tạo ${theoryOfferings.length} lớp ${theoryOfferings[0]?.classType || 'LT'} + ${practicalOfferings.length} lớp TH`);
                } else {
                    // Fallback: tạo single offering
                    const payload = {
                        code: values.code,
                        plannedSize: values.plannedSize,
                        targetClasses: Array.isArray(values.targetClasses)
                            ? values.targetClasses.join(';')
                            : (values.targetClasses || ''),
                        classType: values.classType,
                        course: { id: values.courseId },
                    };
                    await axiosClient.post('/offerings', payload);
                    message.success('Đã tạo mở lớp');
                }
            }

            setEditModalVisible(false);
            fetchData();
        } catch (error) {
            if (error?.errorFields) return; // validation error
            message.error('Lưu mở lớp thất bại');
        }
    };

    const handleDeleteOffering = async (record) => {
        try {
            await axiosClient.delete(`/offerings/${record.id}`);
            message.success('Đã xóa mở lớp');
            fetchData();
        } catch {
            message.error('Xóa mở lớp thất bại');
        }
    };

    const columns = [
        {
            title: 'Mã lớp',
            dataIndex: 'code',
            key: 'code',
            width: 250,
            sorter: (a, b) => (a.code || '').localeCompare(b.code || ''),
            sortDirections: ['ascend', 'descend'],
            render: (text) => <Text strong copyable>{text}</Text>,
        },
        // --- CỘT MỚI: TYPE & PARENT ---
        {
            title: 'Loại',
            key: 'type',
            width: 100,
            align: 'center',
            sorter: (a, b) => ((a.classType || '')).localeCompare(b.classType || ''),
            sortDirections: ['ascend', 'descend'],
            render: (_, record) => {
                let color = 'default';
                let typeText = record.classType || 'ALL';

                if (typeText === 'LT') color = 'purple';
                if (typeText === 'TH') color = 'orange';
                if (typeText === 'ELN') color = 'cyan';
                if (typeText === 'COURSERA') color = 'blue';

                return (
                    <Space direction="vertical" size={0}>
                        <Tag color={color} style={{ fontWeight: 600 }}>{typeText}</Tag>
                        
                        {/* Nếu là lớp TH và có Parent, hiển thị mã Parent */}
                        {typeText === 'TH' && record.parent && (
                            <Tooltip title={`Lớp mẹ: ${record.parent.code}`}>
                                <div style={{ fontSize: '11px', color: '#888', marginTop: 4 }}>
                                    <NodeIndexOutlined /> Mẹ: {record.parent.code.split('-').pop()}...
                                </div>
                            </Tooltip>
                        )}
                    </Space>
                );
            }
        },
        // -----------------------------
        {
            title: 'Học phần',
            dataIndex: 'course',
            key: 'course',
            sorter: (a, b) => ((a.course?.name || '')).localeCompare(b.course?.name || ''),
            sortDirections: ['ascend', 'descend'],
            render: (course) => (
                <div>
                    <div style={{ fontWeight: 500 }}>{course.name}</div>
                    <Tag color="cyan">{course.courseCode}</Tag>
                </div>
            )
        },
        {
            title: 'Sĩ số',
            dataIndex: 'plannedSize',
            key: 'plannedSize',
            align: 'center',
            width: 100,
            sorter: (a, b) => (a.plannedSize || 0) - (b.plannedSize || 0),
            sortDirections: ['ascend', 'descend'],
            render: (size) => <Tag color="volcano">{size}</Tag>
        },
        {
            title: 'Giảng viên phụ trách',
            dataIndex: 'lecturer',
            key: 'lecturer',
            sorter: (a, b) => ((a.lecturer?.fullName || '')).localeCompare(b.lecturer?.fullName || ''),
            sortDirections: ['ascend', 'descend'],
            render: (lecturer, record) => {
                // Logic check quyền: chỉ đơn vị quản lý học phần mới được phân công
                const myFacultyId = user?.facultyId;
                const role = user?.role;
                const managingFacultyId = record.course.managingFaculty?.id;
                const isFacultyManaged = !!managingFacultyId;
                const canAssign = (role === 'ADMIN') ||
                    (isFacultyManaged
                        ? (myFacultyId && myFacultyId === managingFacultyId)
                        : (role === 'ADMIN_SCHOOL'));

                const managingUnitName = isFacultyManaged
                    ? record.course.managingFaculty?.name
                    : record.course.school?.name;

                return (
                    <Space>
                        {lecturer ? <Tag color="blue">{lecturer.fullName}</Tag> : <Text type="secondary">Chưa gán</Text>}
                        
                        {/* Chỉ hiện nút Gán/Sửa nếu có quyền */}
                        {canAssign && (
                            <Button size="small" icon={<EditOutlined />} onClick={() => handleOpenAssignModal(record)}>
                                {lecturer ? 'Đổi GV' : 'Gán GV'}
                            </Button>
                        )}
                        
                        {!canAssign && !lecturer && (
                            <Tooltip title={`Chờ ${isFacultyManaged ? 'khoa' : 'trường'} ${managingUnitName || 'khác'} phân công`}>
                                <Tag color="warning">Chờ đơn vị khác</Tag>
                            </Tooltip>
                        )}
                    </Space>
                );
            }
        },
        {
            title: 'Thao tác',
            key: 'actions',
            width: 'auto',
            render: (_, record) => (
                <Space>
                    <Button size="small" icon={<EditOutlined />} onClick={() => openEditModal(record)}>
                        Sửa
                    </Button>
                    <Popconfirm
                        title="Xóa mở lớp"
                        description={`Bạn có chắc muốn xóa ${record.code}?`}
                        onConfirm={() => handleDeleteOffering(record)}
                    >
                        <Button size="small" danger icon={<DeleteOutlined />}>Xóa</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ];

    return (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>Quản lý mở lớp học phần</Title>
                    <Text type="secondary">Nhập nhu cầu phục vụ xếp thời khóa biểu</Text>
                </div>
                <Space>
                    <Button
                        type="primary"
                        icon={<PlusOutlined />}
                        onClick={openCreateModal}
                    >
                        Thêm mở lớp
                    </Button>
                    <Button 
                        type="primary" 
                        danger // Đổi màu đỏ cho nổi bật
                        icon={<UserSwitchOutlined />} 
                        onClick={handleAutoAssign}
                        loading={loading}
                    >
                        Tự phân công giảng viên
                    </Button>

                    <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                        Mẫu
                    </Button>
                    <Upload {...uploadProps}>
                        <Button type="default" icon={<UploadOutlined />}>Nhập kế hoạch</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined />} onClick={fetchData} />
                </Space>
            </div>

            <Card bodyStyle={{ padding: 0 }}>
                <Table 
                    columns={columns} 
                    dataSource={data} 
                    rowKey="id"
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>

            <Modal
                title={`Gán giảng viên cho ${selectedOffering?.code}`}
                open={assignModalVisible}
                onOk={handleAssignLecturer}
                onCancel={() => setAssignModalVisible(false)}
            >
                <Text>Chọn giảng viên:</Text>
                <Select
                    style={{ width: '100%', marginTop: 8 }}
                    placeholder="Chọn giảng viên"
                    value={selectedLecturerId}
                    onChange={setSelectedLecturerId}
                    showSearch
                    optionFilterProp="children"
                    allowClear
                    filterOption={(input, option) =>
                        (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                    }
                >
                    {lecturers.map(lec => (
                        <Select.Option key={lec.id} value={lec.id}>
                            {lec.fullName} ({lec.lecturerCode}) - {lec.faculty?.name}
                        </Select.Option>
                    ))}
                </Select>
            </Modal>

            {/* Smart Offering Modal */}
            <Modal
                title={editingOffering ? `Sửa mở lớp: ${editingOffering.code}` : 'Thêm mở lớp (Tự động)'}
                open={editModalVisible}
                onOk={handleSaveOffering}
                onCancel={() => setEditModalVisible(false)}
                destroyOnClose
                width={1100}
            >
                <Form form={form} layout="vertical">
                    {/* === PHẦN 1: CHỌN HỌC PHẦN === */}
                    <Form.Item
                        label={<Text strong>Bước 1: Chọn học phần cần mở lớp</Text>}
                        name="courseId"
                        rules={[{ required: true, message: 'Vui lòng chọn học phần' }]}
                    >
                        <Select
                            showSearch
                            placeholder="Chọn học phần..."
                            optionFilterProp="children"
                            filterOption={(input, option) =>
                                (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                            }
                            onChange={handleCourseChange}
                        >
                            {courses.map(c => (
                                <Select.Option key={c.id} value={c.id}>
                                    <strong>{c.courseCode}</strong> - {c.name}
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>

                    {selectedCourse && !editingOffering && (
                        <>
                            <Row gutter={16}>
                                {/* LEFT: cấu hình lớp */}
                                <Col xs={24} md={12}>
                                    <Form.Item
                                        label={<Text strong>Bước 2: Hình thức học lý thuyết</Text>}
                                        name="theoryClassType"
                                        initialValue={theoryClassType}
                                    >
                                        <Select onChange={handleTheoryTypeChange}>
                                            <Select.Option value="LT">LT (tại lớp)</Select.Option>
                                            <Select.Option value="ELN">ELN (online)</Select.Option>
                                            <Select.Option value="COURSERA">COURSERA</Select.Option>
                                        </Select>
                                    </Form.Item>

                                    <Form.Item
                                        label={<Text strong>Bước 2: Số lớp lý thuyết</Text>}
                                        name="numTheoryClasses"
                                        initialValue={numTheoryClasses}
                                    >
                                        <Input
                                            type="number"
                                            min={0}
                                            max={10}
                                            placeholder="Nhập số lớp LT/ELN/COURSERA (0 nếu không mở)"
                                            onChange={(e) => handleNumLTChange(parseInt(e.target.value) || 0)}
                                        />
                                    </Form.Item>

                                    <Form.Item
                                        label={<Text strong>Tuỳ chọn: Phòng yêu cầu cho lớp LT</Text>}
                                        name="theoryRequiredRoomType"
                                        initialValue={theoryRequiredRoomType}
                                    >
                                        <Select onChange={handleTheoryRoomTypeChange}>
                                            <Select.Option value="NONE">Không yêu cầu</Select.Option>
                                            <Select.Option value="PC">PC (phòng máy tính)</Select.Option>
                                            <Select.Option value="LAB">LAB (phòng thí nghiệm/thực hành)</Select.Option>
                                        </Select>
                                    </Form.Item>

                                    <Form.Item
                                        label={<Text strong>Tuỳ chọn: Phòng yêu cầu cho lớp TH</Text>}
                                        name="practicalRequiredRoomType"
                                        initialValue={practicalRequiredRoomType}
                                    >
                                        <Select onChange={handlePracticalRoomTypeChange}>
                                            <Select.Option value="NONE">Tự chọn (LAB hoặc PC)</Select.Option>
                                            <Select.Option value="PC">PC (phòng máy tính)</Select.Option>
                                            <Select.Option value="LAB">LAB (phòng thí nghiệm/thực hành)</Select.Option>
                                        </Select>
                                    </Form.Item>

                                    <Form.Item
                                        label={<Text strong>Bước 3: Số lớp thực hành (TH)</Text>}
                                        name="numPracticalClasses"
                                        initialValue={numPracticalClasses}
                                    >
                                        <Input
                                            type="number"
                                            min={0}
                                            max={10}
                                            placeholder="Nhập số lớp TH (0 nếu không có TH)"
                                            onChange={(e) => handleNumTHChange(parseInt(e.target.value) || 0)}
                                        />
                                    </Form.Item>
                                </Col>

                                {/* RIGHT: tổng sĩ số + lớp hành chính + preview */}
                                <Col xs={24} md={12}>
                                    <Form.Item
                                        label="Tổng sĩ số (toàn bộ học phần)"
                                        name="plannedSize"
                                        initialValue={totalPlannedSize}
                                        rules={[{ required: true, message: 'Vui lòng nhập sĩ số' }]}
                                    >
                                        <Input
                                            type="number"
                                            min={1}
                                            placeholder="vd: 120"
                                            onChange={(e) => handleTotalPlannedSizeChange(e.target.value)}
                                        />
                                    </Form.Item>

                                    <Form.Item
                                        label="Lớp hành chính mục tiêu"
                                        name="targetClasses"
                                    >
                                        <Select
                                            mode="multiple"
                                            allowClear
                                            placeholder="Chọn lớp hành chính"
                                            showSearch
                                            optionFilterProp="children"
                                            filterOption={(input, option) =>
                                                (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                                            }
                                        >
                                            {adminClasses.map(cls => (
                                                <Select.Option key={cls.id} value={cls.name}>
                                                    {cls.name} {cls.code ? `(${cls.code})` : ''}
                                                </Select.Option>
                                            ))}
                                        </Select>
                                    </Form.Item>

                                    {previewOfferings.length > 0 && (
                                        <Card
                                            size="small"
                                            title={<Text strong>Lớp sẽ được tạo (đã chia sĩ số)</Text>}
                                            style={{ marginTop: 8 }}
                                        >
                                            <Space direction="vertical" style={{ width: '100%' }}>
                                                {previewOfferings.map((off, idx) => (
                                                    <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
                                                        <span style={{ flex: 1, minWidth: 0 }}>
                                                            <Tag color={off.classType === 'TH' ? 'orange' : off.classType === 'ELN' ? 'cyan' : off.classType === 'COURSERA' ? 'blue' : 'purple'}>
                                                                {off.classType}
                                                            </Tag>
                                                            <Text strong style={{ marginLeft: 8 }} ellipsis>
                                                                {off.code}
                                                            </Text>
                                                            {off.parentCode && (
                                                                <Tooltip title={`Lớp mẹ: ${off.parentCode}`}>
                                                                    <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
                                                                        (mẹ)
                                                                    </Text>
                                                                </Tooltip>
                                                            )}
                                                        </span>
                                                        <Space size={6}>
                                                            {typeof off.plannedSize === 'number' && (
                                                                <Tag color="volcano">{off.plannedSize}</Tag>
                                                            )}
                                                            {off.requiredRoomType && (
                                                                <Tag color="geekblue">{off.requiredRoomType}</Tag>
                                                            )}
                                                        </Space>
                                                    </div>
                                                ))}
                                            </Space>
                                        </Card>
                                    )}
                                </Col>
                            </Row>
                        </>
                    )}

                    {/* === FALLBACK: EDIT MODE (MODE CŨ) === */}
                    {editingOffering && (
                        <>
                            <Form.Item
                                label="Mã lớp"
                                name="code"
                                rules={[{ required: true, message: 'Vui lòng nhập mã lớp' }]}
                            >
                                <Input placeholder="vd: 2025_JAVA_01" />
                            </Form.Item>

                            <Form.Item
                                label="Sĩ số dự kiến"
                                name="plannedSize"
                                rules={[{ required: true, message: 'Vui lòng nhập sĩ số dự kiến' }]}
                            >
                                <Input type="number" min={1} placeholder="vd: 60" />
                            </Form.Item>

                            <Form.Item
                                label="Lớp hành chính mục tiêu"
                                name="targetClasses"
                            >
                                <Select
                                    mode="multiple"
                                    allowClear
                                    placeholder="Chọn lớp hành chính"
                                    showSearch
                                    optionFilterProp="children"
                                    filterOption={(input, option) =>
                                        (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                                    }
                                >
                                    {adminClasses.map(cls => (
                                        <Select.Option key={cls.id} value={cls.name}>
                                            {cls.name} {cls.code ? `(${cls.code})` : ''}
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>

                            <Form.Item
                                label="Loại lớp"
                                name="classType"
                            >
                                <Select>
                                    <Select.Option value="LT">LT</Select.Option>
                                    <Select.Option value="TH">TH</Select.Option>
                                    <Select.Option value="ELN">ELN</Select.Option>
                                    <Select.Option value="ALL">ALL</Select.Option>
                                    <Select.Option value="COURSERA">COURSERA</Select.Option>
                                </Select>
                            </Form.Item>

                            <Form.Item
                                label="Lớp mẹ (cho TH)"
                                name="parentCode"
                            >
                                <Select
                                    allowClear
                                    placeholder="Chọn lớp mẹ"
                                    showSearch
                                    optionFilterProp="children"
                                    filterOption={(input, option) =>
                                        (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                                    }
                                >
                                    {parentOptions.map(opt => (
                                        <Select.Option key={opt.value} value={opt.value}>
                                            {opt.label}
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                        </>
                    )}
                </Form>
            </Modal>
        </Space>
    );
};

export default OfferingManagement;