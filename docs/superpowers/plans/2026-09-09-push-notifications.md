# Push notifications (US #103) — design and branch plan

**Status:** settled. All decisions below were agreed in a grilling session on 2026-09-09.

**Goal:** Alumni get an Android push when a new event is created, and a reminder before an
event they registered for. Tapping it opens the event. They can turn pushes off in the app.

**Architecture:** One `device_tokens` row per app installation holds an Expo push token.
The backend sends through the Expo Push Service over `RestClient`, triggered by a
`@TransactionalEventListener` on event creation and by an hourly `@Scheduled` sweep for
reminders. The mobile app registers its token after login and deletes it on logout or when
the user turns notifications off.

**Tech stack:** Spring Boot 4.0.6 / Java 25 / Postgres / Flyway · Expo SDK 54 /
expo-notifications ~0.32.17 / expo-router 6 · Next.js web admin.

**Issues:** #103 (US) → #104, #105, #106, #107.

---

## 1. Scope

**In:** Android only. Announcement on event creation. Reminder before start, with a
per-event lead time. Deep link to the event. In-app on/off switch.

**Out:** iOS (no Apple account — the app never registers a token there). Email and Discord
channels. Notifications on event edit or deletion. Waitlist-promotion
pushes. `SPECIFIC_GROUP` audience (no groups exist).

---

## 2. Global constraints

- **Android only.**
- **Commits are subject-line only.** Conventional Commits, no body, no trailers.
- **Almost no source comments.** Only genuinely opaque mechanics.
- **New API enums need `@Schema(enumAsRef = true)`** on the enum class.
- **Backend gates:** `./gradlew spotlessCheck` and `./gradlew build -Pstrict`.
- **Frontend gates:** `pnpm run format:check`, then `turbo run lint typecheck build`.
- **PR titles must be Conventional Commits.**
- **Migrations start at `V18`.** `main` already holds `V16__event_author.sql`.
- **Write as little code as possible.**

---

## 3. Settled decisions

