// Versões publicadas não devem ser alteradas: novas condições exigem nova versão.
export const avisoFidelidade = Object.freeze({
  finalidade: 'FIDELIDADE',
  versaoAviso: '1.0',
  demonstracao: true,
  opcional: true,
  titulo: 'Preferência de fidelidade — demonstração acadêmica',
  descricao: 'Registrar sua decisão opcional sobre participação na demonstração de fidelidade. Esta versão apenas registra a preferência: não acumula pontos, não concede benefícios e não envia mensagens. Um programa real exigirá novo aviso e nova decisão.',
  dados: ['Identificador do usuário autenticado', 'Decisão de aceitar ou revogar', 'Versão do aviso', 'Data e hora da decisão'],
  revogacao: 'Envie {"aceito":false} para PUT /api/privacidade/consentimentos/fidelidade. Pedidos e login permanecem disponíveis.',
  retencao: 'O histórico é mantido para demonstrar as decisões durante o projeto. Não há expurgo automático; consulte a política de privacidade da entrega para o procedimento de encerramento.',
  compartilhamento: 'Nenhum provedor externo recebe os dados de consentimento nesta demonstração.'
});
