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

## Offline Features and Security
- **Offline Mode**: Transactions can be recorded entirely locally using available offline payment channels.
- **Retry & Recovery**: Built-in mechanisms to retry failed operations and recover gracefully from edge cases.
- **Idempotency**: All network requests to the backend handle idempotency to avoid double charges.
- **Wallet & Ledger**: A local representation of the user's wallet and ledger data that stays synchronized.
- **Settlement**: Automated reconcilation process.
- **Security**: Hardened network configurations, encrypted local storage, and granular runtime permissions properly handle sensitive financial logic in compliance with security guidelines.

## Payment Channel Status
- **SMS Payment**: Fully implemented and tested. Allows offline transaction delivery via secure, compressed SMS payloads to the backend's receiver numbers.
- **QR Payment**: Proof of concept / stubbed. UI fragments exist for displaying and scanning QR codes, but the full end-to-end QR channel is not yet integrated.
- **Bluetooth Payment**: Proof of concept / stubbed. Core navigation and fragments are present, but local Bluetooth communication protocols for finalizing a transaction are pending implementation.
