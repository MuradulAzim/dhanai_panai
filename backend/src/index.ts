import { buildServer } from './server';
import { config } from './config';
import { getPool } from './db';

const server = buildServer();

async function start() {
  try {
    // Attempt database connection
    getPool();

    await server.listen({ port: config.PORT, host: config.HOST });
    console.log(`[Server] Second Number backend is running at http://${config.HOST}:${config.PORT}`);
    console.log(`[Server] Outbound line: ${config.TWILIO_PHONE_NUMBER}`);
    console.log(`[Server] Voice Webhook: ${config.BASE_URL.replace(/\/$/, '')}/api/webhooks/voice/incoming`);
    console.log(`[Server] SMS Webhook: ${config.BASE_URL.replace(/\/$/, '')}/api/webhooks/sms/incoming`);
  } catch (err) {
    server.log.error(err);
    process.exit(1);
  }
}

start();
