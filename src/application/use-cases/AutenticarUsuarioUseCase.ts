import { Store } from '../ports/Store.js';
import { HashPort, TokenPort } from '../ports/Security.js';
import { AppError } from '../../domain/errors/AppError.js';
export class AutenticarUsuarioUseCase {
  constructor(private store: Store, private hash: HashPort, private token: TokenPort) {}
  async executar(email: unknown, senha: unknown) {
    if (typeof email !== 'string' || !email.trim() || typeof senha !== 'string' || !senha || email.length > 254 || senha.length > 256)
      throw new AppError('DADOS_OBRIGATORIOS', 'Informe e-mail e senha válidos.', 422);
    const usuario = await this.store.usuarioPorEmail(email.trim().toLowerCase());
    if (!usuario || !await this.hash.comparar(senha, usuario.senha))
      throw new AppError('CREDENCIAIS_INVALIDAS', 'Credenciais de acesso inválidas.', 401);
    const { id, nome, role } = usuario;
    await this.store.auditar('LOGIN', {}, id);
    return { accessToken: this.token.gerarToken({ id, nome, role }), tokenType: 'Bearer',
      expiresIn: this.token.validadeSegundos(), usuario: { id, nome, email: usuario.email, role } };
  }
}
