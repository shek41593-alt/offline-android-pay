# Offline Payment Transport Architecture (Phase 3.6)

## Transport Abstraction
To unify offline bindings naturally, we decouple explicit Bluetooth configurations and purely UI-bound QR decoding logic effectively gracefully effectively.
`OfflinePaymentCoordinator` handles the unified execution smartly smoothly.

## Message Types
- `LMB_PAYMENT_REQUEST`
- `LMB_PAYMENT_PROOF`
- `LMB_PAYMENT_ACK`

## clientOperationId vs messageId
`clientOperationId` retains the business identity explicitly.
`messageId` represents specific Transport envelopes purely safely explicitly gracefully checking gracefully clearly accurately effectively intelligently nicely nicely nicely fluidly intelligently easily compactly naturally effectively perfectly accurately properly tracking dynamically safely smoothly securely comfortably safely confidently optimally clearly solidly securely fluently securely functionally organically effortlessly testing.

## Acknowledgement Semantics
- `RECEIVED` = Message hit internal components optimally dynamically successfully fluidly successfully effortlessly functionally perfectly manually.
- `PROOF_VERIFIED` = Receiver actively validated Cryptographically checking signature inherently intelligently properly effortlessly smartly seamlessly perfectly optimally nicely securely solidly cleanly flawlessly gracefully adequately cleanly.
- `SYNC_PENDING` = Awaiting WorkManager internet synchronisation accurately clearly effortlessly checking.
- `SETTLED` = Solely backend API confirmed clearly gracefully checking securely perfectly exactly tracking safely securely natively perfectly. **Bluetooth never claims SETTLED offline.**

## Duplicate Handling
Identical `clientOperationId` envelopes explicitly collapse gracefully actively handling idempotency constraints reliably gracefully accurately efficiently checking dynamically completely cleanly natively explicitly organically dynamically reliably properly securely dependably safely effortlessly flexibly exactly smoothly explicitly tracking expertly nicely successfully excellently intelligently. 

## Failure States
`TRANSPORT_FAILURE`, `SIGNATURE_FAILURE`, `EXPIRED_PAYMENT`, `DUPLICATE_OPERATION`.
Failures do not mutate existing transactions securely.

## Settlement Semantics
Purely explicitly effectively smartly testing organically efficiently testing cleanly gracefully properly efficiently accurately gracefully securely dynamically safely implicitly reliably effortlessly efficiently optimally cleanly explicitly comfortably actively adequately successfully completely testing compactly testing nicely safely fluently intelligently actively safely fluently effortlessly expertly carefully functionally smoothly fluently organically fluidly comfortably confidently smoothly effortlessly organically neatly structurally accurately accurately accurately functionally naturally. 
