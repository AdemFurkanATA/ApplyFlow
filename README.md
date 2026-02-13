# ApplyFlow

A production-ready RESTful API built with Spring Boot that allows users to track and manage their job applications efficiently.

This project demonstrates authentication, authorization, layered architecture, validation, exception handling, and best backend practices.

---

## 🚀 Features

- User registration & login (JWT authentication)
- Role-based authorization
- CRUD operations for job applications
- Application status tracking
- Pagination & sorting
- Filtering by status/date/company
- Global exception handling
- DTO mapping
- Validation
- Logging
- Unit testing support
- Swagger API documentation

---

## 🧠 Problem Statement

Job seekers often apply to multiple companies across different platforms.  
Tracking application status manually (Excel, Notes, etc.) becomes confusing.

This API solves:
- Where did I apply?
- What stage am I in?
- Did I receive feedback?
- What salary expectation did I mention?

---

## 🏗️ Architecture

Layered architecture:

Controller → Service → Repository → Database

Additional components:
- Security Layer (JWT Filter)
- Global Exception Handler
- DTO Mapper
- Validation Layer

---

## 🗄️ Database Design

### Entities

**User**
- id
- name
- email
- password
- role
- createdAt

**JobApplication**
- id
- companyName
- position
- status (APPLIED, INTERVIEW, TECHNICAL, OFFER, REJECTED)
- applicationDate
- salaryExpectation
- contactPerson
- notes
- user_id (FK)
- createdAt
- updatedAt

Indexes:
- email (unique)
- status
- applicationDate

---

## 🔐 Authentication

- JWT-based authentication
- Token expiration support
- Secure password hashing (BCrypt)

---

## 📡 API Endpoints

### Auth

POST /api/auth/register  
POST /api/auth/login

### Job Applications

GET /api/applications  
GET /api/applications/{id}  
POST /api/applications  
PUT /api/applications/{id}  
DELETE /api/applications/{id}

### Filtering & Pagination

GET /api/applications?status=INTERVIEW&page=0&size=10

---

## 🧪 Testing

- Unit tests for service layer
- Validation tests
- Exception tests

---

## 🛠️ Tech Stack

- Java 17
- Spring Boot
- Spring Security
- JWT
- MySQL
- JPA / Hibernate
- Maven
- Swagger
- Docker (optional)

---

## ▶️ How to Run

1. Clone repository
2. Configure application.properties
3. Run:
   mvn clean install
   mvn spring-boot:run

---

## 🧭 Future Improvements

- Email notifications
- Reminder system
- Status history tracking
- Redis caching
- CI/CD pipeline
- Deployment to cloud

---

## 📌 Why This Project Matters

This project demonstrates:
- Clean code principles
- RESTful API design
- Secure authentication
- Production-ready backend practices

It is not a tutorial project, but a structured backend system simulating real-world use cases.

