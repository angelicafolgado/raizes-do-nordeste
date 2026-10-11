import bcrypt from 'bcryptjs';
export class HashService {
  static comparar(senha: string, hash: string) { return bcrypt.compare(senha, hash); }
  static gerar(senha: string) { return bcrypt.hash(senha, 12); }
}