| # | Decision |
|---|---|
| Q1/Q8 | **Path A — Expo Push Service.** Free Expo account + `eas init`. Free, no volume cap, 600 notifications/second. |
| Q2 | Branch off `web-mobile-reconciliation`; **target every PR at `main`** so CI runs. |
| Q3 | Two backend branches (#104, #105), so #106 can start as soon as #104's spec lands. |
| Q4 | Reminder query written so PR #198's `REGISTERED` filter is a one-line change. Noted in the PR. |
| Q5 | **No `enabled` column.** Token presence *is* the switch: off deletes the row, on re-registers. |
| Q7/Q12 | Two Android channels, ids `events` (DEFAULT) and `reminders` (HIGH). Frozen at creation. |
| Q9 | Identity is an **installation id** — a UUID minted at first launch, kept in SecureStore. The token rotates; the installation id does not. |
| Q10/Q14 | Reminder lead time is **per event**, nullable. `null` means no reminder. No global default. |
| Q11 | Hourly sweep. No reminder for events created, or registrations made, inside the lead window. |
| Q13/Q17 | #105 keeps the automatic announcement on event creation. |
| Q15 | `DeviceNotRegistered` cleanup from tickets and from receipts: sent tickets are stored and checked 15 minutes later, then dropped after 24 hours. (Revised 2026-09-26: an uninstalled app is only reported in the receipt.) |
| Q16 | `event_registrations.reminder_sent_at` guards against double sends. |
| Q18/Q28 | The reminder select in `event-form.tsx` ships **inside #105**, so the field is never unreachable. |
| Q19 | Logout deletes the row; the client remembers the preference in SecureStore. |
| Q20/Q23 | Payload is `{ type, targetId }`. The union lives in `@pkka/domain`, hand-written — it never reaches `openapi.json`. |
| Q21/Q27 | The toggle lives in the **profile tab** (`user-panel.tsx`), not a new settings screen. |
| Q24 | Announcements go to every registered device in the event's audience, author included. |
| Q22 | Copy is Polish. Times formatted in **Europe/Warsaw**. |
| Q26/Q29 | No new GitHub issues. No notifications on event edit or deletion. |

---

## 4. Branch topology

```
web-mobile-reconciliation
└─ feat/104-be-device-token ──────────── PR → main
   ├─ feat/105-be-expo-push ──────────── PR → main   (backend + web select)
   └─ feat/106-mobile-notifications ──── PR → main
      └─ feat/107-mobile-settings ────── PR → main
```

Merge order: 104 → 105 → 106 → 107. #106 must sit on #104 because
`turbo typecheck` depends on `generate`, whose input is `backend/openapi.json`.

---

## 5. Unit 1 — `feat/104-be-device-token` (#104)

**New package** `backend/src/main/java/pl/edu/agh/backend/notifications/`:
`DeviceToken`, `DeviceTokenRepository`, `DeviceTokenService`, `DeviceTokenController`,
`DevicePlatform`, `dto/RegisterDeviceRequest`, `dto/DeviceTokenResponse`.

**Migration** `V18__device_tokens.sql`:

```sql
CREATE TABLE device_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    installation_id VARCHAR(64)  NOT NULL UNIQUE,
    token           VARCHAR(255) NOT NULL UNIQUE,
    platform        VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);
```

`installation_id` is unique because logout deletes the row, so one installation has at most
one row. `token` is unique so a recycled token cannot produce a duplicate send.

**Endpoints** under `/api/notifications/devices`:

- `PUT /{installationId}` — upsert. Body `{ token, platform }`. Idempotent, so a rotation
  and a fresh registration are the same call. Deletes any other row holding that token first.
- `DELETE /{installationId}` — unregister. 404 when it is not the caller's row.

**Tests:** endpoint test modelled on `EventRegistrationEndpointTest` (Testcontainers
`postgres:16`, MockMvc, `@Transactional`); service test for upsert, rotation, token steal,
and ownership.

---

## 6. Unit 2 — `feat/105-be-expo-push` (#105)

**New:** `NotificationType`, `ExpoPushClient`, `ExpoPushMessage`, `ExpoPushTicket`,
`NotificationService`, `EventCreatedEvent`, `EventNotificationListener`,
`EventReminderScheduler`, `NotificationProperties`.

**Modified:** `AdminEventService.create` (publish the event), `Event` +
`EventRequest` + `AdminEventResponse` (`reminderLeadTimeMinutes`), `EventRegistration`
(`reminderSentAt`), `application.yml`, `BackendApplication` (`@EnableScheduling`),
`frontend/apps/web/components/admin/event-form.tsx` (the select), `backend/openapi.json`.

**Migration** `V19__event_reminders.sql`:

```sql
ALTER TABLE events ADD COLUMN reminder_lead_time_minutes INTEGER;
ALTER TABLE event_registrations ADD COLUMN reminder_sent_at TIMESTAMPTZ;
```

**Reminder query — no time window.** Selecting "due and not yet sent" instead of a window
makes a missed sweep self-healing:

```
reminder_lead_time_minutes IS NOT NULL
AND reminder_sent_at IS NULL
AND starts_at > now()
AND starts_at - (reminder_lead_time_minutes * interval '1 minute') <= now()
```

The `REGISTERED`-status filter for PR #198 is one more `AND` on this query.

**Audience.** `PUBLIC` → every device. `ALL_ALUMNI` → devices whose user has an `APPROVED`
application, mirroring `AlumniSpecifications.isApprovedAlumnus()`. `SPECIFIC_GROUP` → none.

**Expo transport.** `POST https://exp.host/--/api/v2/push/send`, always a JSON array,
chunked at **100 recipients**, optional bearer token from config. `details.error` is a
`String`, never an enum. Only `DeviceNotRegistered` deletes a token.

**Scheduler must not run in tests.** There is no `src/test/resources`, so `@SpringBootTest`
inherits the `dev` profile. The scheduled method is gated on
`app.notifications.reminders.enabled`, which the tests set to `false`.

**Copy.** Announcement: `Nowe wydarzenie` / `{title}`. Reminder: `Przypomnienie` /
`{title} — {dd.MM o HH:mm}` in Europe/Warsaw.

---

## 7. Unit 3 — `feat/106-mobile-notifications` (#106)

**New:** `frontend/packages/domain/notifications.ts` (the payload union),
`frontend/apps/mobile/lib/notifications.ts`.

**Modified:** `app.json` (plugin, `googleServicesFile`, `extra.eas.projectId`),
`package.json`, `app/_layout.tsx`, `lib/auth-context.tsx`.

**Order is load-bearing:** create channel → request permission → get token. On Android 13+
the permission prompt does not appear until a channel exists, and nothing errors if you get
it wrong.

**Rotation:** `addPushTokenListener` fires on roll. Call
`getExpoPushTokenAsync({ projectId, devicePushToken })` inside it — passing the token you
were handed avoids the documented infinite loop.

**Deep link:** synchronous `getLastNotificationResponse()` in a root-layout effect for cold
start, `addNotificationResponseReceivedListener` for warm taps, then
`clearLastNotificationResponse()` so the sticky response does not re-navigate.

**`projectId` is supplied by the user later.** Read from config; the code compiles and
typechecks without it and fails loudly at runtime.

---

## 8. Unit 4 — `feat/107-mobile-settings` (#107)

A "Powiadomienia" section in `components/profile/user-panel.tsx`, above logout. Off deletes
the device row and records the choice in SecureStore; on re-registers. When OS permission is
permanently denied, the row opens system settings instead.

---

## 9. Hazards

- **`openapi.json`** is a hand-dumped snapshot CI trusts. `./gradlew test` overwrites it —
  `git checkout backend/openapi.json` after any test run, and re-dump only from a real server
  start. springdoc orders `Page`/`Pageable`/`Sort` properties non-deterministically, so
  compare parsed JSON before believing a diff.
- **orval does not prune.** `rm -rf frontend/packages/api/src/generated` before regenerating.
- **Android builds need JDK 17**, not the default 26.
- **Metro cannot use 8081** — that is Keycloak.
- **The FCM service account key is a secret**; `google-services.json` is not.
