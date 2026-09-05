# Offline QR Protocol

## 1. Purpose
The Offline QR Protocol enables merchants and customers to conduct cryptographically secure payment requests without immediate internet access. It provides indisputable proof of authorized intent to debit a customer's wallet in favor of a merchant, bounded securely within a robust cryptographic architecture, ensuring authenticity and integrity while offline.

## 2. Threat Model
- **Tampering/Manipulation:** Attackers modifying payload fields (like `amount` or `payee/merchantWalletId`) natively breaks the overall EC cryptographic `.sign()` bytes yielding verification failure.
- **Forgery:** Private keys are vaulted locally in the hardware-backed Android KeyStore reducing extraction probability and preventing rogue payload generation. 
- **Replay Attacks:** Addressed through `clientOperationId` (globally unique UUIDs per transaction), `nonce`, and short-lived expiry constraints (1 hour max life).

## 3. QR Schema
Conceptual Equivalent JSON Schema forming the domain `OfflineQrPaymentRequest`:
```json
{
  "version": 1,
  "type": "LMB_PAYMENT_REQUEST",
  "merchantId": "MERCHANT_XYZ",
  "merchantWalletId": "WALLET_XYZ",
  "amount": "100.00",
  "currency": "INR",
  "clientOperationId": "uuid-...",
  "timestamp": 1693892837332,
  "nonce": "NONCE_XYZ",
  "paymentMode": "OFFLINE_QR",
  "signature": "base64_encoded_ecdsa_signature"
}
```
Field Classification:
- `version`, `type`, `merchantId`, `merchantWalletId`, `amount`, `currency`, `clientOperationId`, `timestamp`, `nonce`, `paymentMode`: Required, Security-sensitive, User-visible.
- `signature`: Server-authoritative, Cryptographic proof (Not user-visible directly). 

## 4. Canonicalization
Canonical Payload strictly normalizes elements for signing to avoid encoding drift vulnerabilities.
- Format: `version|type|merchantId|merchantWalletId|amount|currency|clientOperationId|timestamp|nonce|paymentMode`
- Serialization explicitly uses stable delimiters (`|`) and strict UTF-8 encoded Byte Arrays. No arbitrary maps or unordered JSON attributes persist inside the cryptographic core pipeline.

## 5. Cryptographic Model
Merchant Device (Signing):
- Generates un-extractable EC KeyPair explicitly via Android KeyStore `KeyProperties.PURPOSE_SIGN`.
- Binds Canonical String -> SHA256withECDSA -> signature strings (Base64).

Customer Device (Verification):
- Identifies Merchant Public Key. 
- Performs structural validation. 
- Applies SHA256withECDSA `initVerify()` matching Canonical generation bytes.

## 6. Key Lifecycle
- **Key Alias:** `LMB_QR_SIGNING_KEY`
- **Key Storage:** Android hardware-backed KeyStore. Un-exportable.
- **Missing/Invalid:** Generates automatically on init if missing. Must rotate/bind public key dynamically natively against the backend on first login synchronization.
- **Public Key Registry:** Expected to be hosted remotely mapping `merchantId` -> `PublicKey`.

## 7. Replay Protection
- Short expiry limits (Demo enforces 1 hour duration: `System.currentTimeMillis() - request.timestamp > 3600_000L`). 
- Backend layer idempotency strictly prevents re-running successful `clientOperationId` requests rejecting duplicates natively globally across networks avoiding re-charging behaviors.

## 8. Validation
Boundary structurally validates constraints *prior* to hitting the internal processing `TransactionEngine`. Prevents structural spoofing traversing downstream networks. 
Customer-side guards enforce `Amount > 0`, Expiry, Current Currencies (INR), matching Identities (rejects scanning your own merchant code).

## 9. Transaction Lifecycle
1. QR_CREATED (Offline)
2. CUSTOMER_SCANNED (Offline) -> Boundary Validation
3. PENDING_SYNC (SQLite Offline DB Status) 
4. SYNCING (WorkManager network transition state)
5. SETTLED (PostgreSQL Backend Confirmation Final State)

## 10. Offline Limitations
The system must NOT claim monetary transitions happen autonomously while offline. Output definitions map to `"Waiting for synchronization"` and `"Payment recorded offline"`. Settlement equates exclusively to Backend confirmation limits.

## 11. Double-Spending Limitation
This framework proves intentional authenticity. It does NOT eradicate local Double-Spending natively strictly offline. A user could issue 5 Offline proofs having bounded balances. Phase 3.1 defers logical bounds constraints specifically. Eventual limitations require strict Offline-Allowance quotas distributed defensively by the main PostgreSQL backend upon re-synchronizations.

## 12. Security Decisions
- **MITIGATED:** QR tampering, Signature forgery, Malformed structure, Unauthorized format attacks.
- **PARTIALLY MITIGATED:** Replay attacks (Mitigated natively within expiration bounds, globally upon hitting internet/backend). 
- **FUTURE WORK:** Offline Double Spending, Stale/Theft offline public key revocation endpoints. 

