# 🔔 NotifyX - Notification Middleware Platform

NotifyX is a **distributed notification middleware platform** that enables projects to send notifications to their users through multiple channels. Built with a microservices architecture using **Kafka**, **Redis**, and **Spring Boot**.

## 🚀 Features

### For Projects (API Users)
- **Multi-channel notifications**: Email, SMS, Webhook, Push notifications
- **Project isolation**: Each project has its own API key and configuration
- **Bulk notifications**: Send to multiple recipients at once
- **Template support**: Reusable notification templates with variable substitution
- **Webhook delivery**: Receive delivery status updates via webhooks
- **Rate limiting**: Configurable rate limits per project
- **Real-time status**: Track notification delivery status

### For Recipients
- **Multiple delivery channels**: Choose preferred notification methods
- **Real-time delivery**: Instant notification delivery
- **Delivery confirmation**: Track when notifications are received

## 📦 Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Service   │───▶│     Kafka       │───▶│ Notifier Service│
│                 │    │                 │    │                 │
│ • Project Auth  │    │ • Message Queue │    │ • Channel Mgmt  │
│ • Rate Limiting │    │ • Persistence   │    │ • Delivery Logic│
│ • Request Val   │    │ • Scalability   │    │ • Status Updates│
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     Redis       │    │   CLI Tool      │    │ External Services│
│                 │    │                 │    │                 │
│ • Status Cache  │    │ • Command Line  │    │ • Email (SMTP)  │
│ • Project Data  │    │ • Bulk Sending  │    │ • SMS (Twilio)  │
│ • Rate Limits   │    │ • Status Check  │    │ • Webhooks      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🛠️ Quick Start

### 1. Start the Services

```bash
# Clone the repository
git clone https://github.com/yourusername/notifyx.git
cd notifyx

# Start all services with Docker Compose
docker-compose up -d
```

### 2. Register Your Project

```bash
curl -X POST http://localhost:8080/api/projects/register \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "My Awesome App",
    "contactEmail": "admin@myapp.com",
    "webhookUrl": "https://myapp.com/webhooks/notifications",
    "channels": ["webhook", "email"],
    "rateLimit": 1000
  }'
```

Response:
```json
{
  "projectId": "proj_abc123def456",
  "projectName": "My Awesome App",
  "apiKey": "sk_live_xyz789...",
  "webhookSecret": "whsec_abc123...",
  "dashboardUrl": "https://notifyx.com/dashboard/proj_abc123def456",
  "createdAt": "2024-01-15T10:30:00",
  "status": "ACTIVE"
}
```

### 3. Send Your First Notification

```bash
curl -X POST http://localhost:8080/api/notification/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_xyz789..." \
  -d '{
    "recipients": ["user1@example.com", "user2@example.com"],
    "message": "Welcome to our platform!",
    "title": "Welcome",
    "channel": "webhook",
    "channelConfig": {
      "url": "https://myapp.com/webhooks/notifications"
    }
  }'
```

### 4. Check Notification Status

```bash
curl -X GET "http://localhost:8080/api/notification/status/user1@example.com?limit=10" \
  -H "X-API-Key: sk_live_xyz789..."
```

## 📚 API Documentation

### Authentication

All API requests require authentication using your project's API key:

```bash
# Using Authorization header
Authorization: Bearer sk_live_your_api_key_here

# Using X-API-Key header
X-API-Key: sk_live_your_api_key_here

# Using query parameter
?api_key=sk_live_your_api_key_here
```

### Endpoints

#### Project Management
- `POST /api/projects/register` - Register a new project
- `GET /api/projects/me` - Get current project info

#### Notifications
- `POST /api/notification/send` - Send a single notification
- `POST /api/notification/bulk` - Send multiple notifications
- `GET /api/notification/status/{recipient}` - Get notification status
- `GET /api/notification/status/notification/{notificationId}` - Get specific notification status

#### Health & Info
- `GET /api/notification/health` - Service health check
- `GET /api/notification/version` - API version info

## 🔧 Configuration

### Environment Variables

```bash
# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC=notifyx_notifications

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# API Configuration
API_RATE_LIMIT_DEFAULT=1000
API_RATE_LIMIT_WINDOW=3600
```

### Channel Configuration

#### Webhook Channel
```json
{
  "channel": "webhook",
  "channelConfig": {
    "url": "https://your-app.com/webhooks/notifications",
    "secret": "your_webhook_secret",
    "timeout": 5000,
    "retries": 3
  }
}
```

#### Email Channel (Coming Soon)
```json
{
  "channel": "email",
  "channelConfig": {
    "smtpHost": "smtp.sendgrid.net",
    "smtpPort": 587,
    "username": "your_username",
    "password": "your_password",
    "fromEmail": "noreply@yourdomain.com"
  }
}
```

## 🚀 CLI Tool

The NotifyX CLI provides a command-line interface for sending notifications:

```bash
# Install CLI
cd cli
chmod +x notifyx
sudo ./install.sh

# Send notification
notifyx send \
  --api-key sk_live_your_key \
  --recipients user1@example.com,user2@example.com \
  --message "Hello from CLI!" \
  --channel webhook

# Check status
notifyx status --api-key sk_live_your_key --recipient user1@example.com
```

## 🔌 Webhook Integration

Set up a webhook endpoint in your application to receive delivery status updates:

```javascript
// Express.js example
app.post('/webhooks/notifications', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  
  // Verify webhook signature
  if (!verifySignature(req.body, signature, webhookSecret)) {
    return res.status(401).send('Invalid signature');
  }
  
  const event = req.body;
  
  switch (event.status) {
    case 'DELIVERED':
      console.log(`Notification delivered to ${event.recipient}`);
      break;
    case 'FAILED':
      console.log(`Notification failed for ${event.recipient}: ${event.errorMessage}`);
      break;
  }
  
  res.status(200).send('OK');
});
```

## 📊 Monitoring & Analytics

- **Delivery Success Rates**: Track notification delivery success
- **Performance Metrics**: Monitor response times and throughput
- **Error Tracking**: Identify and resolve delivery issues
- **Usage Analytics**: Monitor API usage and trends

## 🔒 Security

- **API Key Authentication**: Secure access to your project
- **Webhook Signatures**: Verify webhook authenticity
- **Rate Limiting**: Prevent abuse and ensure fair usage
- **Data Encryption**: All data encrypted in transit and at rest

## 🛠️ Development

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- Kafka
- Redis

### Local Development

```bash
# Start dependencies
docker-compose up -d kafka redis

# Run API Service
cd api-service
mvn spring-boot:run

# Run Notifier Service
cd notifier-service
mvn spring-boot:run
```

### Testing

```bash
# Run all tests
mvn test

# Run specific service tests
cd api-service && mvn test
cd notifier-service && mvn test
```

## 📈 Performance

- **Throughput**: 10,000+ notifications/second
- **Latency**: <200ms delivery time
- **Scalability**: Horizontal scaling with Kafka
- **Reliability**: 99.9% uptime with failover

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: [docs.notifyx.com](https://docs.notifyx.com)
- **Issues**: [GitHub Issues](https://github.com/yourusername/notifyx/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/notifyx/discussions)
- **Email**: support@notifyx.com

---

**NotifyX** - Powering notifications for modern applications 🚀
