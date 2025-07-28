# Secure Chat & Subdomain Scanner

## 📋 Tổng quan

Đây là ứng dụng **Client-Server** hoàn chỉnh được xây dựng bằng Java, tích hợp **mã hóa bảo mật** và **công cụ quét subdomain**. Ứng dụng sử dụng kiến trúc **Netty** cho giao tiếp mạng, **RSA/AES** để mã hóa, và cung cấp giao diện **GUI** hiện đại.

---

## ✨ Tính năng chính

### 🔐 Bảo mật nâng cao

- Mã hóa **RSA 2048-bit** cho trao đổi khóa
- Mã hóa **AES-CBC** cho dữ liệu truyền tải
- Chữ ký số **SHA256withRSA** để xác thực
- Quản lý khóa an toàn với định dạng **PEM/DER**
- Lưu trữ khóa AES trong **SQLite database**

### 🌐 Quét Subdomain

- Quét subdomain từ wordlist với hơn **1,000,000 entries**
- **Multi-threading** tối ưu với 1000 luồng đồng thời
- Hỗ trợ **DNS lookup** và kiểm tra HTTP
- Thống kê chi tiết kết quả quét
- Cho phép tùy chỉnh target domain

### 💻 Giao diện Người dùng

- **GUI hiện đại** sử dụng giao diện FlatLaf theme
- Hiển thị log theo thời gian thực (timestamp)
- Giám sát kết nối (connection monitoring)
- Quản lý khóa trực quan
- Kết quả quét hiển thị dạng bảng dễ quan sát

### 🚀 Kiến trúc Mạng

- Sử dụng **Netty framework** để đạt hiệu suất cao
- **Asynchronous I/O** cho xử lý không chặn
- **Connection pooling** giúp tái sử dụng kết nối
- Chuẩn hóa trao đổi dữ liệu với **JSON (Jackson)**
- Giao thức dòng (line-based protocol) để tăng ổn định

---

## 🏗️ Kiến trúc Hệ thống

```
┌─────────────────┐    ┌─────────────────┐
│   Client App    │    │   Server App    │
│ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │    GUI      │ │    │ │    GUI      │ │
│ │   Client    │ │    │ │   Server    │ │
│ └─────────────┘ │    │ └─────────────┘ │
│ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │   Crypto    │ │    │ │   Crypto    │ │
│ │   Utils     │ │    │ │   Utils     │ │
│ └─────────────┘ │    │ └─────────────┘ │
│ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │   Netty     │◄┼────┼►│   Netty     │ │
│ │  Client     │ │    │ │  Server     │ │
│ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    │ ┌─────────────┐ │
                       │ │ Subdomain   │ │
                       │ │ Scanner     │ │
                       │ └─────────────┘ │
                       │ ┌─────────────┐ │
                       │ │ Key/IV      │ │
                       │ │ Database    │ │
                       │ └─────────────┘ │
                       └─────────────────┘
```

---

## 🧰 Công nghệ sử dụng

### Backend

- Java 24
- Netty 4.1.99.Final
- BouncyCastle 1.76 (Thư viện mã hóa)
- Jackson 2.17.1 (JSON serialization)
- SQLite 3.45.3.0 (Database dùng để lưu trữ AES keys và IV)

### Frontend

- Swing (Java GUI Framework)
- FlatLaf 3.0 (Giao diện hiện đại)
- GridBagLayout (Layout responsive)

### Security

- RSA 2048-bit (Mã hóa bất đối xứng)
- AES-CBC 256-bit (Mã hóa đối xứng)
- SHA256withRSA (Chữ ký số)
- Hỗ trợ định dạng khóa PKCS8 và PKCS1

---

## 📦 Hướng dẫn Cài đặt và Chạy

### Yêu cầu hệ thống

- Java phiên bản 24 hoặc mới hơn
- Maven phiên bản 3.8+
- RAM tối thiểu 2GB (để đảm bảo hiệu năng quét subdomain)

### Cài đặt

```
# Clone source code
git clone https://github.com/yourusername/secure-chat-scanner.git
cd secure-chat-scanner

# Build dự án
mvn clean compile

# Chạy Server
mvn exec:java -Dexec.mainClass="server.app.AppServer"

# Chạy Client (ở terminal khác)
mvn exec:java -Dexec.mainClass="client.app.AppClient"
```

### Tạo khóa RSA

