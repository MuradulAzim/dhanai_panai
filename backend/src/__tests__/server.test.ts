import { buildServer } from '../server';

describe('Second Number Backend Server', () => {
  const server = buildServer();

  afterAll(async () => {
    await server.close();
  });

  test('GET / returns operational status', async () => {
    const response = await server.inject({
      method: 'GET',
      url: '/'
    });

    expect(response.statusCode).toBe(200);
    const body = JSON.parse(response.payload);
    expect(body.service).toContain('Second Number');
    expect(body.status).toBe('operational');
  });

  test('GET /api/status returns server config status', async () => {
    const response = await server.inject({
      method: 'GET',
      url: '/api/status'
    });

    expect(response.statusCode).toBe(200);
    const body = JSON.parse(response.payload);
    expect(body.status).toBe('online');
    expect(body).toHaveProperty('twilioConfigured');
    expect(body).toHaveProperty('twilioPhoneNumber');
  });

  test('Outbound call requires valid payload and auth', async () => {
    const unauthorized = await server.inject({
      method: 'POST',
      url: '/api/calls/outbound',
      payload: { to: '+1234567890' }
    });
    expect(unauthorized.statusCode).toBe(401);

    const authorized = await server.inject({
      method: 'POST',
      url: '/api/calls/outbound',
      headers: {
        'x-api-key': 'test-secret-token'
      },
      payload: { to: '+1234567890' }
    });
    expect(authorized.statusCode).toBe(200);
    const body = JSON.parse(authorized.payload);
    expect(body.success).toBe(true);
    expect(body).toHaveProperty('callSid');
  });

  test('Voice Webhook generates valid TwiML response', async () => {
    const response = await server.inject({
      method: 'POST',
      url: '/api/webhooks/voice/incoming',
      payload: {
        From: '+15551234567',
        To: '+18005550199',
        CallSid: 'CA123456'
      }
    });

    expect(response.statusCode).toBe(200);
    expect(response.headers['content-type']).toContain('text/xml');
    expect(response.payload).toContain('<Response>');
  });

  test('SMS Webhook receives message and responds with empty TwiML', async () => {
    const response = await server.inject({
      method: 'POST',
      url: '/api/webhooks/sms/incoming',
      payload: {
        From: '+15551234567',
        To: '+18005550199',
        Body: 'Hello world!',
        MessageSid: 'SM123456'
      }
    });

    expect(response.statusCode).toBe(200);
    expect(response.payload).toBe('<Response></Response>');
  });
});
