# 🤖 VS Code AI Agent Master Prompt
### সেকেন্ড নম্বর প্রজেক্ট: সাব-ডোমেইন ব্যাকএন্ড ইন্সটলেশন, টুইলিও কনফিগারেশন ও প্লে-স্টোর পাবলিশিং গাইড

নিচের সম্পূর্ণ প্রম্পটটি কপি করে আপনার VS Code-এর AI Assistant (যেমন Cursor, GitHub Copilot, Cline, বা Roo Code)-এ পেস্ট করুন:

```markdown
Role & Task:
You are an expert DevOps and Full-Stack Android Engineer assistant working inside VS Code. Your objective is to help me:
1. Deploy and run the `backend` Node.js/Fastify server on my custom subdomain.
2. Open the integrated VS Code Simple Browser tabs for Twilio Console and Google Play Console.
3. Guide and assist me through Twilio Webhook configuration and Google Play Store App Publishing (including In-App Billing and Signed AAB).

---

### Step 1: Backend Installation & Custom Subdomain Configuration
1. Navigate to the `backend/` directory in this workspace.
2. Run `npm install` and build the project using `npm run build`.
3. Check the `.env` file in `backend/` and update `BASE_URL` with my custom subdomain:
   - Format: `BASE_URL=https://api.yourdomain.com` (ask me for my custom domain if not specified).
4. Configure HTTPS reverse proxy (Nginx / Caddy / Cloudflare Tunnel) to forward incoming traffic from port 80/443 to `http://localhost:3000`.
5. Start the server using PM2 or systemd:
   `pm2 start dist/index.js --name "second-number-backend"`
6. Verify that the health check and web portal load successfully at:
   - `https://api.yourdomain.com/portal`
   - `https://api.yourdomain.com/health`

---

### Step 2: Open Built-in Browser for Twilio Login & Configuration
1. Use VS Code's command `Simple Browser: Show` (or `workbench.action.webview.open`) to open:
   - Twilio Console: `https://console.twilio.com`
2. Assist me in obtaining and saving the credentials in `backend/.env`:
   - `TWILIO_ACCOUNT_SID`
   - `TWILIO_AUTH_TOKEN`
   - `TWILIO_PHONE_NUMBER`
3. Guide me in setting up Twilio Webhooks under Phone Numbers -> Manage -> Active Numbers:
   - Voice Webhook (POST): `https://api.yourdomain.com/api/webhooks/voice/incoming`
   - SMS Webhook (POST): `https://api.yourdomain.com/api/webhooks/sms/incoming`

---

### Step 3: Open Google Play Console Tab & Set Up In-App Products
1. Open a new browser tab inside VS Code:
   - Google Play Console: `https://play.google.com/console`
2. Help me set up the Google Play Developer Account ($25 one-time registration).
3. Under Monetization -> In-app products, guide me to create these 3 In-App Products matching the Android code:
   - Product ID 1: `pkg_temp_burner_otp` (One-Time Burner OTP - Recommended price: $0.99)
   - Product ID 2: `pkg_us_call_sms` (US Dedicated Call & SMS Line - Recommended price: $2.99)
   - Product ID 3: `pkg_wa_verification` (WhatsApp & Social Verification Line - Recommended price: $1.49)

---

### Step 4: App Store Listing & Publishing Metadata
Use the exact pre-approved Play Store Listing details:

- **App Title (Max 30 characters):**
  Second Number: Virtual Line

- **Short Description (Max 80 characters):**
  Make voice calls and send SMS with a private second virtual phone number.

- **Full Description:**
Keep your personal phone number private with Second Number. Get a dedicated virtual phone line for calling and texting without needing an extra SIM card or device.

Key Features:
• Dedicated Virtual Line: Make outbound calls and send SMS messages using your assigned virtual business line.
• Inbound Call & SMS Alerts: Receive incoming calls with live answer/decline controls and real-time heads-up alerts.
• Full SMS Threading: Chat seamlessly with delivery receipts (single and double checkmarks) and character count indicators.
• Clean Call Logs: Track all incoming, outgoing, and missed calls with complete duration logs and instant callback shortcuts.
• Flexible Packages: Convenient package-based usage with transparent minute and SMS quotas.

Second Number ensures your personal contacts, business clients, and online services remain neatly separated on a single mobile device.

- **Privacy Policy Link:**
  Help me host a clean privacy policy on GitHub Pages or Notion covering:
  - Phone and Microphone permission for voice calling.
  - SMS & Contact separation policy for privacy.

---

### Step 5: Android App Bundle (AAB) Generation & Release
1. Guide me to build the signed release Android App Bundle (AAB):
   `./gradlew :app:bundleRelease`
2. Ensure the keystore is properly referenced in `app/build.gradle.kts`.
3. Assist in uploading the generated `.aab` file located at:
   `app/build/outputs/bundle/release/app-release.aab` to Google Play Console Closed Testing or Production track.
```
