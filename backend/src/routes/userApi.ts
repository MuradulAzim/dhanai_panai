import { FastifyInstance } from 'fastify';
import { resellerStore, RechargeRequest } from '../db/store';

export async function userApiRoutes(fastify: FastifyInstance) {
  // Public Rates and Mobile Banking numbers for Users
  fastify.get('/api/user/rates', async (_request, reply) => {
    return reply.send({
      pricePerSmsBdt: resellerStore.ratesAndBanking.pricePerSmsBdt,
      pricePerCallMinuteBdt: resellerStore.ratesAndBanking.pricePerCallMinuteBdt,
      bkashNumber: resellerStore.ratesAndBanking.bkashNumber,
      nagadNumber: resellerStore.ratesAndBanking.nagadNumber,
      rocketNumber: resellerStore.ratesAndBanking.rocketNumber
    });
  });

  // User submits a Package / API Key Request with bKash/Nagad TrxID & Screenshot
  fastify.post('/api/user/request-package', async (request, reply) => {
    const body = (request.body as Record<string, any>) || {};

    const userName = (body.userName || 'Valued User').trim();
    const userContact = (body.userContact || '').trim();
    const smsRequested = parseInt(body.smsRequested || '0', 10);
    const minutesRequested = parseInt(body.minutesRequested || '0', 10);
    const totalAmountBdt = parseFloat(body.totalAmountBdt || '0');
    const paymentMethod = (body.paymentMethod || 'BKASH').toUpperCase() as 'BKASH' | 'NAGAD' | 'ROCKET';
    const senderNumber = (body.senderNumber || '').trim();
    const transactionId = (body.transactionId || '').trim();
    const screenshotBase64 = body.screenshotBase64 || '';

    if (!userContact || !transactionId) {
      return reply.status(400).send({
        success: false,
        message: 'Contact number and Transaction ID (TrxID) are required.'
      });
    }

    const requestId = 'REQ-' + Date.now().toString().slice(-6);

    const newRequest: RechargeRequest = {
      id: requestId,
      userName,
      userContact,
      smsRequested,
      minutesRequested,
      totalAmountBdt,
      paymentMethod,
      senderNumber,
      transactionId,
      screenshotBase64,
      status: 'PENDING',
      createdAt: new Date().toISOString()
    };

    resellerStore.requests.unshift(newRequest);

    return reply.send({
      success: true,
      message: 'Request submitted successfully. Admin will review and activate your API Key.',
      request: newRequest
    });
  });

  // Check Status of a Request
  fastify.get('/api/user/request-status/:id', async (request, reply) => {
    const params = request.params as { id: string };
    const req = resellerStore.requests.find(r => r.id === params.id);

    if (!req) {
      return reply.status(404).send({ success: false, message: 'Request not found' });
    }

    return reply.send({
      success: true,
      request: req
    });
  });

  // Get User Profile by API Key (assigned virtual number and remaining balance)
  fastify.get('/api/user/profile', async (request, reply) => {
    const apiKey = (request.headers['x-api-key'] as string) || (request.query as Record<string, string>)?.apiKey;

    if (!apiKey) {
      return reply.status(401).send({ success: false, message: 'Missing x-api-key header' });
    }

    const user = resellerStore.users.get(apiKey);
    if (!user) {
      return reply.status(404).send({ success: false, message: 'Invalid or inactive API Key' });
    }

    return reply.send({
      success: true,
      user: {
        apiKey: user.apiKey,
        name: user.name,
        assignedNumber: user.assignedNumber,
        smsBalance: user.smsBalance,
        callMinutesBalance: user.callMinutesBalance,
        status: user.status
      }
    });
  });
}
