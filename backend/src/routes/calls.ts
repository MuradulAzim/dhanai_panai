import { FastifyInstance } from 'fastify';
import { TwilioService } from '../services/twilio.service';
import { config } from '../config';
import { query, memoryStore } from '../db';
import { verifyAppAuth } from './auth';
import { resellerStore } from '../db/store';

export async function callRoutes(fastify: FastifyInstance) {
  // Make Outbound Call
  fastify.post('/api/calls/outbound', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { to } = request.body as { to: string };
    if (!to) {
      return reply.status(400).send({ success: false, error: 'Destination phone number (to) is required' });
    }

    const apiKey = (request.headers['x-api-key'] as string) || (request.query as Record<string, string>)?.apiKey;
    let callerNumber = config.TWILIO_PHONE_NUMBER;
    let user = apiKey ? resellerStore.users.get(apiKey) : undefined;

    if (user) {
      if (user.status !== 'ACTIVE') {
        return reply.status(403).send({ success: false, error: 'Your account is suspended. Please contact admin.' });
      }
      if (user.callMinutesBalance <= 0) {
        return reply.status(402).send({
          success: false,
          error: 'Your call minutes quota is exhausted. Please request a recharge package from Settings.'
        });
      }
      callerNumber = user.assignedNumber || callerNumber;
    }

    const result = await TwilioService.makeOutboundCall(to);

    const callRecord = {
      callSid: result.callSid || 'CALL-' + Date.now(),
      apiKey: user?.apiKey,
      from: callerNumber,
      to,
      direction: 'OUTGOING',
      status: result.status || 'INITIATED',
      duration: 0,
      createdAt: new Date().toISOString()
    };

    memoryStore.calls.unshift(callRecord);
    resellerStore.calls.unshift(callRecord as any);

    await query(
      `INSERT INTO calls (call_sid, from_number, to_number, direction, status, created_at)
       VALUES ($1, $2, $3, $4, $5, NOW())
       ON CONFLICT (call_sid) DO UPDATE SET status = EXCLUDED.status`,
      [callRecord.callSid, callRecord.from, callRecord.to, callRecord.direction, callRecord.status]
    );

    return reply.send({
      success: result.success,
      callSid: result.callSid,
      status: result.status,
      callerNumber,
      remainingMinutes: user ? user.callMinutesBalance : undefined,
      message: result.error
    });
  });

  // Terminate Active Call
  fastify.post('/api/calls/terminate', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { callSid } = request.body as { callSid: string };
    if (!callSid) {
      return reply.status(400).send({ success: false, error: 'callSid is required' });
    }

    const result = await TwilioService.terminateCall(callSid);

    const memCall = memoryStore.calls.find(c => c.callSid === callSid);
    if (memCall) {
      memCall.status = 'COMPLETED';
    }

    await query(
      `UPDATE calls SET status = 'COMPLETED', updated_at = NOW() WHERE call_sid = $1`,
      [callSid]
    );

    return reply.send({ success: result.success, status: result.status || 'completed' });
  });

  // Get Call History
  fastify.get('/api/calls', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const apiKey = (request.headers['x-api-key'] as string) || (request.query as Record<string, string>)?.apiKey;
    const user = apiKey ? resellerStore.users.get(apiKey) : undefined;

    if (user) {
      const userCalls = memoryStore.calls.filter(c =>
        c.apiKey === user.apiKey ||
        c.from === user.assignedNumber ||
        c.to === user.assignedNumber ||
        !c.apiKey
      );
      return reply.send(userCalls);
    }

    return reply.send(memoryStore.calls);
  });
}
