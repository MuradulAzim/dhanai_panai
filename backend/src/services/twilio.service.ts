import twilio from 'twilio';
import { config, isTwilioConfigured } from '../config';

let twilioClient: twilio.Twilio | null = null;

if (isTwilioConfigured()) {
  try {
    twilioClient = twilio(config.TWILIO_ACCOUNT_SID, config.TWILIO_AUTH_TOKEN);
    console.log('[Twilio] Client initialized successfully with Account SID:', config.TWILIO_ACCOUNT_SID);
  } catch (err: any) {
    console.warn('[Twilio] Failed to initialize Twilio client:', err.message);
  }
} else {
  console.log('[Twilio] Running in mock/development mode. Set TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN to enable live calls/SMS.');
}

export class TwilioService {
  /**
   * Dynamically update Twilio credentials from App Settings
   */
  static updateCredentials(accountSid: string, authToken: string, phoneNumber: string): boolean {
    try {
      config.TWILIO_ACCOUNT_SID = accountSid.trim();
      config.TWILIO_AUTH_TOKEN = authToken.trim();
      config.TWILIO_PHONE_NUMBER = phoneNumber.trim();
      twilioClient = twilio(config.TWILIO_ACCOUNT_SID, config.TWILIO_AUTH_TOKEN);
      console.log('[Twilio] Credentials dynamically updated for Account SID:', config.TWILIO_ACCOUNT_SID);
      return true;
    } catch (err: any) {
      console.error('[Twilio] Failed to update credentials dynamically:', err.message);
      return false;
    }
  }

  /**
   * Place an outbound call using Twilio REST API
   */
  static async makeOutboundCall(to: string): Promise<{ success: boolean; callSid?: string; status?: string; error?: string }> {
    if (!twilioClient) {
      // Mock call for local test/demo
      const mockSid = 'CA' + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
      return {
        success: true,
        callSid: mockSid,
        status: 'queued'
      };
    }

    try {
      const voiceTwimlUrl = `${config.BASE_URL.replace(/\/$/, '')}/api/webhooks/voice/twiml?to=${encodeURIComponent(to)}`;
      const statusCallbackUrl = `${config.BASE_URL.replace(/\/$/, '')}/api/webhooks/voice/status`;

      const call = await twilioClient.calls.create({
        to,
        from: config.TWILIO_PHONE_NUMBER,
        url: voiceTwimlUrl,
        statusCallback: statusCallbackUrl,
        statusCallbackEvent: ['initiated', 'ringing', 'answered', 'completed']
      });

      return {
        success: true,
        callSid: call.sid,
        status: call.status
      };
    } catch (error: any) {
      console.error('[Twilio] makeOutboundCall error:', error.message);
      return {
        success: false,
        error: error.message
      };
    }
  }

  /**
   * Terminate an active call
   */
  static async terminateCall(callSid: string): Promise<{ success: boolean; status?: string }> {
    if (!twilioClient || callSid.startsWith('CAmock') || callSid.startsWith('CALL-')) {
      return { success: true, status: 'completed' };
    }

    try {
      const call = await twilioClient.calls(callSid).update({ status: 'completed' });
      return { success: true, status: call.status };
    } catch (error: any) {
      console.error('[Twilio] terminateCall error:', error.message);
      return { success: false };
    }
  }

  /**
   * Send SMS via Twilio Messaging
   */
  static async sendSms(to: string, body: string): Promise<{ success: boolean; messageSid?: string; status?: string; error?: string }> {
    if (!twilioClient) {
      const mockSid = 'SM' + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
      return {
        success: true,
        messageSid: mockSid,
        status: 'sent'
      };
    }

    try {
      const statusCallbackUrl = `${config.BASE_URL.replace(/\/$/, '')}/api/webhooks/sms/status`;
      const msg = await twilioClient.messages.create({
        to,
        from: config.TWILIO_PHONE_NUMBER,
        body,
        statusCallback: statusCallbackUrl
      });

      return {
        success: true,
        messageSid: msg.sid,
        status: msg.status
      };
    } catch (error: any) {
      console.error('[Twilio] sendSms error:', error.message);
      return {
        success: false,
        error: error.message
      };
    }
  }

  /**
   * Validate webhook request signature against Twilio Auth Token
   */
  static validateWebhookSignature(
    expectedUrl: string,
    params: Record<string, any>,
    signatureHeader: string | undefined
  ): boolean {
    if (!config.TWILIO_VALIDATE_WEBHOOKS) {
      return true; // Skip in dev/local mode
    }

    if (!signatureHeader || !config.TWILIO_AUTH_TOKEN) {
      return false;
    }

    try {
      return twilio.validateRequest(
        config.TWILIO_AUTH_TOKEN,
        signatureHeader,
        expectedUrl,
        params
      );
    } catch (err: any) {
      console.error('[Twilio] Signature verification failed:', err.message);
      return false;
    }
  }

  /**
   * Generate TwiML for an incoming call to play message or forward
   */
  static generateIncomingVoiceTwiml(callerNumber: string): string {
    const VoiceResponse = twilio.twiml.VoiceResponse;
    const response = new VoiceResponse();
    response.say({ voice: 'alice' }, 'Thank you for calling Second Number. Please hold while we connect your call.');
    // Keep line active / play ringback or forward
    response.pause({ length: 2 });
    response.say({ voice: 'alice' }, 'The subscriber is currently on another line or using the mobile application. Please leave a message after the tone.');
    response.record({ maxLength: 60, playBeep: true });
    return response.toString();
  }

  /**
   * Generate TwiML for an outbound call
   */
  static generateOutboundVoiceTwiml(destinationNumber: string): string {
    const VoiceResponse = twilio.twiml.VoiceResponse;
    const response = new VoiceResponse();
    const dial = response.dial({ callerId: config.TWILIO_PHONE_NUMBER });
    dial.number(destinationNumber);
    return response.toString();
  }
}