## 13. Future Improvements
Introduce backend-issued temporary allowance bounds dictating maximum total spending limits allowed completely off-grid before requiring a mandatory hard-token reauthentication synchronization sequence preventing massive net balance overdrafts.

## Phase 3.2 — Merchant QR Generation
### Merchant Flow
Merchants engage an isolated offline workflow navigating from `Merchant Home` -> `Receive Payment`. The merchant declares the payment intent's structural bound (e.g. amount) prior to exposing the QR representation manually.

### QR Generation
`OfflineQrPaymentRequest` payload bytes generate cleanly via `Zxing` embedded implementations creating offline bitmap outputs rendering securely within strictly controlled view parameters without storing state.  

### Expiration
System enforces deterministic 1-Hour window validations globally mapped sequentially.

### Cancellation
Cancellations merely drop physical QR displays clearing session bounds. Financial bounds remain untampered internally.

### Offline/No-Network Behavior
All bindings generated sequentially execute purely offline inside Android environments via `SecureRandom` mapped internally fetching user identity parameters through local SQLite dependencies (`UserRepository`, `WalletRepository`) independent of Retrofit networking dependencies securely enabling strict Airplane Mode environments.

### Wallet Mutation Rules
Generating QR signatures actively bypasses existing UI balances securely skipping any mutation. Only actual remote-backend transactions synced synchronously execute settlement bindings updating locally mapped `WalletEntity`.

### Security Boundaries
Signatures execute bound via Android hardware Keystore layers isolating ECDSA keys securely from serialization breaches.

## Phase 3.3 — Customer QR Scanning

### Scan Flow
Customer selects "Scan QR" and aligns the merchant's generated QR code within the view frame. The camera decodes the QR string seamlessly completely offline without hitting any backend resolution mechanisms.

### Validation Order
1. **Decode**: Capture Raw QR bits mapping via ZXing `BarcodeCallback`.
2. **Parse**: Unmarshall structural `OfflineQrPaymentRequest` payload bytes verifying correct JSON schemas and required protocol fields.
3. **Validate**: Bound amount (> 0), operational timing dependencies (expiration offsets <= 1HR), identity integrity (Sender ≠ Receiver).
4. **Signature Verification**: Validates `ECDSA` signature against reconstructed Canonical representations confirming absolute merchant authenticity.
5. **Confirmation Render**: Explicit UI displays merchant details exactly as resolved without arbitrary manipulation forcing confirmation.

### Transaction Creation Boundary
Only upon rigorous explicit `Confirm` interaction does the local `TransactionEngine` generate durability footprints mapped explicitly via localized `SenderWallet` -> `ReceiverWallet` abstractions. 
`clientOperationId` is preserved seamlessly from the merchant's signature mapping into the `TransactionEntity`. Resulting statuses default explicitly to `PENDING_SYNC` maintaining offline double-entry boundaries securely avoiding UI balances inflation.

### Known Limitations
Network public key registries are simulated utilizing local runtime single-instantiated boundaries pending backend integration inside subsequent iterations mapping Trust networks properly.

## Phase 3.4 — Offline Payment Proof

### Payment Proof Transport Model
Phase 3.4 expands the security bound to establish offline Customer-to-Merchant confirmations. The transaction boundary ensures robust tracking through `OfflineQrPaymentProof`.

### Protocol Schema
```json
{
  "version": 1,
  "type": "LMB_PAYMENT_PROOF",
  "clientOperationId": "...",
  "transactionId": "...",
  "merchantId": "...",
  "merchantWalletId": "...",
  "customerWalletId": "...",
  "amount": "...",
  "currency": "INR",
  "timestamp": 123456789,
  "nonce": "securely_generated",
  "paymentMode": "OFFLINE_QR",
  "signature": "..."
}
```

### Trust & Integrity Rules
1. **Separation of Keys**: Customer signatures bound via `LMB_CUSTOMER_PROOF_KEY` guaranteeing distinction between Merchant issuance and Customer endorsement.
2. **Deterministic Canonicalization**: Serializes bytes exactly: `version|type|clientOperationId|transactionId|merchantId|merchantWalletId|customerWalletId|amount|currency|timestamp|nonce|paymentMode`.
3. **Transaction Binding**: Preserves backend structural logic by leveraging purely explicit IDs natively preventing double operation generation.
4. **Offline Double-Spending Warning**: This cryptographic signature definitively asserts Customer Consent, but intrinsically cannot prove global settled funds without eventual synchronization. Validation determines authorization, but final reconciliation executes securely strictly at Backend layer.

### Scan Flow & Replay Protections
Merchant receives the confirmation via inverted QR scanning. `OfflineQrPaymentProofValidator` re-evaluates all temporal boundaries actively mitigating replays via rigid Nonce mapping against existing offline operations definitively. Mismatched proof (e.g. invalid merchant, altered amounts) actively generates failed states throwing validation boundaries properly.


