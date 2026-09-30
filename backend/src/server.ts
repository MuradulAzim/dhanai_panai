import Fastify, { FastifyInstance } from 'fastify';
import cors from '@fastify/cors';
import formbody from '@fastify/formbody';
import { callRoutes } from './routes/calls';
import { messageRoutes } from './routes/messages';
import { webhookRoutes } from './routes/webhooks';
import { statusRoutes } from './routes/status';
import { adminRoutes } from './routes/admin';
import { userApiRoutes } from './routes/userApi';
import { twilioProvisioningRoutes } from './routes/twilioProvisioning';
import { portalRoutes } from './routes/portal';

export function buildServer(): FastifyInstance {
  const fastify = Fastify({
    logger: true
  });

  // Enable CORS for web or emulator clients
  fastify.register(cors, {
    origin: true
  });

  // Enable formbody parsing for Twilio URL-encoded webhook payloads
  fastify.register(formbody);

  // Register routes
  fastify.register(statusRoutes);
  fastify.register(callRoutes);
  fastify.register(messageRoutes);
  fastify.register(webhookRoutes);
  fastify.register(adminRoutes);
  fastify.register(userApiRoutes);
  fastify.register(twilioProvisioningRoutes);
  fastify.register(portalRoutes);

  // Root health check & portal fallback
  fastify.get('/', async (_req, reply) => {
    reply.type('text/html');
    return reply.redirect('/portal');
  });

  return fastify;
}
