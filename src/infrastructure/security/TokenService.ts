import jwt from 'jsonwebtoken';
import { Identidade } from '../../domain/entities/Usuario.js';
import { RoleUsuario } from '../../domain/enums/RoleUsuario.js';
export class TokenService {
  private static secret(): string {
    const secret = process.env.JWT_SECRET;
    if (!secret || secret.length < 32 || secret === 'SUBSTITUA_POR_UM_SEGREDO_ALEATORIO') throw new Error('Configure JWT_SECRET com pelo menos 32 caracteres aleatórios.');
    return secret;
  }
  static validadeSegundos(): number {
    const value = process.env.JWT_EXPIRES_IN ?? '8h';
    const match = /^(\d+)(s|m|h|d)?$/.exec(value);
    if (!match) throw new Error('JWT_EXPIRES_IN inválido. Exemplo: 8h.');
    const seconds = Number(match[1]) * ({ s: 1, m: 60, h: 3600, d: 86400 }[match[2] || 's']!);
    if (!Number.isSafeInteger(seconds) || seconds < 1) throw new Error('JWT_EXPIRES_IN inválido.');
    return seconds;
  }
  static validarConfiguracao() { this.secret(); this.validadeSegundos(); }
  static gerarToken(payload: Identidade): string {
    return jwt.sign(payload, this.secret(), { algorithm: 'HS256', expiresIn: this.validadeSegundos(), issuer: 'raizes-do-nordeste', audience: 'raizes-api' });
  }
  static verificarToken(token: string): Identidade {
    const payload = jwt.verify(token, this.secret(), { algorithms: ['HS256'], issuer: 'raizes-do-nordeste', audience: 'raizes-api' });
    if (typeof payload === 'string' || !Number.isSafeInteger(payload.id) || payload.id < 1 ||
      typeof payload.nome !== 'string' || !Object.values(RoleUsuario).includes(payload.role))
      throw new Error('Identidade inválida.');
    return { id: payload.id, nome: payload.nome, role: payload.role };
  }
}
