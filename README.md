# Offline Android Pay

## Project Overview
This project provides an offline-first mobile payment solution for Android. Its purpose is to enable secure transactions in environments with limited or no internet connectivity. 

### Concept
The offline payment concept relies on securely capturing transactions locally, persisting them on the device, and synchronizing them with a remote backend once connectivity is restored.

### Architecture
The Android application follows a clean architecture pattern with separated UI, domain, and data layers. 
Major implemented components include an authentication module, an offline transaction persistence engine, and synchronization services.
