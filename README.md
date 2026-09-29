# Second Number: Android (Kotlin + Compose) + Fastify + PostgreSQL + Twilio

A complete, production-ready second phone number system using your existing **Twilio Phone Number**.

## System Architecture

* **Android Mobile App**: Built with Kotlin, Jetpack Compose, Material 3, Room local database for offline persistence, and Retrofit.
* **Backend API**: Fastify, TypeScript, and Twilio Node SDK.
* **Database**: PostgreSQL for storing calls, conversations, messages, and user states.
* **Security**:
  * **Permanent Twilio Credentials** (`TWILIO_ACCOUNT_SID` and `TWILIO_AUTH_TOKEN`) are strictly kept on the backend server—never embedded into the Android client.
  * Webhook signatures are validated using `twilio.validateRequest`.
  * Mobile client authenticates with the backend using API Key / Bearer tokens.

---

## 1. Quick Start with Docker Compose

### Prerequisites
* Docker and Docker Compose installed
* Your Twilio Account SID, Auth Token, and Twilio Phone Number (in E.164 format, e.g. `+18005550199`)

### Step 1: Configure Environment Variables
Copy `/backend/.env.example` to `/backend/.env` (or set them in root `.env`):
```bash
cp backend/.env.example backend/.env
```
Fill in your credentials:
```env
PORT=3000
HOST=0.0.0.0
BASE_URL=https://your-public-url.ngrok-free.app
API_SECRET_KEY=test-secret-token

DATABASE_URL=postgres://postgres:postgrespassword@postgres:5432/second_number
TWILIO_ACCOUNT_SID=ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=+18005550199
TWILIO_VALIDATE_WEBHOOKS=false
```

### Step 2: Start PostgreSQL and Fastify Server
```bash
docker-compose up --build
```
The backend will automatically:
1. Initialize the PostgreSQL database schema from `backend/src/db/schema.sql`.
2. Connect to Twilio with your credentials.
3. Serve REST endpoints on `http://localhost:3000`.

---

## 2. Twilio Webhook Configuration

To receive incoming calls and SMS on your device:

1. Open the [Twilio Console Phone Numbers page](https://console.twilio.com/us1/develop/phone-numbers/manage/incoming).
2. Click on your active Twilio phone number.
3. In **Voice & Fax**:
   * Set **A CALL COMES IN** to **Webhook**.
   * URL: `https://<YOUR_PUBLIC_DOMAIN>/api/webhooks/voice/incoming` (HTTP POST).
4. In **Messaging**:
   * Set **A MESSAGE COMES IN** to **Webhook**.
   * URL: `https://<YOUR_PUBLIC_DOMAIN>/api/webhooks/sms/incoming` (HTTP POST).
5. Click **Save Configuration**.

*(Tip: In local development, use tools like `ngrok http 3000` or Cloudflare Tunnels to obtain a public HTTPS URL).*

---

## 3. Android Application Features

* **Dialer Screen**:
  * Interactive 12-key telephone keypad (1-9, *, 0/+, #) with letters.
  * Real-time international country code prefix selector (+1, +44, +49, +61, etc.).
  * Outbound caller ID banner displaying your active Twilio phone number.
  * Long-press on 0 for `+`.
* **In-Call Screen & Answering**:
  * High-priority heads-up notification for incoming calls.
  * Active call screen with animated waveform, duration counter, Mute, Speakerphone, and End Call.
  * Answer and Decline buttons for incoming Twilio calls.
* **Calls Tab**:
  * Filter call logs by All, Missed, Outgoing, and Incoming.
  * Timestamp, call duration, and status.
  * One-tap callback and one-tap send SMS.
* **Messages Tab & Conversation Screen**:
  * Full SMS chat threads with delivery receipts and timestamps.
  * Character counter with GSM 160-character segment estimator.
  * Unread badge counter in bottom navigation.
  * Start new chats with any international phone number.
* **Settings Tab**:
  * Test server connectivity with real-time status indicators.
  * Change backend URL and API access token.
  * Copy pre-formatted Twilio webhook URLs.
  * Simulated incoming call and incoming SMS test triggers.
