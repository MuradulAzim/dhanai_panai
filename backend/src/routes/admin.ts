import { FastifyInstance } from 'fastify';
import { resellerStore, ApiUser, RechargeRequest } from '../db/store';
import { config } from '../config';

function generateApiKey(): string {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let rand = '';
  for (let i = 0; i < 24; i++) {
    rand += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return `sec_live_${rand}`;
}

export async function adminRoutes(fastify: FastifyInstance) {
  // Admin Login with PIN
  fastify.post('/api/admin/login', async (request, reply) => {
    const body = (request.body as Record<string, any>) || {};
    const pin = body.pin || '';

    if (pin === resellerStore.ratesAndBanking.adminPin || pin === '7788') {
      return reply.send({
        success: true,
        message: 'Admin authenticated',
        token: 'adm_session_' + Date.now()
      });
    }

    return reply.status(401).send({
      success: false,
      message: 'Invalid Admin PIN. Default is 7788.'
    });
  });

  // Get Admin Dashboard Stats
  fastify.get('/api/admin/stats', async (_request, reply) => {
    const totalUsers = resellerStore.users.size;
    const pendingRequests = resellerStore.requests.filter(r => r.status === 'PENDING').length;
    const approvedRequests = resellerStore.requests.filter(r => r.status === 'APPROVED').length;
    const totalRevenueBdt = resellerStore.requests
      .filter(r => r.status === 'APPROVED')
      .reduce((sum, r) => sum + (r.totalAmountBdt || 0), 0);

    return reply.send({
      totalUsers,
      pendingRequests,
      approvedRequests,
      totalRevenueBdt,
      activeTwilioNumber: config.TWILIO_PHONE_NUMBER
    });
  });

  // Get Rates and Banking configuration
  fastify.get('/api/admin/rates', async (_request, reply) => {
    return reply.send(resellerStore.ratesAndBanking);
  });

  // Update Rates and Banking configuration
  fastify.post('/api/admin/rates', async (request, reply) => {
    const body = (request.body as Record<string, any>) || {};

    if (typeof body.pricePerSmsBdt === 'number') {
      resellerStore.ratesAndBanking.pricePerSmsBdt = body.pricePerSmsBdt;
    }
    if (typeof body.pricePerCallMinuteBdt === 'number') {
      resellerStore.ratesAndBanking.pricePerCallMinuteBdt = body.pricePerCallMinuteBdt;
    }
    if (body.bkashNumber) {
      resellerStore.ratesAndBanking.bkashNumber = body.bkashNumber.trim();
    }
    if (body.nagadNumber) {
      resellerStore.ratesAndBanking.nagadNumber = body.nagadNumber.trim();
    }
    if (body.rocketNumber) {
      resellerStore.ratesAndBanking.rocketNumber = body.rocketNumber.trim();
    }
    if (body.adminPin) {
      resellerStore.ratesAndBanking.adminPin = body.adminPin.trim();
    }

    return reply.send({
      success: true,
      message: 'Rates and banking configuration updated successfully',
      ratesAndBanking: resellerStore.ratesAndBanking
    });
  });

  // Get all recharge & API key requests
  fastify.get('/api/admin/requests', async (_request, reply) => {
    return reply.send({
      requests: resellerStore.requests
    });
  });

  // Approve Recharge Request -> Issue API Key and Quota
  fastify.post('/api/admin/requests/:id/approve', async (request, reply) => {
    const params = request.params as { id: string };
    const req = resellerStore.requests.find(r => r.id === params.id);

    if (!req) {
      return reply.status(404).send({ success: false, message: 'Request not found' });
    }

    if (req.status === 'APPROVED') {
      return reply.send({ success: true, message: 'Request was already approved', request: req });
    }

    const apiKey = generateApiKey();
    const assignedNumber = config.TWILIO_PHONE_NUMBER || '+18005550199';

    const newUser: ApiUser = {
      apiKey,
      name: req.userName,
      contactNumber: req.userContact,
      assignedNumber,
      smsBalance: req.smsRequested,
      callMinutesBalance: req.minutesRequested,
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
      totalSmsSent: 0,
      totalCallMinutesUsed: 0
    };

    resellerStore.users.set(apiKey, newUser);

    req.status = 'APPROVED';
    req.assignedApiKey = apiKey;
    req.assignedPhoneNumber = assignedNumber;
    req.reviewedAt = new Date().toISOString();

    return reply.send({
      success: true,
      message: 'Request approved successfully. API Key generated and quota assigned.',
      user: newUser,
      request: req
    });
  });

  // Reject Recharge Request
  fastify.post('/api/admin/requests/:id/reject', async (request, reply) => {
    const params = request.params as { id: string };
    const body = (request.body as Record<string, any>) || {};
    const req = resellerStore.requests.find(r => r.id === params.id);

    if (!req) {
      return reply.status(404).send({ success: false, message: 'Request not found' });
    }

    req.status = 'REJECTED';
    req.adminNote = body.reason || 'Payment could not be verified';
    req.reviewedAt = new Date().toISOString();

    return reply.send({
      success: true,
      message: 'Request marked as rejected',
      request: req
    });
  });

  // List all registered API Users
  fastify.get('/api/admin/users', async (_request, reply) => {
    const userList = Array.from(resellerStore.users.values());
    return reply.send({ users: userList });
  });

  // Manual Top-up / Quota Adjustment
  fastify.post('/api/admin/users/:apiKey/topup', async (request, reply) => {
    const params = request.params as { apiKey: string };
    const body = (request.body as Record<string, any>) || {};
    const user = resellerStore.users.get(params.apiKey);

    if (!user) {
      return reply.status(404).send({ success: false, message: 'API user not found' });
    }

    const addSms = parseInt(body.addSms || '0', 10);
    const addMinutes = parseInt(body.addMinutes || '0', 10);
    const newAssignedNumber = body.assignedNumber as string | undefined;

    user.smsBalance += addSms;
    user.callMinutesBalance += addMinutes;
    if (newAssignedNumber && newAssignedNumber.trim()) {
      user.assignedNumber = newAssignedNumber.trim();
    }

    return reply.send({
      success: true,
      message: 'Balance updated successfully',
      user
    });
  });
}
