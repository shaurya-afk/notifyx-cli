# 🔔 NotifyX CLI

NotifyX CLI is a lightweight command-line tool to send real-time notifications to users or teams, powered by a distributed microservice architecture using **Kafka**, **Redis**, and **Spring Boot**.

This CLI is built to interface with the NotifyX API and push notifications quickly — either individually, in bulk, or through automation scripts.

---

## 🚀 Features

- Send real-time notifications via CLI  
- Bulk message dispatch from CSV  
- Check notification status per user  
- Lightweight, Bash-based and cross-platform  
- Designed for developers, DevOps, and system admins

---

## 📦 Project Overview

NotifyX is a distributed real-time notification system built using:

- **Spring Boot** (API and Kafka integration)
- **Kafka** (asynchronous messaging backbone)
- **Redis** (real-time persistence and fast reads)
- **Docker + Docker Compose** (for isolated environments)
- **WebSockets** (upcoming, for live browser-based delivery)

> It supports 10K+ messages/sec throughput with <200ms delivery latency.

---

## 🛠️ CLI Installation

```bash
# Clone the repo
git clone https://github.com/yourusername/notifyx-cli.git
cd notifyx-cli/cli

# Make it executable and install globally
chmod +x notifyx
sudo ./install.sh
```
