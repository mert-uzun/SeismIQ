# SeismIQ

## 🧭 Overview

Natural disasters like earthquakes can strike without warning, leaving cities and communities in urgent need of coordination, communication, and resource distribution. In such chaotic environments, time is critical. Rescue operations, humanitarian aid, and survivor outreach efforts must be fast, efficient, and based on accurate, real-time information. Unfortunately, traditional communication infrastructures often collapse or become overloaded, creating serious gaps in coordination.

This project was created as a response to that challenge — a mobile-first, cloud-powered platform designed to facilitate disaster relief through crowd participation and intelligent data processing.

SeismIQ provides a centralized system where individuals affected by the disaster, as well as volunteers and responders, can report:

-What kind of help they need (e.g., medical aid, food, water, shelter)

-What resources they can offer (e.g., crane availability, first aid kits, mobile kitchens)

-Where they are located, using GPS and map integration

These reports are immediately visualized on an interactive map to help relief organizations and other users gain situational awareness and act accordingly.

Beyond manual reporting, the system integrates optional machine learning capabilities to process Twitter data and detect urgent calls for help. Tweets containing keywords like “under rubble”, “need food”, or “stuck in debris” are automatically analyzed using Natural Language Processing (NLP) models and then geolocated and classified. This enables the system to pick up signals even when users can’t access the app directly — increasing visibility of unseen crises.

The backend is fully built on Amazon Web Services (AWS) using a serverless architecture. AWS Lambda, DynamoDB, API Gateway, and SageMaker power the backend logic, data storage, and AI model inference. This approach ensures high scalability and low latency, even under sudden traffic surges during a disaster scenario.

The mobile app, developed using Android Studio in Java, is designed to be intuitive and lightweight, requiring minimal input while offering life-saving functionality. The UI is focused on ease of use in high-stress environments, with map-based reporting, quick taps for emergency types, and push notification support for nearby alerts.

In summary, this project aims to:

-Empower citizens to self-report and assist others during a disaster

-Support first responders with real-time spatial data on needs and resources

-Leverage AI and social media to amplify voices that may not otherwise be heard

-Provide a scalable, modular, and cloud-native architecture for humanitarian applications


---

## **🚀 Features**

- 📍 Location-based help/resource reporting
- 🧑‍🤝‍🧑 Real-time crowd mapping using heatmaps
- 🧠 Tweet analysis for detecting emergency requests (via NLP)
- 🔔 Push notifications for critical alerts
- 🗺️ Resource visualization: cranes, food spots, shelters, etc.
- 🧩 Modular backend using AWS Lambda & API Gateway
- 📲 Android mobile app with interactive maps and reporting

---

## **🧱 Technologies Used**

### Backend
- **AWS Lambda** – Serverless event-driven backend
- **Amazon API Gateway** – RESTful endpoints
- **Amazon DynamoDB / RDS** – Storing reports and tweet data
- **Amazon S3** – Storing static assets or logs
- **Amazon Comprehend / SageMaker** – NLP model deployment
- **AWS SNS / SQS** – Notifications and async queueing

### Machine Learning
- **Text Classification Models** – Classify tweets and reports into categories
- **Anomaly Detection** – Identify unusual spikes in certain areas
- **Clustering & Geo-spatial Analysis** – Determine crowded zones

### Frontend
- **Android Studio (Java)** – Native app with report submission, map, etc.
- **Google Maps SDK** – Map rendering & real-time markers
- **Firebase (optional)** – Authentication & push notification services

---

## **📂 Project Structure**

```bash
seismiq/
├── backend/                # AWS Lambda functions & APIs
│   ├── lambda_functions/
│   └── tweet_processor/
├── ml/                     # Machine learning models & data pipelines
│   ├── models/
│   └── training/
├── mobile-app/             # Android app source
│   ├── app/src/
│   └── activities/
├── docs/                   # Screenshots, diagrams, architecture
│   └── system_architecture.md
└── README.md               # Project documentation (this file)
```

--
## ⚙️ Getting Started

### Used Technologies

- Java + Android Studio (for mobile app)  
- Python 3.9+ (for ML pipelines)  
- AWS CLI + credentials  
- Node.js (if using Serverless Framework)  
- Firebase account (optional)

---

## 📱 Usage

### Submitting a Report

1. Open the Android app  
2. Tap on **“+ Report Help / Resource”**  
3. Choose your category:
   - **Need**: water, shelter, medical help  
   - **Available**: food, crane, ambulance, etc.  
4. Your location is auto-detected (or set manually)  
5. Submit — the report appears on the shared map in real time

### Visualizing Crowds & Tweets

- Crowded areas are shown as red/orange heat zones  
- Tweets matching emergency keywords like *"under rubble"*, *"need help"*, or *"no food"* are classified, geotagged, and visualized

---

## 🌐 API Endpoints

| Method | Endpoint         | Description                         |
|--------|------------------|-------------------------------------|
| GET    | `/api/reports`   | List all user-submitted reports     |
| POST   | `/api/reports`   | Submit a new report                 |
| GET    | `/api/heatmap`   | Get density and crowd data          |
| GET    | `/api/tweets`    | Get analyzed emergency tweets       |

> 🔐 JWT or Firebase token required for protected endpoints

---

## 🧭 Roadmap

-  Offline report submission with later syncing  
-  Admin dashboard with stats and heatmaps  
-  Volunteer matching system with proximity-based alerts  Multilingual NLP support (Turkish, Arabic, Kurdish)  
-  Integration with AFAD, Red Crescent, NGOs

---

## 📜 License

This project is licensed under the **MIT License**.

---

## ❤️ Acknowledgments

Special thanks to the **AWS Student Engagement Program** and to all team members (Sıla Bozkurt, Mert Uzun, Berksu Tekkaya, Ayşe Ece Bilgi, who contributed to this project. SeismIQ is built with the belief that **rapid, decentralized, and citizen-powered disaster response** can save lives.

---
