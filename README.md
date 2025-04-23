# 🛒 E-commerce Platform – Microservices Architecture
E-commerce system built on a microservices architecture. This project leverages modern technologies such as Kafka, Redis, Eureka, Stripe, Zookeeper, MongoDB, MySQL, and Docker for scalable and maintainable deployment.
## 📁 Project Structure
This platform consists of multiple services, each responsible for a specific domain:

| Service                  | Description                                                          |
|:-------------------------|:---------------------------------------------------------------------| 
| **API Gateway**          | Central entry point, routes requests to appropriate services.        |
| **Auth Service**         | Handles user authentication and authorization using Redis and MySQL. |
| **Customer Service**     | Manages customer data.                                               |
| **Product Service**      | Manages product catalog and categories.                              |
| **Order Service**        | Manages order placement and tracking                                 |
| **Payment Service**      | Integrates with Stripe for payments.                                 |
| **Cart Service**         | Manages user shopping carts using Redis.                             |
| **Notification Service** | Sends emails and listens to Kafka topics.                            |
| **File Storage Service** | Handles file uploads and serving static resources.                   |
| **Frontend**             | Angular-based user interface.                                        |
| **Eureka Server**        | Service discovery and registry.                                      |
| **Kafka & Zookeeper**    | Event streaming and service coordination.                            |
| **Stripe CLI**           | 	Listens for Stripe webhook events and forwards them.                |
| **Cart Service**         | Manages user shopping carts using Redis.                             |

## ⚙️ Requirements
- Docker + Docker Compose
- Node.js (for frontend development)
- Stripe account (test mode is fine)
- Email account with SMTP access (e.g., Gmail)

## 🧾 File Setup
Create a `.env` file in the root directory of your project and fill in the required variables:
```env
MAIL_USERNAME=your-email
MAIL_PASSWORD=your-mail-password

JWT_SECRET=your-jwt-secret

GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

STRIPE_SECRET=your-stripe-secret
WEBHOOK_KEY=your-webhook-key
```

Create a `env.ts` in `frontend-service/src/enviroments/` catalog and fill in the required variables:
```ts
export const environment = {
    production: false,
    googleClientId: 'google-client-key'
};
```

## 🚀 Running the Project
1. Clone the repository:
```bash
git clone https://github.com/KarolWojnar/SportStore-microservices.git
cd SportStore-microservices
```
2. Create your `.env` and `env.ts` file following the format shown above.
3. Start all services using Docker Compose:
   ```bash
   docker compose up --build
   ```
This will:
- Build and start all services.
- Automatically register them in the Eureka service registry.
- Initialize all databases (MySQL & MongoDB).
- Start Kafka, Redis, and Stripe CLI.
4. Access the system:

| Component                      | URL                                            |
|:-------------------------------|:-----------------------------------------------|
| **Frontend (Angular)**         | [http://localhost:4200](http://localhost:4200) |
| **API Gateway**                | [http://localhost:8080](http://localhost:8080) |
| **Eureka Dashboard**           | [http://localhost:8761](http://localhost:8761) |

## 📦 Technologies Used
 - Docker: Containerization and orchestration
 - Kafka & Zookeeper: Asynchronous communication between services
 - MySQL & MongoDB: Relational and document-oriented databases
 - Redis: Session and cart caching
 - Stripe: Payment gateway integration
 - Eureka: Service discovery and health checks
 - Spring Boot / Angular: Backend and frontend frameworks
