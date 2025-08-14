# 📨 NotifyX Message Storage Guide

This guide explains how to use the new message storage functionality integrated with **Upstash Redis** in the NotifyX platform.

## 🚀 **Overview**

The message storage system allows you to:
- **Store messages** in Upstash Redis when notifications are sent
- **Retrieve messages** for specific users
- **Track read/unread status** of messages
- **Manage message lifecycle** (create, read, delete)
- **Get message statistics** for your project

## 🔧 **Configuration**

### **Environment Variables**

Set these environment variables in your deployment:

```bash
# Upstash Redis Configuration
UPSTASH_REDIS_URL=rediss://default:AT13AAIjcDFkNjM2OGI0NGQ5NTc0YTU3YjZhMGI0NWQwOTI0ZTk4Y3AxMA@legal-elephant-15735.upstash.io:6379
UPSTASH_REDIS_REST_URL=https://legal-elephant-15735.upstash.io
UPSTASH_REDIS_REST_TOKEN=AT13AAIjcDFkNjM2OGI0NGQ5NTc0YTU3YjZhMGI0NWQwOTI0ZTk4Y3AxMA

# Message Storage Configuration
NOTIFICATION_STORAGE_TTL_DAYS=30
NOTIFICATION_STORAGE_MAX_PER_USER=100
```

### **Application Properties**

The system automatically configures:
- **SSL/TLS connection** to Upstash Redis
- **Connection pooling** for optimal performance
- **TTL (Time To Live)** for automatic message cleanup
- **Rate limiting** and error handling

## 📡 **API Endpoints**

### **1. Store a Message**

```http
POST /api/messages/store
```

**Authentication**: `X-API-Key` header required

**Request Body**:
```json
{
  "recipient": "user@example.com",
  "message": "Welcome to our platform!",
  "title": "Welcome Message",
  "channel": "webhook",
  "metadata": {
    "source": "onboarding",
    "priority": "high"
  }
}
```

**Response**:
```json
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "projectId": "proj_abc123",
  "recipient": "user@example.com",
  "status": "STORED"
}
```

### **2. Get User Messages**

```http
GET /api/messages/user/{recipient}?limit=10
```

**Authentication**: None required (public endpoint)

**Parameters**:
- `recipient`: User identifier (email, user ID, etc.)
- `limit`: Number of messages to return (default: 10, max: 100)

**Response**:
```json
{
  "projectId": "proj_abc123",
  "recipient": "user@example.com",
  "messages": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "projectId": "proj_abc123",
      "recipient": "user@example.com",
      "message": "Welcome to our platform!",
      "title": "Welcome Message",
      "channel": "webhook",
      "metadata": {
        "source": "onboarding",
        "priority": "high"
      },
      "createdAt": "2024-01-15T10:30:00",
      "status": "STORED",
      "read": false
    }
  ],
  "totalMessages": 1,
  "unreadCount": 1,
  "limit": 10
}
```

### **3. Get Specific Message**

```http
GET /api/messages/{messageId}
```

**Authentication**: None required (public endpoint)

**Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "projectId": "proj_abc123",
  "recipient": "user@example.com",
  "message": "Welcome to our platform!",
  "title": "Welcome Message",
  "channel": "webhook",
  "metadata": {
    "source": "onboarding",
    "priority": "high"
  },
  "createdAt": "2024-01-15T10:30:00",
  "status": "STORED",
  "read": false
}
```

### **4. Mark Message as Read**

```http
PUT /api/messages/{messageId}/read
```

**Authentication**: None required (public endpoint)

**Response**:
```json
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "READ",
  "message": "Message marked as read successfully"
}
```

### **5. Get Unread Count**

```http
GET /api/messages/user/{recipient}/unread-count
```

**Authentication**: None required (public endpoint)

**Response**:
```json
{
  "projectId": "proj_abc123",
  "recipient": "user@example.com",
  "unreadCount": 5
}
```

### **6. Delete Message**

```http
DELETE /api/messages/{messageId}?recipient=user@example.com
```

**Authentication**: `X-API-Key` header required

**Parameters**:
- `recipient`: User identifier (required for proper cleanup)

**Response**:
```json
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "DELETED",
  "message": "Message deleted successfully"
}
```

### **7. Get Project Statistics**

```http
GET /api/messages/stats
```

**Authentication**: `X-API-Key` header required

**Response**:
```json
{
  "projectId": "proj_abc123",
  "totalMessages": 150,
  "ttlDays": 30,
  "maxPerUser": 100
}
```

### **8. Health Check**

```http
GET /api/messages/health
```

**Authentication**: None required

**Response**:
```json
{
  "status": "UP",
  "service": "Message Storage",
  "redis": "UP",
  "timestamp": 1705312200000
}
```

## 💡 **Usage Examples**

### **JavaScript/Node.js**

```javascript
// Store a message
const storeMessage = async (recipient, message, title) => {
  const response = await fetch('https://your-notifyx-domain.com/api/messages/store', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': 'sk_live_your_api_key'
    },
    body: JSON.stringify({
      recipient,
      message,
      title,
      channel: 'webhook',
      metadata: { source: 'app' }
    })
  });
  
  return response.json();
};

