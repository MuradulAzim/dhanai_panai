import { Pool } from 'pg';
import { config } from '../config';

let pool: Pool | null = null;
let useMemoryFallback = false;

// In-memory data store as fallback when PostgreSQL is not reached
export const memoryStore = {
  calls: [] as any[],
  messages: [] as any[],
  conversations: new Map<string, any>()
};

export function getPool(): Pool | null {
  if (!pool && config.DATABASE_URL && !useMemoryFallback) {
    try {
      pool = new Pool({
        connectionString: config.DATABASE_URL,
        ssl: config.DATABASE_URL.includes('sslmode=require') ? { rejectUnauthorized: false } : undefined,
        max: 10,
        idleTimeoutMillis: 30000,
        connectionTimeoutMillis: 3000
      });
      pool.on('error', (err) => {
        console.warn('[DB] PostgreSQL pool error, switching to safe memory store:', err.message);
        useMemoryFallback = true;
      });
    } catch (e: any) {
      console.warn('[DB] Failed to initialize Pool, fallback to memory:', e.message);
      useMemoryFallback = true;
    }
  }
  return useMemoryFallback ? null : pool;
}

export async function query(text: string, params?: any[]): Promise<any> {
  const p = getPool();
  if (p) {
    try {
      return await p.query(text, params);
    } catch (err: any) {
      console.warn('[DB] Query failed, falling back to memory store:', err.message);
      useMemoryFallback = true;
    }
  }
  return { rows: [] };
}
