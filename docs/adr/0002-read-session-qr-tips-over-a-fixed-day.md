---
status: accepted
---

# Read session QR tips over a fixed day

The read-only `GET /api/v1/session/{sessionId}/qr-tips` endpoint retrieves
Electronic Tips for any Shift Session status. It requests a fixed 24-hour
duration beginning at the session's date and start work time, interpreted in
`Europe/Moscow`, from the tips bot's `TipsService.GetTipsByDuration` RPC.
The period does not depend on the current time, session closure, or Checkpoints;
this keeps the lookup identical for open, closed, and recalculating sessions.

The HTTP response expresses the total in whole Russian rubles, discarding
kopecks from the aggregated RPC result with integer division by 100. This
deliberately gives up fractional-ruble precision to match the application's
existing monetary units. Reading Electronic Tips does not change Checkpoints,
Shift Results, or Payments.

The Company UUID is shared with tips bot. Each call uses a three-second
deadline and retries `UNAVAILABLE` and `DEADLINE_EXCEEDED` up to twice after
the initial attempt, reusing the same period. The final deadline error becomes
HTTP 504; other final RPC failures become HTTP 502, never a zero total.
An actual zero total is a successful HTTP 200 response.
