import fs from 'fs';
import path from 'path';
import { Pool } from 'pg';
import { config } from '../config';

async function migrate() {
  console.log('[Migrate] Connecting to PostgreSQL at:', config.DATABASE_URL);
  const pool = new Pool({
    connectionString: config.DATABASE_URL
  });

  try {
    const schemaPath = path.join(__dirname, 'schema.sql');
    const sql = fs.readFileSync(schemaPath, 'utf8');
    await pool.query(sql);
    console.log('[Migrate] Successfully applied database schema.');
  } catch (error: any) {
    console.error('[Migrate] Migration failed:', error.message);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

if (require.main === module) {
  migrate();
}
