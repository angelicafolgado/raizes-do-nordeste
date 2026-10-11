export interface RegistroAuditoria {
  id: number;
  acao: string;
  detalhes: string;
  usuarioId: number | null;
  timestamp: Date;
}
