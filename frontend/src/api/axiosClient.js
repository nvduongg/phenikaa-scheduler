// src/api/axiosClient.js
import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080/api/v1', // Đảm bảo Backend đang chạy port này
    headers: {
        'Content-Type': 'application/json',
    },
});

export default axiosClient;