// Get user messages
const getUserMessages = async (recipient, limit = 10) => {
  const response = await fetch(
    `https://your-notifyx-domain.com/api/messages/user/${recipient}?limit=${limit}`
  );
  
  return response.json();
};

// Mark message as read
const markAsRead = async (messageId) => {
  const response = await fetch(
    `https://your-notifyx-domain.com/api/messages/${messageId}/read`,
    { method: 'PUT' }
  );
  
  return response.json();
};
```

### **Python**

```python
import requests

class NotifyXMessageClient:
    def __init__(self, api_key, base_url="https://your-notifyx-domain.com"):
        self.api_key = api_key
        self.base_url = base_url
    
    def store_message(self, recipient, message, title, channel="webhook"):
        url = f"{self.base_url}/api/messages/store"
        headers = {"X-API-Key": self.api_key, "Content-Type": "application/json"}
        data = {
            "recipient": recipient,
            "message": message,
            "title": title,
            "channel": channel,
            "metadata": {"source": "python-app"}
        }
        
        response = requests.post(url, headers=headers, json=data)
        return response.json()
    
    def get_user_messages(self, recipient, limit=10):
        url = f"{self.base_url}/api/messages/user/{recipient}?limit={limit}"
        response = requests.get(url)
        return response.json()
    
    def mark_as_read(self, message_id):
        url = f"{self.base_url}/api/messages/{message_id}/read"
        response = requests.put(url)
        return response.json()

# Usage
client = NotifyXMessageClient("sk_live_your_api_key")
client.store_message("user@example.com", "Hello!", "Welcome")
messages = client.get_user_messages("user@example.com")
```

### **cURL Examples**

```bash
# Store a message
curl -X POST https://your-notifyx-domain.com/api/messages/store \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_your_api_key" \
  -d '{
    "recipient": "user@example.com",
    "message": "Welcome to our platform!",
    "title": "Welcome",
    "channel": "webhook"
  }'

# Get user messages
curl -X GET "https://your-notifyx-domain.com/api/messages/user/user@example.com?limit=5"

# Mark message as read
curl -X PUT https://your-notifyx-domain.com/api/messages/550e8400-e29b-41d4-a716-446655440000/read

# Get unread count
curl -X GET https://your-notifyx-domain.com/api/messages/user/user@example.com/unread-count
```

## 🔄 **Integration with Notifications**

When you send a notification using the existing `/api/notification/send` endpoint, the system automatically:

1. **Sends the notification** through the configured channel (webhook, email, etc.)
2. **Stores the message** in Upstash Redis for user retrieval
3. **Tracks delivery status** for monitoring purposes

This means you get both **real-time delivery** and **persistent message storage** with a single API call.

## 📊 **Data Structure**

### **Message Storage Keys**

The system uses these Redis key patterns:

- `message:{messageId}` - Individual message data
- `user:messages:{projectId}:{recipient}` - User's message list
- `project:messages:{projectId}` - Project's message list

### **Message Object Structure**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "projectId": "proj_abc123",
  "recipient": "user@example.com",
  "message": "Welcome to our platform!",
  "title": "Welcome Message",
  "channel": "webhook",
  "metadata": {
    "source": "onboarding",
    "priority": "high"
  },
  "createdAt": "2024-01-15T10:30:00",
  "status": "STORED",
  "read": false,
  "readAt": "2024-01-15T11:00:00"  // Only present when read
}
```

## ⚙️ **Configuration Options**

### **TTL (Time To Live)**

Messages are automatically deleted after the configured TTL:

```properties
notification.storage.ttl-days=30
```

### **Maximum Messages Per User**

Limit the number of messages stored per user:

```properties
notification.storage.max-per-user=100
```

### **Redis Connection Pool**

Optimize Redis connection performance:

```properties
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

## 🔒 **Security**

- **Message storage endpoints** are public (no authentication required) for easy user access
- **Message creation endpoints** require API key authentication
- **Message deletion** requires API key authentication
- **All data** is encrypted in transit and at rest in Upstash Redis

## 📈 **Performance**

- **Fast retrieval**: O(1) for individual messages, O(n) for user message lists
- **Automatic cleanup**: Messages expire after TTL to prevent storage bloat
- **Connection pooling**: Optimized Redis connections for high throughput
- **Caching**: Redis provides in-memory performance for message storage

## 🚨 **Error Handling**

The system gracefully handles:

- **Redis connection failures** with fallback responses
- **Invalid message IDs** with 404 responses
- **Missing required fields** with 400 responses
- **Rate limiting** with appropriate HTTP status codes

## 🔍 **Monitoring**

Monitor your message storage with:

```bash
# Check health
curl https://your-notifyx-domain.com/api/messages/health

# Get project stats
curl -H "X-API-Key: sk_live_your_api_key" \
     https://your-notifyx-domain.com/api/messages/stats
```

## 🎯 **Best Practices**

1. **Use meaningful titles** for better user experience
2. **Include metadata** for filtering and analytics
3. **Implement read status tracking** in your UI
4. **Monitor unread counts** for notification badges
5. **Clean up old messages** using the delete endpoint
6. **Handle pagination** for users with many messages

---

**🎉 You're now ready to use NotifyX with persistent message storage in Upstash Redis!**
