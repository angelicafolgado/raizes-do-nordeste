import { Identidade } from '../../domain/entities/Usuario.js';
export interface HashPort { comparar(senha: string, hash: string): Promise<boolean> }
export interface TokenPort { gerarToken(payload: Identidade): string; validadeSegundos(): number }
