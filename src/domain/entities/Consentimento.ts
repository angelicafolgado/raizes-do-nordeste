export interface Consentimento {
  id: number;
  usuarioId: number;
  finalidade: string;
  versaoAviso: string;
  aceito: boolean;
  demonstracao: boolean;
  registradoEm: Date;
}
