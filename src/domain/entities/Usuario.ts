export interface Usuario {
  id: number; nome: string; email: string; senha: string; role: string; consentimentoLGPD: boolean;
}
export type Identidade = Pick<Usuario, 'id' | 'nome' | 'role'>;
