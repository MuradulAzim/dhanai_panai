import Fastify, { FastifyInstance } from 'fastify';
import cors from '@fastify/cors';
import formbody from '@fastify/formbody';
import { callRoutes } from './routes/calls';
import { messageRoutes } from './routes/messages';
import { webhookRoutes } from './routes/webhooks';
import { statusRoutes } from './routes/status';

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

  // Root health check
  fastify.get('/', async () => {
    return {
      service: 'Second Number Twilio Voice & SMS Backend',
      version: '1.0.0',
      status: 'operational'
    };
  });

  return fastify;
}
