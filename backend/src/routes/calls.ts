import { FastifyInstance } from 'fastify';
import { TwilioService } from '../services/twilio.service';
import { config } from '../config';
import { query, memoryStore } from '../db';
import { verifyAppAuth } from './auth';

export async function callRoutes(fastify: FastifyInstance) {
  // Make Outbound Call
  fastify.post('/api/calls/outbound', { preHandler: [verifyAppAuth] }, async (request, reply) => {
    const { to } = request.body as { to: string };
    if (!to) {
      return reply.status(400).send({ success: false, error: 'Destination phone number (to) is required' });
    }

    const result = await TwilioService.makeOutboundCall(to);

    const callRecord = {
      callSid: result.callSid || 'CALL-' + Date.now(),
      from: config.TWILIO_PHONE_NUMBER,
      to,
      direction: 'OUTGOING',
      status: result.status || 'INITIATED',
      duration: 0,
      createdAt: new Date().toISOString()
    };

    memoryStore.calls.unshift(callRecord);

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
    const dbResult = await query(
      `SELECT id, call_sid as "callSid", from_number as "from", to_number as "to",
              direction, status, duration, created_at as "createdAt"
       FROM calls ORDER BY created_at DESC LIMIT 100`
    );

    if (dbResult.rows && dbResult.rows.length > 0) {
      return reply.send(dbResult.rows);
    }

    return reply.send(memoryStore.calls);
  });
}
