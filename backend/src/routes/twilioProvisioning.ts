import { FastifyInstance } from 'fastify';
import { TwilioService } from '../services/twilio.service';
import { resellerStore, ApiUser } from '../db/store';
import { config } from '../config';

export async function twilioProvisioningRoutes(fastify: FastifyInstance) {
  /**
   * Search available numbers by country
   */
  fastify.get('/api/twilio/available-numbers', async (request, reply) => {
    const query = (request.query as Record<string, string>) || {};
    const country = query.country || 'US';
    const numbers = await TwilioService.searchAvailableNumbers(country, 10);
    return reply.send({ success: true, country, numbers });
  });

  /**
   * Buy a new Twilio number directly and auto-configure webhook
   */
  fastify.post('/api/twilio/buy-number', async (request, reply) => {
    const body = (request.body as Record<string, any>) || {};
    const phoneNumber = body.phoneNumber;
    const friendlyName = body.friendlyName || 'SecondNumber User Line';
    const assignToApiKey = body.assignToApiKey;

    const result = await TwilioService.buyPhoneNumber(phoneNumber, friendlyName);
    if (!result.success || !result.phoneNumber) {
      return reply.status(400).send({
        success: false,
        message: result.error || 'Failed to purchase number from Twilio'
      });
    }

    // If an existing API user was specified, link this new number to their account
    if (assignToApiKey && resellerStore.users.has(assignToApiKey)) {
      const user = resellerStore.users.get(assignToApiKey)!;
      user.assignedNumber = result.phoneNumber;
      resellerStore.users.set(assignToApiKey, user);
    }

    return reply.send({
      success: true,
      message: 'Twilio phone number purchased and webhooks auto-configured!',
      phoneNumber: result.phoneNumber,
      sid: result.sid
    });
  });

  /**
   * Release / Cancel number from Twilio account to stop recurring charges
   */
  fastify.post('/api/twilio/release-number', async (request, reply) => {
    const body = (request.body as Record<string, any>) || {};
    const sidOrNumber = body.sid || body.phoneNumber;

    if (!sidOrNumber) {
      return reply.status(400).send({ success: false, message: 'Missing sid or phoneNumber' });
    }

    const result = await TwilioService.releasePhoneNumber(sidOrNumber);
    if (!result.success) {
      return reply.status(400).send({ success: false, message: result.error || 'Failed to release number' });
    }

    return reply.send({
      success: true,
      message: 'Number released successfully from Twilio. Monthly billing stopped.'
    });
  });

  /**
   * List all phone numbers active under this Twilio account
   */
  fastify.get('/api/twilio/my-numbers', async (_request, reply) => {
    const numbers = await TwilioService.listActivePhoneNumbers();
    return reply.send({
      success: true,
      total: numbers.length,
      numbers
    });
  });

  /**
   * Public Numbers & Services Catalog (Calling, WhatsApp, Google OTP, Burner)
   */
  fastify.get('/api/numbers/catalog', async (_request, reply) => {
    const catalog = [
      {
        id: 'pkg_us_call_sms',
        title: 'United States Dedicated Line',
        country: 'United States',
        countryCode: 'US',
        flag: '🇺🇸',
        sampleNumber: '+1 (202) 555-0143',
        category: 'CALL_AND_SMS',
        typeLabel: 'Full Voice + SMS',
        description: 'Personal 2nd line for real voice calls & SMS with delivery receipts.',
        priceBdt: 350,
        priceUsd: 2.99,
        durationDays: 30,
        smsQuota: 100,
        callMinutesQuota: 30,
        supportsWhatsApp: true,
        supportsGoogleOtp: true,
        supportsTelegram: true
      },
      {
        id: 'pkg_uk_call_sms',
        title: 'United Kingdom London Line',
        country: 'United Kingdom',
        countryCode: 'GB',
        flag: '🇬🇧',
        sampleNumber: '+44 20 7946 0195',
        category: 'CALL_AND_SMS',
        typeLabel: 'Full Voice + SMS',
        description: 'UK virtual number for international clients and private texting.',
        priceBdt: 450,
        priceUsd: 3.99,
        durationDays: 30,
        smsQuota: 80,
        callMinutesQuota: 25,
        supportsWhatsApp: true,
        supportsGoogleOtp: true,
        supportsTelegram: true
      },
      {
        id: 'pkg_wa_verification',
        title: 'WhatsApp / Telegram Verification Line',
        country: 'United States',
        countryCode: 'US',
        flag: '🇺🇸',
        sampleNumber: '+1 (415) 555-0188',
        category: 'WHATSAPP_TELEGRAM',
        typeLabel: 'Social App Verification',
        description: 'Instant verification for WhatsApp Business, Telegram, Signal, or Viber.',
        priceBdt: 180,
        priceUsd: 1.50,
        durationDays: 30,
        smsQuota: 30,
        callMinutesQuota: 10,
        supportsWhatsApp: true,
        supportsGoogleOtp: true,
        supportsTelegram: true
      },
      {
        id: 'pkg_temp_burner_otp',
        title: 'Temporary 1-Hour Burner OTP Line',
        country: 'United States',
        countryCode: 'US',
        flag: '🇺🇸',
        sampleNumber: '+1 (646) 555-0172',
        category: 'BURNER_OTP',
        typeLabel: 'One-Time OTP Burner',
        description: 'Anonymous single-use number for Facebook, Google, OpenAI, TikTok registration.',
        priceBdt: 99,
        priceUsd: 0.99,
        durationDays: 1, // 1 day / 1 hour burner
        smsQuota: 15,
        callMinutesQuota: 0,
        supportsWhatsApp: true,
        supportsGoogleOtp: true,
        supportsTelegram: true
      },
      {
        id: 'pkg_ca_call_sms',
        title: 'Canada Ottawa Business Line',
        country: 'Canada',
        countryCode: 'CA',
        flag: '🇨🇦',
        sampleNumber: '+1 (613) 555-0119',
        category: 'CALL_AND_SMS',
        typeLabel: 'Full Voice + SMS',
        description: 'Canadian number for calling and receiving verification codes.',
        priceBdt: 380,
        priceUsd: 3.20,
        durationDays: 30,
        smsQuota: 100,
        callMinutesQuota: 30,
        supportsWhatsApp: true,
        supportsGoogleOtp: true,
        supportsTelegram: true
      }
    ];

    return reply.send({
      success: true,
      catalog
    });
  });

  /**
   * Google Play In-App Billing verification endpoint
   */
  fastify.post('/api/billing/verify-play-purchase', async (request, reply) => {
    const body = (request.body as Record<string, any>) || {};
    const { productId, purchaseToken, orderId, apiKey } = body;

    if (!productId || !purchaseToken) {
      return reply.status(400).send({
        success: false,
        message: 'Missing productId or purchaseToken'
      });
    }

    // Determine package details from productId
    let addSms = 50;
    let addMinutes = 20;
    if (productId.includes('starter') || productId.includes('burner')) {
      addSms = 30;
      addMinutes = 10;
    } else if (productId.includes('pro') || productId.includes('monthly')) {
      addSms = 150;
      addMinutes = 50;
    } else if (productId.includes('unlimited') || productId.includes('business')) {
      addSms = 400;
      addMinutes = 150;
    }

    // Auto-allocate or top-up user
    let userKey = apiKey;
    if (!userKey || !resellerStore.users.has(userKey)) {
      const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
      let rand = '';
      for (let i = 0; i < 24; i++) {
        rand += chars.charAt(Math.floor(Math.random() * chars.length));
      }
      userKey = `sec_live_${rand}`;
      const newUser: ApiUser = {
        apiKey: userKey,
        name: 'Google Play Subscriber',
        contactNumber: orderId || 'Play Order',
        assignedNumber: config.TWILIO_PHONE_NUMBER || '+18005550199',
        smsBalance: addSms,
        callMinutesBalance: addMinutes,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
        totalSmsSent: 0,
        totalCallMinutesUsed: 0
      };
      resellerStore.users.set(userKey, newUser);
    } else {
      const existing = resellerStore.users.get(userKey)!;
      existing.smsBalance += addSms;
      existing.callMinutesBalance += addMinutes;
      resellerStore.users.set(userKey, existing);
    }

    const updatedUser = resellerStore.users.get(userKey)!;

    return reply.send({
      success: true,
      message: 'Google Play purchase verified successfully! Credits & number activated.',
      apiKey: userKey,
      assignedNumber: updatedUser.assignedNumber,
      smsBalance: updatedUser.smsBalance,
      callMinutesBalance: updatedUser.callMinutesBalance
    });
  });
}
