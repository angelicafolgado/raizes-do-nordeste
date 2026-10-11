import 'dotenv/config';
import { app } from './app.js';
import { prisma } from './infrastructure/database/prismaClient.js';
import { TokenService } from './infrastructure/security/TokenService.js';
async function main() {
  TokenService.validarConfiguracao();
  await prisma.$connect();
  const port = Number(process.env.PORT ?? 3000);
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('PORT inválida.');
  const server = app.listen(port, () => console.log('API: http://localhost:' + port + '/api-docs'));
  server.on('error', async err => { console.error(err); await prisma.$disconnect(); process.exitCode = 1; });
  const shutdown = () => { server.close(() => { void prisma.$disconnect(); }); };
  process.once('SIGINT', shutdown); process.once('SIGTERM', shutdown);
}
main().catch(async err => { console.error(err); await prisma.$disconnect(); process.exitCode = 1; });
