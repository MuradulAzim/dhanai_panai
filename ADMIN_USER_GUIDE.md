# Admin & User Complete Guide (Second Number System)
## Multi-User Reseller, Twilio Automation, Mobile Admin & Google Play Billing

---

## 1. Automated 1-Click Provisioning on Payment Approval
When an end-user submits a payment order (bKash/Nagad/Rocket/Card/IAP) and Admin taps **"অনুমোদন ও Key ইস্যু"**:
1. The backend automatically calls Twilio's Incoming Phone Numbers Provisioning API.
2. Twilio provisions a fresh virtual line and binds voice/SMS webhooks directly to your server.
3. The server generates a unique API key (`sec_live_xxxx...`) and assigns the dedicated phone number and SMS/Minute quotas.
4. The client's app is activated immediately without any manual configuration.

---

## 2. Full Mobile UI Control (Zero Terminal Required)
Every admin operation can be performed directly from your smartphone UI:
* **Twilio Number Inventory Tab**:
  * Browse available numbers live from Twilio across US, UK, Canada, Australia.
  * 1-Tap **"Buy ($1.15)"** button to add numbers to stock on-demand.
  * 1-Tap **"Release Number"** to cancel Twilio billing for that number if an end-user does not renew.
* **Pricing & Banking Management**:
  * Change per-SMS and per-Minute rates and update bKash/Nagad/Rocket phone numbers on the fly.
* **Web Portal Direct Access**:
  * Preview and copy your public online recharge portal link with one tap.

---

## 3. Marketplace Store Catalog
The app features a dedicated **Store** tab for users:
* **📞 Full Voice Calling & SMS Line**: 30-day renewable dedicated line for personal calling and texting.
* **💬 WhatsApp & Telegram Verification Line**: Dedicated number for activating WhatsApp Business, Telegram, Signal, or Viber.
* **🔥 Temporary 1-Hour Burner OTP Line**: Short-term anonymous number for bypassing verification on Google, Facebook, OpenAI, PayPal, etc.

---

## 4. Online Recharge Web Portal
Hosted on Fastify at `/portal` (and `/web`):
* Allows any user to purchase packages or verify numbers using desktop or mobile web browsers.
* Live rate display, payment copy buttons, order tracking, and live balance verification.

---

## 5. Google Play In-App Billing (IAP) Integration
Integrated using `com.android.billingclient:billing-ktx`:
* Supported Product IDs for Google Play Console:
  * `pkg_temp_burner_otp`
  * `pkg_us_call_sms`
  * `pkg_wa_verification`
* The app automatically queries Play Store product details, launches native Google purchase flows, acknowledges transactions, and updates quotas via `/api/billing/verify-play-purchase`.

---

## 6. Zero-Loss Expiry & Release System
* Virtual numbers have a 30-day lifecycle (or 1-hour for burners).
* If a customer renews, they pay the monthly fee.
* If a customer does not renew, tap **"নম্বর বাতিল"** in the Admin Panel. Twilio cancels the line immediately, ensuring zero financial inventory loss.
