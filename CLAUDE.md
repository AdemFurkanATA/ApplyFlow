# Project Context for Claude

Project Name: Job Application Tracker API  
Stack: Java 17, Spring Boot, Spring Security, JWT, MySQL

---

## 🎯 Project Goal

This is a backend REST API designed to help users track job applications.  
It supports authentication, CRUD operations, filtering, pagination, and secure access.

---

## 🏗️ Architecture

Layered architecture:

- Controller Layer: Handles HTTP requests
- Service Layer: Business logic
- Repository Layer: Database interaction
- Security Layer: JWT-based authentication

The project follows clean code principles and separation of concerns.

---

## 🔐 Security

- JWT authentication
- BCrypt password hashing
- Stateless session management
- Role-based authorization (USER / ADMIN)

---

## 📊 Core Entity

JobApplication:
- companyName
- position
- status (enum)
- applicationDate
- salaryExpectation
- notes
- contactPerson
- belongs to User

Each user can only access their own applications.

---

## 🧠 Design Decisions

- DTO pattern used to prevent exposing entities directly
- Global exception handler implemented
- Validation annotations used on request bodies
- Pagination implemented using Spring Pageable
- Enum used for application status to enforce consistency

---

## 🔍 Expected Enhancements

- Add status history tracking
- Add reminder scheduler
- Add email notifications
- Add Docker configuration
- Add integration tests

---

## 🧪 Code Expectations

- Clean code
- Meaningful commit messages
- Clear separation of layers
- Proper error handling
- No business logic inside controllers
- No direct entity exposure in responses

---

This project is meant to simulate a production-ready backend system rather than a simple tutorial CRUD app.
