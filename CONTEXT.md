# pkka

The Klub Alumnów WI AGH platform: a Spring Boot backend with a Next.js web app and an
Expo mobile app, serving alumni of the AGH Faculty of Computer Science.

## Language

### Membership

**Alumn**:
An authenticated user holding the Keycloak realm role `verified-alumn`. In the database
the equivalent proof is an `Application` with status `APPROVED` — there is no role column.
_Avoid_: member, graduate, alumnus (as a status)

**Application**:
A person's request to be recognised as an alumn, reviewed by an admin.
_Avoid_: request, form, submission

**Audience**:
Who an event is for — `PUBLIC` (any signed-in user), `ALL_ALUMNI` (alumni only), or
`SPECIFIC_GROUP` (reserved; no group belongs to it yet).
_Avoid_: visibility, permission, access level

### Notifications

**Installation**:
One copy of the mobile app on one device, identified by an **installation id** — a UUID
the app mints on first launch and keeps in SecureStore. Stable for the life of the
install; it does not change when the push token rotates.
_Avoid_: device, client, session

**Push token**:
The Expo-issued address of an installation (`ExponentPushToken[…]`). Rotates on app
updates and reinstalls, so it identifies nothing durably and is never a key.
_Avoid_: device id, registration id, FCM token (that is the raw token underneath)

**Announcement**:
A push sent once when a new event is created, to everyone in that event's audience.
_Avoid_: broadcast, notification (too general), alert

**Reminder**:
A push sent to a user who is registered for an event, a configured interval before it
starts.
_Avoid_: alert, nudge

**Reminder lead time**:
How long before an event starts its reminder is sent. Set per event; when absent, the
event sends no reminder at all.
_Avoid_: reminder window, offset, delay

**Channel**:
An Android notification category the user can mute independently — `events` for
announcements, `reminders` for reminders. Its importance and sound are fixed when it is
first created and can never be changed for an existing install.
_Avoid_: topic, category, group
