import dotenv from 'dotenv';
dotenv.config();

export const config = {
  PORT: parseInt(process.env.PORT || '3000', 10),
  HOST: process.env.HOST || '0.0.0.0',
  BASE_URL: process.env.BASE_URL || 'http://localhost:3000',
  API_SECRET_KEY: process.env.API_SECRET_KEY || 'test-secret-token',
  DATABASE_URL: process.env.DATABASE_URL || 'postgres://postgres:postgres@localhost:5432/second_number',

  // Twilio credentials
  TWILIO_ACCOUNT_SID: process.env.TWILIO_ACCOUNT_SID || '',
  TWILIO_AUTH_TOKEN: process.env.TWILIO_AUTH_TOKEN || '',
  TWILIO_PHONE_NUMBER: process.env.TWILIO_PHONE_NUMBER || '+18005550199',
  TWILIO_VALIDATE_WEBHOOKS: process.env.TWILIO_VALIDATE_WEBHOOKS === 'true'
};

export function isTwilioConfigured(): boolean {
  return (
    Boolean(config.TWILIO_ACCOUNT_SID) &&
    config.TWILIO_ACCOUNT_SID.startsWith('AC') &&
    Boolean(config.TWILIO_AUTH_TOKEN)
  );
}
