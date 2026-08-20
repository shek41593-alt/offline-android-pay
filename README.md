# Offline Android Pay

## Project Overview
This project provides an offline-first mobile payment solution for Android. Its purpose is to enable secure transactions in environments with limited or no internet connectivity. 

### Concept
The offline payment concept relies on securely capturing transactions locally, persisting them on the device, and synchronizing them with a remote backend once connectivity is restored.

### Architecture
The Android application follows a clean architecture pattern with separated UI, domain, and data layers. 
Major implemented components include an authentication module, an offline transaction persistence engine, and synchronization services.

## Authentication and Synchronization
- **Authentication**: JWT-based authentication allows users to securely identify themselves.
- **Retrofit**: Used as the primary HTTP client to communicate with backend APIs for login and real-time syncing.
- **Room**: Provides a local SQLite abstraction to securely store pending and historical transactions locally.
- **WorkManager**: Schedules reliable background jobs to perform transaction synchronization.
- **Transaction Synchronization**: A resilient engine built around WorkManager and Retrofit that ensures every offline transaction makes it securely to the server when network is restored.
