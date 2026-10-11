const fs = require('node:fs');
const { randomBytes } = require('node:crypto');
if (!fs.existsSync('.env')) {
  fs.writeFileSync('.env', 'PORT=3000\nDATABASE_URL="file:./dev.db"\nJWT_SECRET="' + randomBytes(48).toString('hex') + '"\nJWT_EXPIRES_IN="8h"\n');
  console.log('.env criado com segredo JWT aleatorio.');
}
