import { FastifyInstance } from 'fastify';
import { config, isTwilioConfigured } from '../config';
import { memoryStore, query } from '../db';
import { TwilioService } from '../services/twilio.service';
import { verifyAppAuth } from './auth';

export async function statusRoutes(fastify: FastifyInstance) {
  // Server Status & Profile info
  fastify.get('/api/status', async (request, reply) => {
    return reply.send({
      status: 'online',
      twilioConfigured: isTwilioConfigured(),
      twilioPhoneNumber: config.TWILIO_PHONE_NUMBER,
      activeCallsCount: memoryStore.calls.filter(c => c.status === 'IN_PROGRESS' || c.status === 'RINGING').length,
      serverTime: new Date().toISOString()
    });
  });

  // Update Twilio credentials dynamically from App Settings
  fastify.post('/api/settings/twilio', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { accountSid, authToken, phoneNumber } = request.body as {
      accountSid?: string;
      authToken?: string;
      phoneNumber?: string;
    };

    if (!accountSid || !authToken || !phoneNumber) {
      return reply.status(400).send({
        success: false,
        error: 'accountSid, authToken, and phoneNumber are all required'
      });
    }

    const updated = TwilioService.updateCredentials(accountSid, authToken, phoneNumber);
    return reply.send({
      success: updated,
      message: updated ? 'Twilio credentials successfully updated on backend' : 'Failed to initialize Twilio client with provided credentials',
      twilioPhoneNumber: config.TWILIO_PHONE_NUMBER,
      twilioConfigured: isTwilioConfigured()
    });
  });

  // Simulation endpoint for test alerts & demonstrations
  fastify.post('/api/simulate', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { type, from, body } = request.body as { type: string; from: string; body?: string };

    if (type === 'call') {
      const callRecord = {
        callSid: 'SIM-CALL-' + Date.now(),
        from: from || '+15550001122',
        to: config.TWILIO_PHONE_NUMBER,
        direction: 'INCOMING',
        status: 'RINGING',
        duration: 0,
        createdAt: new Date().toISOString()
      };
      memoryStore.calls.unshift(callRecord);

      await query(
        `INSERT INTO calls (call_sid, from_number, to_number, direction, status, created_at)
         VALUES ($1, $2, $3, 'INCOMING', 'RINGING', NOW())`,
        [callRecord.callSid, callRecord.from, callRecord.to]
      );

      return reply.send({ success: true, message: 'Incoming call simulated' });
    } else {
      const messageRecord = {
        messageSid: 'SIM-MSG-' + Date.now(),
        conversationNumber: from || '+15550001122',
        from: from || '+15550001122',
        to: config.TWILIO_PHONE_NUMBER,
        body: body || 'Simulated incoming SMS message',
        direction: 'INCOMING',
        status: 'RECEIVED',
        createdAt: new Date().toISOString()
      };
      memoryStore.messages.push(messageRecord);

      await query(
        `INSERT INTO messages (message_sid, conversation_number, from_number, to_number, body, direction, status, is_read, created_at)
         VALUES ($1, $2, $3, $4, $5, 'INCOMING', 'RECEIVED', FALSE, NOW())`,
        [messageRecord.messageSid, messageRecord.conversationNumber, messageRecord.from, messageRecord.to, messageRecord.body]
      );

      return reply.send({ success: true, message: 'Incoming SMS simulated' });
    }
  });
}
