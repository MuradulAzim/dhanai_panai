import { FastifyInstance } from 'fastify';
import { TwilioService } from '../services/twilio.service';
import { config } from '../config';
import { query, memoryStore } from '../db';
import { verifyAppAuth } from './auth';
import { resellerStore } from '../db/store';

export async function messageRoutes(fastify: FastifyInstance) {
  // Send SMS
  fastify.post('/api/messages/send', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { to, body } = request.body as { to: string; body: string };
    if (!to || !body) {
      return reply.status(400).send({ success: false, error: 'Both "to" and "body" are required' });
    }

    const apiKey = (request.headers['x-api-key'] as string) || (request.query as Record<string, string>)?.apiKey;
    let senderNumber = config.TWILIO_PHONE_NUMBER;
    let user = apiKey ? resellerStore.users.get(apiKey) : undefined;

    if (user) {
      if (user.status !== 'ACTIVE') {
        return reply.status(403).send({ success: false, error: 'Your account is suspended. Please contact admin.' });
      }
      if (user.smsBalance <= 0) {
        return reply.status(402).send({
          success: false,
          error: 'Your SMS quota is exhausted. Please request a recharge package from Settings.'
        });
      }
      user.smsBalance -= 1;
      user.totalSmsSent += 1;
      senderNumber = user.assignedNumber || senderNumber;
    }

    const result = await TwilioService.sendSms(to, body);

    const messageRecord = {
      messageSid: result.messageSid || 'MSG-' + Date.now(),
      apiKey: user?.apiKey,
      conversationNumber: to,
      from: senderNumber,
      to,
      body,
      direction: 'OUTGOING',
      status: result.status || 'SENT',
      createdAt: new Date().toISOString()
    };

    memoryStore.messages.push(messageRecord);
    resellerStore.messages.push(messageRecord as any);

    await query(
      `INSERT INTO messages (message_sid, conversation_number, from_number, to_number, body, direction, status, is_read, created_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, TRUE, NOW())`,
      [messageRecord.messageSid, messageRecord.conversationNumber, messageRecord.from, messageRecord.to, messageRecord.body, messageRecord.direction, messageRecord.status]
    );

    await query(
      `INSERT INTO conversations (phone_number, last_message, last_message_at, unread_count)
       VALUES ($1, $2, NOW(), 0)
       ON CONFLICT (phone_number) DO UPDATE
       SET last_message = EXCLUDED.last_message, last_message_at = NOW()`,
      [to, body]
    );

    return reply.send({
      success: result.success,
      messageSid: result.messageSid,
      status: result.status,
      to,
      from: senderNumber,
      body,
      remainingSms: user ? user.smsBalance : undefined,
      errorMessage: result.error
    });
  });

  // Get Messages list
  fastify.get('/api/messages', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const apiKey = (request.headers['x-api-key'] as string) || (request.query as Record<string, string>)?.apiKey;
    const user = apiKey ? resellerStore.users.get(apiKey) : undefined;

    if (user) {
      const userMessages = memoryStore.messages.filter(m =>
        m.apiKey === user.apiKey ||
        m.from === user.assignedNumber ||
        m.to === user.assignedNumber ||
        !m.apiKey
      );
      return reply.send(userMessages);
    }

    return reply.send(memoryStore.messages);
  });

  // Get Conversation messages
  fastify.get('/api/conversations/:number/messages', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { number } = request.params as { number: string };
    const filtered = memoryStore.messages.filter(m => m.conversationNumber === number);
    return reply.send(filtered);
  });

  // Mark messages as read (sets status to READ so double ticks turn blue)
  fastify.post('/api/messages/mark-read', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { phoneNumber } = request.body as { phoneNumber?: string };

    if (phoneNumber) {
      memoryStore.messages.forEach(m => {
        if (m.conversationNumber === phoneNumber) {
          m.status = 'READ';
        }
      });

      await query(
        `UPDATE messages SET is_read = TRUE, status = 'READ' WHERE conversation_number = $1`,
        [phoneNumber]
      );
      await query(
        `UPDATE conversations SET unread_count = 0 WHERE phone_number = $1`,
        [phoneNumber]
      );
    }

    return reply.send({ success: true, status: 'READ' });
  });
}