1. Khởi động Server trước
2. Trên giao diện Server, click nút **"Tạo cặp khóa"**
3. Copy khóa public từ Server sang Client
4. Trên Client, click nút **"Tạo khóa"**

---

## 🎯 Cách sử dụng

### Khởi động hệ thống

```
# Terminal 1: Chạy Server
java -cp target/classes server.app.AppServer

# Terminal 2: Chạy Client
java -cp target/classes client.app.AppClient
```

### Thiết lập kết nối

1. Server tự động chạy trên port 8080
2. Client nhập IP Server và nhấn **"🔗 Kết nối"**
3. Server gửi khóa public cho Client để xác thực

### Gửi tin nhắn bảo mật

1. Nhập nội dung tin nhắn vào trường text
2. Chọn khóa private và public phù hợp
3. Nhập domain cần quét (mặc định: huflit.edu.vn)
4. Nhấn **"Gửi tin nhắn bảo mật"**

### Xem kết quả quét

- Server hiển thị log chi tiết và danh sách subdomain được quét
- Client hiển thị thống kê kết quả một cách chi tiết

---

## 🔐 Quy trình bảo mật

1. **Trao đổi khóa:**

```
Client ↔ Server: Trao đổi khóa RSA Public Key
```

2. **Mã hóa tin nhắn:**

```
Tin nhắn → Ký số RSA → Mã hóa AES → Gửi đi
```

3. **Xác thực & Giải mã:**

```
Nhận dữ liệu → Giải mã AES → Xác nhận chữ ký RSA → Xử lý
```

4. **Lưu trữ khóa:**

- AES key và IV được lưu trong SQLite (Server)
- RSA Keys được lưu dưới dạng PEM file (Client và Server)

---

## 📊 Hiệu suất

### Quét Subdomain

- Wordlist với hơn 1,000,000 entries
- Chạy đa luồng với 1000 threads đồng thời
- Timeout 3 giây cho mỗi domain
- Đạt throughput ~10,000 domain/phút

### Mã hóa

- Tạo khóa RSA 2048-bit nhanh chóng
- AES 256-bit đảm bảo bảo mật dữ liệu
- Độ trễ < 100ms cho mỗi tin nhắn

---

## 🗂️ Cấu trúc dự án

```
src/main/java/
├── client/
│   ├── app/          # Điểm vào Client
│   ├── crypto/       # Thư viện mã hóa
│   ├── gui/          # Client GUI
│   ├── handler/      # Netty handler cho client
│   ├── model/        # Định nghĩa model dữ liệu
│   └── network/      # Netty client
├── server/
│   ├── app/          # Điểm vào Server
│   ├── crypto/       # Thư viện mã hóa
│   ├── gui/          # Server GUI
│   ├── handler/      # Netty handler cho server
│   ├── model/        # Định nghĩa model
│   ├── network/      # Netty server
│   ├── scanner/      # Module quét subdomain
│   └── utils/        # Công cụ Database
└── resources/
    ├── wordlist/     # File wordlist subdomain
    └── keys/         # File khóa mã hóa
```

---

## 🧪 Testing

### Unit Tests

```
mvn test
```

### Integration Tests (Run server và client đồng thời)

```
# Server
mvn exec:java -Dexec.mainClass="server.app.AppServer" &

# Client
mvn exec:java -Dexec.mainClass="client.app.AppClient"
```

---

## 🚀 Tính năng nâng cao

- **Quản lý database**: Tự động cleanup các bản ghi AES key cũ, thống kê số liệu sử dụng
- **Tối ưu mạng**: Connection pooling, giữ kết nối (keep-alive), xử lý timeout hiệu quả
- **Bảo mật nâng cao**: Xoay khóa tự động (key rotation), xác thực chữ ký tin nhắn, lưu trữ khóa private an toàn

---

## 🤝 Đóng góp

1. Fork dự án
2. Tạo branch cho tính năng mới (`git checkout -b feature/AmazingFeature`)
3. Commit thay đổi (`git commit -m 'Add AmazingFeature'`)
4. Push branch lên fork (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request trên GitHub

---

## 🏆 Acknowledgments

- **Netty Team** - Framework mạng hiệu suất cao
- **BouncyCastle** - Thư viện mã hóa nổi tiếng
- **FlatLaf** - Giao diện UI hiện đại
- **SQLite** - Cơ sở dữ liệu nhúng tiện lợi

---

⭐ Nếu bạn thấy dự án hữu ích, đừng quên **star** để ủng hộ nhé!

```
