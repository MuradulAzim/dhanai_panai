import { FastifyInstance } from 'fastify';
import { TwilioService } from '../services/twilio.service';
import { config } from '../config';
import { query, memoryStore } from '../db';
import { verifyAppAuth } from './auth';

export async function messageRoutes(fastify: FastifyInstance) {
  // Send SMS
  fastify.post('/api/messages/send', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { to, body } = request.body as { to: string; body: string };
    if (!to || !body) {
      return reply.status(400).send({ success: false, error: 'Both "to" and "body" are required' });
    }

    const result = await TwilioService.sendSms(to, body);

    const messageRecord = {
      messageSid: result.messageSid || 'MSG-' + Date.now(),
      conversationNumber: to,
      from: config.TWILIO_PHONE_NUMBER,
      to,
      body,
      direction: 'OUTGOING',
      status: result.status || 'SENT',
      createdAt: new Date().toISOString()
    };

    memoryStore.messages.push(messageRecord);

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
      from: config.TWILIO_PHONE_NUMBER,
      body,
      errorMessage: result.error
    });
  });

  // Get Messages list
  fastify.get('/api/messages', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const dbResult = await query(
      `SELECT id, message_sid as "messageSid", from_number as "from", to_number as "to",
              body, direction, status, created_at as "createdAt"
       FROM messages ORDER BY created_at DESC LIMIT 100`
    );

    if (dbResult.rows && dbResult.rows.length > 0) {
      return reply.send(dbResult.rows);
    }

    return reply.send(memoryStore.messages);
  });

  // Get Conversation messages
  fastify.get('/api/conversations/:number/messages', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { number } = request.params as { number: string };

    const dbResult = await query(
      `SELECT id, message_sid as "messageSid", from_number as "from", to_number as "to",
              body, direction, status, created_at as "createdAt"
       FROM messages
       WHERE conversation_number = $1
       ORDER BY created_at ASC`,
      [number]
    );

    if (dbResult.rows && dbResult.rows.length > 0) {
      return reply.send(dbResult.rows);
    }

    const filtered = memoryStore.messages.filter(m => m.conversationNumber === number);
    return reply.send(filtered);
  });

  // Mark messages as read
  fastify.post('/api/messages/mark-read', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { phoneNumber } = request.body as { phoneNumber?: string };

    if (phoneNumber) {
      await query(
        `UPDATE messages SET is_read = TRUE WHERE conversation_number = $1`,
        [phoneNumber]
      );
      await query(
        `UPDATE conversations SET unread_count = 0 WHERE phone_number = $1`,
        [phoneNumber]
      );
    }

    return reply.send({ success: true, message: 'Messages marked as read' });
  });
}
