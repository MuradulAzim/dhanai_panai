import { FastifyRequest, FastifyReply } from 'fastify';
import { config } from '../config';

export async function verifyAppAuth(request: FastifyRequest, reply: FastifyReply) {
  // If no API secret is configured or client passes the secret key
  const apiKey = request.headers['x-api-key'] as string | undefined;
  const authHeader = request.headers['authorization'];
  const bearerToken = authHeader?.startsWith('Bearer ') ? authHeader.substring(7) : undefined;

  const providedToken = apiKey || bearerToken;

  if (config.API_SECRET_KEY && config.API_SECRET_KEY !== 'none') {
    if (!providedToken || providedToken !== config.API_SECRET_KEY) {
      reply.status(401).send({ error: 'Unauthorized: Invalid or missing API key' });
      return;
    }
  }
}
