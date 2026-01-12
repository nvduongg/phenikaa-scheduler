# Phenikaa Scheduler

Hệ thống quản lý thời khóa biểu (Timetable Management) cho Đại học Phenikaa, gồm Backend (Spring Boot) và Frontend (React + Vite). Dự án hỗ trợ quản lý trường/khoa, ngành, môn học, lớp hành chính, giảng viên, phòng học, khung chương trình, mở lớp, xếp lịch tự động (Genetic Algorithm), thống kê tải giảng dạy và nhiều tiện ích khác.

## Kiến trúc
- Backend: Spring Boot REST API (Java 21) phục vụ dữ liệu và nghiệp vụ.
- Frontend: Ứng dụng React dùng Ant Design, giao tiếp với API.
- Database: PostgreSQL. Có `compose.yaml` để khởi động nhanh DB bằng Docker.

## Công nghệ chính
- Backend: Spring Boot 3.5.x, Spring Data JPA, Spring Security (JWT), Hibernate, Lombok, PostgreSQL.
- Frontend: React 19, Vite 7, Ant Design.
- Khác: Apache POI (Excel), OpenCSV, Commons IO.

## Yêu cầu hệ thống
- Java 21 (JDK 21)
- Maven 3.9+
- Node.js 18+ và npm 9+
- Docker & Docker Compose (tùy chọn, để chạy PostgreSQL nhanh)

## Khởi chạy nhanh
### 1) Khởi động Database bằng Docker
```bash
# Chạy PostgreSQL ở nền
docker compose up -d

# Kiểm tra container
docker ps | grep pk_scheduler_db
```
Thông tin mặc định:
- Host: `localhost`, Port: `5432`
- Database: `scheduler_db`
- User/Password: `admin` / `admin`

### 2) Chạy Backend (API)
```bash
cd backend
# Cách 1: chạy trực tiếp
mvn spring-boot:run

# Cách 2: đóng gói và chạy jar
mvn clean package
java -jar target/scheduler-0.0.1-SNAPSHOT.jar
```
Mặc định API chạy tại: `http://localhost:8080/api/v1`.

Cấu hình kết nối DB nằm tại `backend/src/main/resources/application.properties`:
```
spring.datasource.url=jdbc:postgresql://localhost:5432/scheduler_db
spring.datasource.username=admin
spring.datasource.password=admin
server.port=8080
```
Khi ứng dụng khởi động, hệ thống chỉ khởi tạo tài khoản `admin` mặc định (không tạo sẵn School/Faculty).

### 3) Chạy Frontend (UI)
```bash
cd frontend
npm install
npm run dev
```
Frontend chạy tại `http://localhost:5173` (mặc định của Vite). Base URL API đã cấu hình tại `frontend/src/api/axiosClient.js`:
```js
baseURL: 'http://localhost:8080/api/v1'
```

## Tài khoản mặc định
Sau khi Backend khởi động, `DataInitializer` chỉ tạo tài khoản quản trị:
- ADMIN:
  - Username: `admin`
  - Password: `123456`

Đăng nhập UI tại trang `/login` và sử dụng tài khoản trên.

## Cấu trúc thư mục
```
phenikaa-scheduler/
├── compose.yaml                # Docker Compose cho PostgreSQL
├── backend/                    # Spring Boot API
│   ├── pom.xml                 # Khai báo dependencies
│   └── src/main/java/com/phenikaa/scheduler/
│       ├── controller/         # REST Controllers
│       ├── model/              # Entities
│       ├── repository/         # JPA Repositories
│       ├── service/            # Business Services
│       ├── security/           # JWT & Security
│       ├── core/               # GeneticAlgorithm (xếp lịch)
│       └── config/             # Khởi tạo dữ liệu, cấu hình
├── frontend/                   # React + Vite UI
│   ├── src/components/         # Các màn quản trị
│   ├── src/layouts/            # Layout & menu
│   ├── src/pages/              # Login & routing
│   └── src/api/axiosClient.js  # Cấu hình API client
```

## Lệnh hữu ích
- Backend:
  - Kiểm thử: `mvn test`
  - Format/verify: `mvn -q -DskipTests package`
- Frontend:
  - Lint: `npm run lint`
  - Build: `npm run build`
  - Preview build: `npm run preview`

## Mẹo phát triển
- Database:
  - Sửa thông tin DB trong `application.properties` nếu không dùng Docker.
  - Bật/tắt log SQL qua `spring.jpa.show-sql=true/false`.
- Bảo mật:
  - Token JWT được tự động gắn ở client qua Interceptor trong `axiosClient.js`.
- API:
  - Namespace mặc định: `/api/v1`.
  - Có thể mở rộng Controllers và Services dưới `backend/src/main/java/com/phenikaa/scheduler/`.

## Sự cố thường gặp
- Không kết nối được DB: kiểm tra container PostgreSQL đang chạy và thông tin kết nối trong `application.properties`.
- CORS hoặc 401: chắc chắn Frontend trỏ tới đúng `baseURL` và bạn đăng nhập để lấy JWT.
- Lỗi build Frontend: kiểm tra phiên bản Node.js (đề xuất >= 18).

## Giấy phép
Dự án phục vụ mục đích nghiên cứu và phát triển nội bộ tại Phenikaa University.
