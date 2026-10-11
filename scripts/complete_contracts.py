"""Exemplos explícitos e contratos finais do Swagger (dados fictícios)."""
from copy import deepcopy

def complete_contracts(doc):
    schemas = doc['components']['schemas']
    timestamp = '2026-10-11T12:00:00.000Z'
    unidade = {'id': 1, 'nome': 'Unidade Recife Olinda', 'cidade': 'Recife'}
    produto = {'id': 1, 'nome': 'Baião de Dois Completo', 'descricao': 'Queijo coalho e carne de sol', 'preco': 38.5}
    item = {'id': 1, 'pedidoId': 1, 'produtoId': 1, 'quantidade': 2, 'precoUnitario': 38.5}
    pedido = {'id': 1, 'usuarioId': 1, 'unidadeId': 1, 'canalPedido': 'TOTEM', 'status': 'AGUARDANDO_PAGAMENTO', 'total': 91.0, 'criadoEm': timestamp, 'atualizadoEm': timestamp, 'itens': [item, {'id': 2, 'pedidoId': 1, 'produtoId': 2, 'quantidade': 1, 'precoUnitario': 14.0}]}
    pagamento = {'id': 1, 'pedidoId': 1, 'valor': 91.0, 'status': 'APROVADO', 'metodo': 'SIMULACAO_GATEWAY', 'transacaoId': 'MOCK-TX-exemplo', 'criadoEm': timestamp}
    consentimento = {'id': 1, 'usuarioId': 1, 'finalidade': 'FIDELIDADE', 'versaoAviso': '1.0', 'aceito': True, 'demonstracao': True, 'registradoEm': timestamp}
    estado = {'finalidade': 'FIDELIDADE', 'versaoAviso': '1.0', 'aceito': True, 'demonstracao': True, 'ultimoRegistroId': 1}
    def page(dados): return {'dados': dados, 'paginacao': {'pagina': 1, 'limite': 10, 'total': len(dados), 'totalPaginas': 1 if dados else 0}}
    catalogo = {**produto, 'estoques': [{'id': 1, 'unidadeId': 1, 'produtoId': 1, 'quantidade': 25, 'unidade': unidade}]}
    # A listagem inclui produto em cada item e pagamento (ausente = null).
    schemas['ProdutoResumo'] = {'type':'object','required':list(produto),'properties':{k:deepcopy(schemas['Produto']['properties'][k]) for k in produto}}
    schemas['ItemPedidoListado'] = deepcopy(schemas['ItemPedido'])
    schemas['ItemPedidoListado']['properties']['produto'] = {'$ref':'#/components/schemas/ProdutoResumo'}
    schemas['ItemPedidoListado']['required'].append('produto')
    schemas['PedidoListado'] = deepcopy(schemas['Pedido'])
    schemas['PedidoListado']['properties']['itens']['items'] = {'$ref':'#/components/schemas/ItemPedidoListado'}
    schemas['PedidoListado']['properties']['pagamento'] = {**deepcopy(schemas['Pagamento']), 'nullable':True}
    schemas['PedidoListado']['required'].append('pagamento')
    schemas['ListaPedidos']['properties']['dados']['items'] = {'$ref':'#/components/schemas/PedidoListado'}
    listado = deepcopy(pedido)
    listado['itens'][0]['produto'] = produto
    listado['itens'][1]['produto'] = {'id':2,'nome':'Tapioca Tradicional de Manteiga','descricao':'Tapioca artesanal nordestina','preco':14.0}
    listado['pagamento'] = None
    aviso = {'finalidade':'FIDELIDADE','versaoAviso':'1.0','demonstracao':True,'opcional':True,
      'titulo':'Preferência de fidelidade — demonstração acadêmica',
      'descricao':'Registrar sua decisão opcional sobre participação na demonstração de fidelidade. Esta versão apenas registra a preferência: não acumula pontos, não concede benefícios e não envia mensagens. Um programa real exigirá novo aviso e nova decisão.',
      'dados':['Identificador do usuário autenticado','Decisão de aceitar ou revogar','Versão do aviso','Data e hora da decisão'],
      'revogacao':'Envie {"aceito":false} para PUT /api/privacidade/consentimentos/fidelidade. Pedidos e login permanecem disponíveis.',
      'retencao':'O histórico é mantido para demonstrar as decisões durante o projeto. Não há expurgo automático; consulte a política de privacidade da entrega para o procedimento de encerramento.',
      'compartilhamento':'Nenhum provedor externo recebe os dados de consentimento nesta demonstração.'}
    exemplos = {
      ('/health','get'): {'status':'ok'},
      ('/auth/login','post'): {'accessToken':'JWT_FICTICIO_OBTENHA_UM_TOKEN_PELO_LOGIN','tokenType':'Bearer','expiresIn':28800,'usuario':{'id':1,'nome':'Maria da Silva','email':'maria@cliente.com','role':'CLIENTE'}},
      ('/unidades','get'): page([unidade]),
      ('/produtos','get'): page([catalogo]),
      ('/pedidos','post'): pedido,
      ('/pedidos','get'): page([listado]),
      ('/pedidos/{id}/pagamento','post'): {'sucesso':True,'mensagem':'Pagamento simulado aprovado.','transacaoId':pagamento['transacaoId'],'statusPedido':'EM_PREPARO','detalhes':pagamento},
      ('/pedidos/{id}/status','patch'): {**pedido,'status':'PRONTO'},
      ('/privacidade/aviso','get'): aviso,
      ('/privacidade/consentimentos','get'): {**page([consentimento]),'atual':estado},
      ('/privacidade/consentimentos/fidelidade','put'): consentimento,
      ('/auditoria','get'): page([{'id':1,'acao':'CONSULTA_PEDIDOS','usuarioId':3,'timestamp':timestamp,'detalhes':{'pagina':1,'limite':10,'quantidadeRetornada':1,'escopo':'REDE'}}])
    }
    codes = {
      ('/health','get'):[], ('/privacidade/aviso','get'):[],
      ('/auth/login','post'):[400,401,413,422,500],
      ('/unidades','get'):[409,422,500], ('/produtos','get'):[404,409,422,500],
      ('/pedidos','post'):[400,401,403,404,409,413,422,500],
      ('/pedidos','get'):[400,401,409,422,500],
      ('/pedidos/{id}/pagamento','post'):[400,401,403,404,409,413,422,500,502,503,504],
      ('/pedidos/{id}/status','patch'):[400,401,403,404,409,413,422,500],
      ('/privacidade/consentimentos','get'):[401,409,422,500],
      ('/privacidade/consentimentos/fidelidade','put'):[400,401,409,413,422,500],
      ('/auditoria','get'):[401,403,409,422,500]
    }
    erros = {400:('JSON_INVALIDO','Corpo JSON inválido.'),401:('NAO_AUTENTICADO','Cabeçalho de autorização não fornecido.'),403:('ACESSO_NEGADO','Perfil sem permissão para esta operação.'),404:('PEDIDO_NAO_ENCONTRADO','Pedido não encontrado.'),409:('CONFLITO_OPERACAO','Conflito ao processar a operação.'),413:('CORPO_MUITO_GRANDE','O corpo excede o limite de 100 KB.'),422:('PARAMETRO_INVALIDO','Informe parâmetros válidos.'),500:('ERRO_INTERNO_SERVIDOR','Falha inesperada no processamento.'),502:('RESPOSTA_GATEWAY_INVALIDA','Resposta do gateway não corresponde à solicitação.'),503:('GATEWAY_INDISPONIVEL','Gateway simulado indisponível.'),504:('GATEWAY_TIMEOUT','Tempo de resposta do gateway simulado esgotado.')}
    for (path, method), example in exemplos.items():
      op=doc['paths'][path][method]
      op['operationId'] = method+'_'+path.strip('/').replace('/','_').replace('{','').replace('}','')
      op['tags'] = [path.split('/')[1].capitalize()]
      op.setdefault('security', [])
      success='201' if (path,method)==('/pedidos','post') else '200'
      keep=op['responses'][success]
      keep['content']['application/json']['example']=deepcopy(example)
      op['responses']={success:keep}
      for code in codes[(path,method)]:
        error,message=erros[code]
        if path=='/auth/login' and code==401: error,message='CREDENCIAIS_INVALIDAS','Credenciais de acesso inválidas.'
        if path=='/produtos' and code==404: error,message='UNIDADE_NAO_ENCONTRADA','Unidade não encontrada.'
        if path=='/pedidos' and method=='get' and code==400: error,message='CANAL_INVALIDO','Filtro de canal inválido.'
        if path=='/pedidos' and method=='post' and code==409: error,message='ESTOQUE_INSUFICIENTE','Estoque insuficiente para Baião de Dois Completo.'
        sample={'error':error,'message':message,'details':[],'timestamp':timestamp,'path':'/api'+path.replace('{id}','1'),'requestId':'86b6d503-7449-4479-9af0-9bd890e6c587'}
        if code in (503,504): sample['details']=[{'field':'correlacaoId','issue':'c74b1106-96a0-478f-bb23-7b72473e1ef2'}]
        op['responses'][str(code)]={'description':message,'content':{'application/json':{'schema':{'$ref':'#/components/schemas/Erro'},'example':sample}}}
    pay=doc['paths']['/pedidos/{id}/pagamento']['post']
    approved=pay['responses']['200']['content']['application/json'].pop('example')
    refused=deepcopy(approved);refused.update(sucesso=False,mensagem='Pagamento simulado recusado.',statusPedido='AGUARDANDO_PAGAMENTO');refused['detalhes']['status']='RECUSADO'
    pay['responses']['200']['content']['application/json']['examples']={'aprovado':{'value':approved},'recusado':{'value':refused}}
    pay['requestBody']['content']['application/json'].pop('example',None)
    pay['requestBody']['content']['application/json']['examples']={name.lower():{'value':{'statusSimulado':name}} for name in ['APROVADO','RECUSADO','INDISPONIVEL','TIMEOUT']}
    doc['paths']['/privacidade/consentimentos/fidelidade']['put']['requestBody']['content']['application/json']['schema'] = {'oneOf':[
      {'type':'object','required':['aceito','versaoAviso'],'additionalProperties':False,'properties':{'aceito':{'type':'boolean','enum':[True]},'versaoAviso':{'type':'string','enum':['1.0']}}},
      {'type':'object','required':['aceito'],'additionalProperties':False,'properties':{'aceito':{'type':'boolean','enum':[False]},'versaoAviso':{'type':'string'}}}
    ]}
    for op in doc['paths'].values():
      for value in op.values():
        for parameter in value.get('parameters',[]):
          if parameter['in']=='path': parameter['schema'].update(minimum=1,maximum=2147483647)
