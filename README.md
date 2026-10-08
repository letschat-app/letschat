# LetsChat - Real-Time Communication & Collaboration Platform

[![Author](https://img.shields.io/badge/Author-Yokeshwar-orange?style=for-the-badge)](#intellectual-property--copyright)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Reactive WebFlux](https://img.shields.io/badge/Spring-WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://docs.spring.io/spring-framework/reference/web/webflux.html)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-R2DBC-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://r2dbc.io/)
[![Performance](https://img.shields.io/badge/Latency-318ms%20%40%2049%20msg%2Fsec-blueviolet?style=for-the-badge)](#performance--benchmarks)
[![License](https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge)](#intellectual-property--copyright)

**LetsChat** is a low-latency, reactive real-time chat, multimedia sharing, and collaborative learning platform built on **Spring Boot 3 (WebFlux)**, **PostgreSQL (R2DBC)**, and **WebSockets**.

Designed for high concurrency and ultra-low latency, LetsChat powers 1-on-1 private messaging, group rooms, ephemeral stories (*Pulses*), interactive polls, classroom management, system broadcasts, and experimental feature validation (*LetsChat Labs*).

---

## Performance & Benchmarks

Engineered with Spring WebFlux non-blocking reactive streams and R2DBC database connections, LetsChat delivers high real-time throughput under resource constraints:

| Metric | Measured Value | Environment & Setup |
| :--- | :--- | :--- |
| **Message Delivery Latency** | **318 ms** | Real-Time WebSocket Delivery Stream |
| **Average ACK Latency** | **328 ms** | Delivery & Read Confirmation ACKs |
| **Median ACK Latency (p50)** | **286 ms** | Half of all ACKs processed under 286ms |
| **P95 ACK Latency** | **537 ms** | 95th percentile under heavy burst load |
| **System Throughput** | **49 msgs / sec** | Concurrent real-time message stream |
| **Deployment Infrastructure** | **512 MB RAM** | Render Cloud Free Tier + Neon PostgreSQL Free Tier |

> Note: These benchmarks demonstrate resource efficiency—delivering high-frequency real-time messaging on a single 512MB RAM instance connected to a serverless database.

---

## Key Features

### 1. Authentication & Security
- **Secure Registration & Login**: Password hashing via BCrypt.
- **Two-Factor Authentication (2FA)**: Email OTP verification for secure user logins.
- **Account Recovery**: Password reset flow powered by OTP verification.
- **User Profiles**: Profile picture upload, status updates, and user search.

### 2. Real-Time Messaging & Virtual Rooms
- **Instant Messaging**: Low-latency bidirectional WebSocket message streaming.
- **Message Status & ACKs**: Real-time delivery (`delivered`) and read receipt (`read`) acknowledgments.
- **1-on-1 & Group Chats**: Dynamic group creation, admin role management, and member tracking.
- **Virtual Rooms & Classrooms**: Dedicated spaces for study groups, classrooms, and assignment tracking.
- **Scheduled Messages**: Schedule messages to be sent automatically at a future timestamp.

### 3. Pulses (Ephemeral 24-Hour Stories)
- **Story Creation**: Support for text, image, and video pulses with custom background colors and font styles.
- **24-Hour Expiration**: Pulses auto-expire after 24 hours.
- **Archive Retention**: Expired pulses remain visible to the creator in `/api/pulse/mine` for up to 12 hours post-expiration.
- **View History Tracking**: Real-time viewer counter with detailed viewer profiles (`viewerUserId`, `viewerName`, `viewerProfile`, `viewedAt`).

### 4. Interactive Polls
- **Multi-Option Voting**: Create customizable polls inside group chats and rooms.
- **Real-Time Tallying**: Live update broadcasts when users submit or change votes.

### 5. System Broadcasts & LetsChat Labs
- **Admin Broadcasts**: Global system announcements for text, image, and video content.
- **LetsChat Labs**: Participation-based feature validation framework allowing users to vote on upcoming experimental features (*"Connect Your Bot"*, *AI Study Tools*).

---

## Tech Stack & Architecture

- **Backend Framework**: Java 17, Spring Boot 3.5.3 (WebFlux / Reactive Stack)
- **Database**: PostgreSQL with Spring Data R2DBC (Non-blocking Reactive Access)
- **Real-Time Protocol**: WebSockets (`WebSocketHandler`, Reactive Sinks)
- **Push Notifications**: Firebase Cloud Messaging (FCM)
- **Media & File Storage**: Amazon S3 / MinIO Object Storage
- **API Specification**: SpringDoc OpenAPI 3.0

---

## Core API Architecture

| Controller | Section | Description |
| :--- | :--- | :--- |
| `UserController` | User & Auth | Sign up, Login, 2FA OTP, User Search, Profile Upload |
| `PulseController` | Pulses (Stories) | Create pulses, contacts feed, mine archive, view tracking |
| `PollController` | Polls | Create polls, vote options, poll analytics |
| `ClassRoomController` | Classrooms | Assignment creation, submission status, student stats |
| `RoomController` | Virtual Rooms | Create and join public/private virtual rooms |
| `GroupController` | Groups | Group creation, member additions, role assignments |
| `MediaController` | Media & Storage | File uploads (images, videos, documents) |
| `FCMController` | Notifications | Device FCM token registration & push alerts |
| `ScheduleMessageController` | Scheduler | Create & manage scheduled messages |

---

## Intellectual Property & Copyright

**Author**: Yokeshwar  
**Copyright © LetsChat - Yokeshwar. All Rights Reserved.**

This repository and its source code are **strictly proprietary and confidential**. 

- **No Permission to Copy**: Unauthorized copying, cloning, modification, distribution, or reproduction of any part of this repository via any medium is strictly prohibited.
- **No License Granted**: No open-source or commercial license is granted to any party. All rights, title, and interest in and to this software remain exclusively with Yokeshwar (LetsChat).
