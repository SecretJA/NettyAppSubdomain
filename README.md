## 📋 Tổng quan



Đây là một ứng dụng \*\*Client-Server\*\* hoàn chỉnh được xây dựng bằng Java, tích hợp \*\*mã hóa bảo mật\*\* và \*\*công cụ quét subdomain\*\*. Ứng dụng sử dụng kiến trúc \*\*Netty\*\* cho giao tiếp mạng, \*\*RSA/AES\*\* cho mã hóa, và cung cấp giao diện \*\*GUI\*\* hiện đại.



## ✨ Tính năng chính



###  Bảo mật nâng cao

- **Mã hóa RSA 2048-bit** cho trao đổi khóa

- **Mã hóa AES-CBC** cho dữ liệu truyền tải

- **Chữ ký số SHA256withRSA** để xác thực

- **Quản lý khóa an toàn** với PEM/DER format

- **Lưu trữ khóa AES** trong SQLite database



### 🌐 Quét Subdomain

- **Quét subdomain** từ wordlist với 1,000,000+ entries

- **Multi-threading** tối ưu (1000 threads)

- **DNS lookup** và HTTP verification

- **Thống kê chi tiết** về kết quả quét

- **Target domain** tùy chỉnh



### 💻 Giao diện người dùng

- **GUI hiện đại** với FlatLaf theme

- **Real-time logging** với timestamp

- **Connection monitoring** 

- **Key management** trực quan

\- \*\*Scan results\*\* hiển thị dạng bảng



### 🚀 Kiến trúc mạng

\- \*\*Netty framework\*\* cho hiệu suất cao

\- \*\*Asynchronous I/O\*\* 

\- \*\*Connection pooling\*\*

\- \*\*JSON serialization\*\* với Jackson

\- \*\*Line-based protocol\*\* cho ổn định



## 🏗️ Kiến trúc hệ thống



```

┌─────────────────┐    ┌─────────────────┐

│   Client App    │    │   Server App    │

│                 │    │                 │

│ ┌─────────────┐ │    │ ┌─────────────┐ │

│ │   GUI       │ │    │ │   GUI       │ │

│ │   Client    │ │    │ │   Server    │ │

│ └─────────────┘ │    │ └─────────────┘ │

│                 │    │                 │

│ ┌─────────────┐ │    │ ┌─────────────┐ │

│ │   Crypto    │ │    │ │   Crypto    │ │

│ │   Utils     │ │    │ │   Utils     │ │

│ └─────────────┘ │    │ └─────────────┘ │

│                 │    │                 │

│ ┌─────────────┐ │    │ ┌─────────────┐ │

│ │   Netty     │◄┼────┼►│   Netty     │ │

│ │   Client    │ │    │ │   Server    │ │

│ └─────────────┘ │    │ └─────────────┘ │

└─────────────────┘    │                 │

&nbsp;                      │ ┌─────────────┐ │

&nbsp;                      │ │ Subdomain   │ │

&nbsp;                      │ │ Scanner     │ │

&nbsp;                      │ └─────────────┘ │

&nbsp;                      │                 │

&nbsp;                      │ ┌─────────────┐ │

&nbsp;                      │ │ Key/IV      │ │

&nbsp;                      │ │ Database    │ │

&nbsp;                      │ └─────────────┘ │

&nbsp;                      └─────────────────┘

```



## ️ Công nghệ sử dụng



### Backend

\- \*\*Java 24\*\* - Ngôn ngữ lập trình chính

\- \*\*Netty 4.1.99.Final\*\* - Framework mạng

\- \*\*BouncyCastle 1.76\*\* - Thư viện mã hóa

\- \*\*Jackson 2.17.1\*\* - JSON serialization

\- \*\*SQLite 3.45.3.0\*\* - Database lưu trữ



### Frontend

\- \*\*Swing\*\* - GUI framework

\- \*\*FlatLaf 3.0\*\* - Modern UI theme

\- \*\*GridBagLayout\*\* - Responsive layout



### Security

\- \*\*RSA 2048-bit\*\* - Asymmetric encryption

\- \*\*AES-CBC\*\* - Symmetric encryption  

\- \*\*SHA256withRSA\*\* - Digital signatures

\- \*\*PKCS8/PKCS1\*\* - Key formats



## 📦 Cài đặt và chạy



### Yêu cầu hệ thống

\- Java 24 hoặc cao hơn

\- Maven 3.8+

\- RAM: 2GB+ (cho quét subdomain)



### Cài đặt

```bash

\# Clone repository

git clone https://github.com/yourusername/secure-chat-scanner.git

cd secure-chat-scanner



\# Build project

mvn clean compile



\# Chạy server

mvn exec:java -Dexec.mainClass="server.app.AppServer"



\# Chạy client (terminal khác)

mvn exec:java -Dexec.mainClass="client.app.AppClient"

```



