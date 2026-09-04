git add app/build.gradle.kts app/src/main/java/com/lastmilebanking/app/di/AppwriteModule.kt app/src/main/java/com/lastmilebanking/app/di/DatabaseModule.kt app/src/main/java/com/lastmilebanking/app/di/NetworkModule.kt app/src/main/res/xml/network_security_config.xml
git commit -m "build(android): configure network and dependency injection modules"

git add app/src/main/java/com/lastmilebanking/app/data/local/LastMileDatabase.kt app/src/main/java/com/lastmilebanking/app/data/local/dao/UserDao.kt app/src/main/java/com/lastmilebanking/app/data/local/dao/WalletDao.kt app/src/main/java/com/lastmilebanking/app/data/local/entity/UserEntity.kt app/src/main/java/com/lastmilebanking/app/data/local/entity/WalletEntity.kt
git commit -m "feat(database): implement local Room database entities and DAOs"

git add app/src/main/java/com/lastmilebanking/app/data/network/api/LastMileApiService.kt app/src/main/java/com/lastmilebanking/app/data/network/dto/NetworkDtos.kt app/src/main/java/com/lastmilebanking/app/data/repository/AuthenticationRepository.kt app/src/main/java/com/lastmilebanking/app/data/repository/UserRepository.kt app/src/main/java/com/lastmilebanking/app/data/repository/WalletRepository.kt
git commit -m "feat(data): implement API service and data repositories"

git add app/src/main/java/com/lastmilebanking/app/domain/engines/impl/AuthenticationEngineImpl.kt app/src/main/java/com/lastmilebanking/app/features/authentication/AuthViewModel.kt app/src/main/java/com/lastmilebanking/app/features/authentication/LoginFragment.kt app/src/main/java/com/lastmilebanking/app/features/authentication/OTPFragment.kt app/src/main/java/com/lastmilebanking/app/features/onboarding/CreatePasswordFragment.kt app/src/main/res/layout/fragment_otp.xml app/src/main/res/layout/fragment_address.xml app/src/main/java/com/lastmilebanking/app/features/profile/ProfileFragment.kt app/src/main/java/com/lastmilebanking/app/features/profile/ProfileViewModel.kt app/src/main/res/layout/fragment_profile.xml
git commit -m "feat(auth): implement login, OTP, and profile flows"

git add app/src/main/java/com/lastmilebanking/app/features/history/HistoryFragment.kt app/src/main/java/com/lastmilebanking/app/features/home/HomeFragment.kt app/src/main/java/com/lastmilebanking/app/features/home/HomeViewModel.kt app/src/main/java/com/lastmilebanking/app/features/home/TransactionAdapter.kt app/src/main/java/com/lastmilebanking/app/features/wallet/WalletFragment.kt
git commit -m "feat(wallet): implement wallet, home, and transaction history screens"

git add app/src/main/java/com/lastmilebanking/app/features/payments/BluetoothPayViewModel.kt app/src/main/java/com/lastmilebanking/app/features/payments/QrPayFragment.kt app/src/main/java/com/lastmilebanking/app/features/payments/QrPayViewModel.kt app/src/main/java/com/lastmilebanking/app/features/payments/SmsPayViewModel.kt app/src/main/res/layout/fragment_qr_pay.xml
git commit -m "feat(payment): integrate QR, Bluetooth, and SMS payment flows"

git add app/src/main/java/com/lastmilebanking/app/features/onboarding/SyncProgressFragment.kt app/src/main/res/navigation/nav_graph.xml
git commit -m "feat(sync): implement sync progress UI and navigation graph updates"

git add app/src/androidTest/java/com/lastmilebanking/app/e2e/OfflineSyncE2ETest.kt app/src/test/java/com/lastmilebanking/app/domain/engines/SynchronizationEngineTest.kt app/src/test/java/com/lastmilebanking/app/network/NetworkLayerTest.kt
git commit -m "test(android): add end-to-end sync, network, and synchronization engine tests"

git push origin main
