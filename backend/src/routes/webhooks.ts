import { FastifyInstance } from 'fastify';
import { TwilioService } from '../services/twilio.service';
import { config } from '../config';
import { query, memoryStore } from '../db';

export async function webhookRoutes(fastify: FastifyInstance) {
  // Incoming Voice Call Webhook from Twilio
  fastify.post('/api/webhooks/voice/incoming', async (request, reply) => {
    const fullUrl = `${config.BASE_URL.replace(/\/$/, '')}/api/webhooks/voice/incoming`;
    const signature = request.headers['x-twilio-signature'] as string | undefined;
    const body = (request.body as Record<string, any>) || {};

    if (!TwilioService.validateWebhookSignature(fullUrl, body, signature)) {
      reply.status(403).send('Invalid Twilio Signature');
      return;
    }

    const fromNumber = body.From || 'Unknown';
    const toNumber = body.To || config.TWILIO_PHONE_NUMBER;
    const callSid = body.CallSid || 'INC-' + Date.now();

    const callRecord = {
      callSid,
      from: fromNumber,
      to: toNumber,
      direction: 'INCOMING',
      status: 'RINGING',
      duration: 0,
      createdAt: new Date().toISOString()
    };

    memoryStore.calls.unshift(callRecord);

    await query(
      `INSERT INTO calls (call_sid, from_number, to_number, direction, status, created_at)
       VALUES ($1, $2, $3, 'INCOMING', 'RINGING', NOW())
       ON CONFLICT (call_sid) DO UPDATE SET status = 'RINGING'`,
      [callSid, fromNumber, toNumber]
    );

    const twiml = TwilioService.generateIncomingVoiceTwiml(fromNumber);
    reply.header('Content-Type', 'text/xml');
    return reply.send(twiml);
  });

  // Outbound Call TwiML Generator
  fastify.post('/api/webhooks/voice/twiml', async (request, reply) => {
    const queryParams = (request.query as Record<string, string>) || {};
    const destinationNumber = queryParams.to || '+18005550100';

    const twiml = TwilioService.generateOutboundVoiceTwiml(destinationNumber);
    reply.header('Content-Type', 'text/xml');
    return reply.send(twiml);
  });

  // Voice Status Callback
  fastify.post('/api/webhooks/voice/status', async (request, reply) => {
    const body = (request.body as Record<string, any>) || {};
    const callSid = body.CallSid;
    const callStatus = body.CallStatus || 'completed';
    const duration = parseInt(body.CallDuration || '0', 10);

    if (callSid) {
      const memCall = memoryStore.calls.find(c => c.callSid === callSid);
      if (memCall) {
        memCall.status = callStatus.toUpperCase();
        memCall.duration = duration;
      }

      await query(
        `UPDATE calls SET status = $1, duration = $2, updated_at = NOW() WHERE call_sid = $3`,
        [callStatus.toUpperCase(), duration, callSid]
      );
    }

    return reply.send({ received: true });
  });

  // Incoming SMS Webhook from Twilio
  fastify.post('/api/webhooks/sms/incoming', async (request, reply) => {
    const fullUrl = `${config.BASE_URL.replace(/\/$/, '')}/api/webhooks/sms/incoming`;
    const signature = request.headers['x-twilio-signature'] as string | undefined;
    const body = (request.body as Record<string, any>) || {};

    if (!TwilioService.validateWebhookSignature(fullUrl, body, signature)) {
      reply.status(403).send('Invalid Twilio Signature');
      return;
    }

    const fromNumber = body.From || 'Unknown';
    const toNumber = body.To || config.TWILIO_PHONE_NUMBER;
    const messageBody = body.Body || '';
    const messageSid = body.MessageSid || 'SMS-' + Date.now();

    const messageRecord = {
      messageSid,
      conversationNumber: fromNumber,
      from: fromNumber,
      to: toNumber,
      body: messageBody,
      direction: 'INCOMING',
      status: 'RECEIVED',
      createdAt: new Date().toISOString()
    };

    memoryStore.messages.push(messageRecord);

    await query(
      `INSERT INTO messages (message_sid, conversation_number, from_number, to_number, body, direction, status, is_read, created_at)
       VALUES ($1, $2, $3, $4, $5, 'INCOMING', 'RECEIVED', FALSE, NOW())`,
      [messageSid, fromNumber, fromNumber, toNumber, messageBody]
    );

    await query(
      `INSERT INTO conversations (phone_number, last_message, last_message_at, unread_count)
       VALUES ($1, $2, NOW(), 1)
       ON CONFLICT (phone_number) DO UPDATE
       SET last_message = EXCLUDED.last_message,
           last_message_at = NOW(),
           unread_count = conversations.unread_count + 1`,
      [fromNumber, messageBody]
    );

    // Respond with empty TwiML
    reply.header('Content-Type', 'text/xml');
    return reply.send('<Response></Response>');
  });

  // SMS Status Callback
  fastify.post('/api/webhooks/sms/status', async (request, reply) => {
    const body = (request.body as Record<string, any>) || {};
    const messageSid = body.MessageSid;
    const messageStatus = body.MessageStatus || 'delivered';

    if (messageSid) {
      const memMsg = memoryStore.messages.find(m => m.messageSid === messageSid);
      if (memMsg) {
        memMsg.status = messageStatus.toUpperCase();
      }

      await query(
        `UPDATE messages SET status = $1 WHERE message_sid = $2`,
        [messageStatus.toUpperCase(), messageSid]
      );
    }

    return reply.send({ received: true });
  });
}