### Tạo khóa RSA

1\. Chạy server trước

2\. Bấm nút \*\*"Tạo cặp khóa"\*\* trên GUI server

3\. Copy public key từ server sang client

4\. Tạo khóa client bằng nút \*\*"Tạo khóa"\*\* trên GUI client



##  Cách sử dụng



### 1. Khởi động hệ thống

```bash

\# Terminal 1 - Server

java -cp target/classes server.app.AppServer



\# Terminal 2 - Client  

java -cp target/classes client.app.AppClient

```



### 2. Thiết lập kết nối

1\. \*\*Server\*\*: Tự động khởi động tại port 8080

2\. \*\*Client\*\*: Nhập IP server và bấm \*\*"🔗 Kết nối"\*\*

3\. \*\*Xác thực\*\*: Server gửi public key, client xác nhận



### 3. Gửi tin nhắn bảo mật

1\. Nhập tin nhắn vào text field

2\. Chọn private key và public key

3\. Nhập target domain (mặc định: huflit.edu.vn)

4\. Bấm \*\*" Gửi tin nhắn bảo mật"\*\*



### 4. Xem kết quả quét

\- \*\*Server\*\*: Hiển thị log và danh sách subdomain

\- \*\*Client\*\*: Hiển thị thống kê chi tiết



## 🔐 Quy trình bảo mật



### 1. Trao đổi khóa

```

Client ←→ Server: RSA Public Key Exchange

```



### 2. Mã hóa tin nhắn

```

Raw Message → RSA Sign → AES Encrypt → Send

```



### 3. Xác thực và giải mã

```

Receive → AES Decrypt → RSA Verify → Process

```



### 4. Lưu trữ khóa

```

AES Key + IV → SQLite Database (Server)

RSA Keys → PEM Files (Client/Server)

```



## 📊 Hiệu suất



### Quét Subdomain

\- \*\*Wordlist\*\*: 1,000,000+ entries

\- \*\*Threads\*\*: 1000 concurrent

\- \*\*Timeout\*\*: 3 giây per domain

\- \*\*Throughput\*\*: ~10,000 domains/phút



### Mã hóa

\- \*\*RSA\*\*: 2048-bit key generation

\- \*\*AES\*\*: 256-bit encryption

\- \*\*Latency\*\*: <100ms per message



## 🗂️ Cấu trúc project



```

src/main/java/

├── client/

│   ├── app/          # Client entry point

│   ├── crypto/       # Encryption utilities

│   ├── gui/          # Client GUI

│   ├── handler/      # Netty handlers

│   ├── model/        # Data models

│   └── network/      # Netty client

├── server/

│   ├── app/          # Server entry point

│   ├── crypto/       # Encryption utilities

│   ├── gui/          # Server GUI

│   ├── handler/      # Netty handlers

│   ├── model/        # Data models

│   ├── network/      # Netty server

│   ├── scanner/      # Subdomain scanner

│   └── utils/        # Database utilities

└── resources/

&nbsp;   ├── wordlist/     # Subdomain wordlist

&nbsp;   └── keys/         # Key files

```



## 🧪 Testing



### Unit Tests

```bash

mvn test

```



### Integration Tests

```bash

\# Test server-client communication

mvn exec:java -Dexec.mainClass="server.app.AppServer" \&

mvn exec:java -Dexec.mainClass="client.app.AppClient"

```



##  Tính năng nâng cao



### 1. Database Management

\- \*\*SQLite\*\*: Lưu trữ AES keys và IVs

\- \*\*Cleanup\*\*: Tự động xóa records cũ

\- \*\*Statistics\*\*: Thống kê usage



### 2. Network Optimization

\- \*\*Connection pooling\*\*: Tái sử dụng connections

\- \*\*Keep-alive\*\*: Duy trì kết nối

\- \*\*Timeout handling\*\*: Xử lý lỗi mạng



### 3. Security Features

\- \*\*Key rotation\*\*: Thay đổi khóa định kỳ

\- \*\*Signature verification\*\*: Xác thực tin nhắn

\- \*\*Secure storage\*\*: Bảo vệ khóa private



## 🤝 Đóng góp



1\. Fork project

2\. Tạo feature branch (`git checkout -b feature/AmazingFeature`)

3\. Commit changes (`git commit -m 'Add AmazingFeature'`)

4\. Push to branch (`git push origin feature/AmazingFeature`)

5\. Tạo Pull Request



##  Acknowledgments



\- \*\*Netty Team\*\* - Network framework

\- \*\*BouncyCastle\*\* - Cryptography library

\- \*\*FlatLaf\*\* - Modern UI theme

\- \*\*SQLite\*\* - Embedded database



---



⭐ \*\*Star this repository if you find it useful!\*\*

```

