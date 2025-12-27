// src/api/axiosClient.js
import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080/api/v1', // Đảm bảo Backend đang chạy port này
    headers: {
        'Content-Type': 'application/json',
    },
});

// Interceptor: Tự động chèn Token vào mỗi request
axiosClient.interceptors.request.use((config) => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (user && user.token) {
        config.headers.Authorization = `Bearer ${user.token}`;
    }
    return config;
});

export default axiosClient;