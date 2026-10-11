from pathlib import Path
import json
from complete_contracts import complete_contracts
from complete_postman import complete_postman

root = Path(__file__).resolve().parents[1]
p = root / 'src/api/docs/swagger.json'
d = json.loads(p.read_text(encoding='utf-8'))
d['info']['version'] = '1.0.0'
d['info']['description'] = 'API de demonstração com estoque por unidade, JWT e pagamentos simulados. Não realiza cobranças reais.'
d['servers'] = [{'url': '/api', 'description': 'Servidor atual'}]
def string(**extra): return {'type': 'string', **extra}
def integer(**extra): return {'type': 'integer', 'minimum': 1, **extra}
def obj(props, required=None): return {'type': 'object', 'properties': props, 'required': list(props) if required is None else required}
def arr(item): return {'type': 'array', 'items': item}
def ref(name): return {'$ref': '#/components/schemas/' + name}
def response(desc, schema): return {'description': desc, 'content': {'application/json': {'schema': schema}}}
canais = ['APP', 'TOTEM', 'BALCAO', 'PICKUP', 'WEB']
status = ['AGUARDANDO_PAGAMENTO', 'EM_PREPARO', 'PRONTO', 'ENTREGUE', 'CANCELADO']
money = {'type': 'number', 'minimum': 0}
date = string(format='date-time')
schemas = {
 'Erro': obj({'error': string(), 'message': string(), 'details': arr(obj({'field': string(), 'issue': string()}, ['issue'])), 'timestamp': date, 'path': string(), 'requestId': string(format='uuid')}),
 'Login': obj({'email': string(format='email'), 'senha': string(format='password')}),
 'Usuario': obj({'id': integer(), 'nome': string(), 'email': string(), 'role': string(enum=['CLIENTE','ATENDENTE','COZINHA','ADMIN'])}),
 'Autenticacao': obj({'accessToken': string(), 'tokenType': string(enum=['Bearer']), 'expiresIn': integer(), 'usuario': ref('Usuario')}),
 'Unidade': obj({'id': integer(), 'nome': string(), 'cidade': string()}),
 'Estoque': obj({'id': integer(), 'unidadeId': integer(), 'produtoId': integer(), 'quantidade': integer(minimum=0), 'unidade': ref('Unidade')}),
 'Produto': obj({'id': integer(), 'nome': string(), 'descricao': string(), 'preco': money, 'estoques': arr(ref('Estoque'))}),
 'NovoPedido': obj({'unidadeId': integer(), 'canalPedido': string(enum=canais), 'itens': {**arr(obj({'produtoId': integer(), 'quantidade': integer()})), 'minItems': 1, 'maxItems': 100}}),
 'ItemPedido': obj({'id': integer(), 'pedidoId': integer(), 'produtoId': integer(), 'quantidade': integer(), 'precoUnitario': money}),
 'Pedido': obj({'id': integer(), 'usuarioId': integer(), 'unidadeId': integer(), 'canalPedido': string(enum=canais), 'status': string(enum=status), 'total': money, 'criadoEm': date, 'atualizadoEm': date, 'itens': arr(ref('ItemPedido'))}),
 'ListaPedidos': obj({'dados': arr(ref('Pedido')), 'paginacao': obj({'pagina': integer(), 'limite': integer(maximum=100), 'total': integer(minimum=0), 'totalPaginas': integer(minimum=0)})}),
 'Pagamento': obj({'id': integer(), 'pedidoId': integer(), 'valor': money, 'status': string(enum=['APROVADO','RECUSADO']), 'metodo': string(), 'transacaoId': string(), 'criadoEm': date}),
 'ResultadoPagamento': obj({'sucesso': {'type': 'boolean'}, 'mensagem': string(), 'transacaoId': string(), 'statusPedido': string(enum=status), 'detalhes': ref('Pagamento')})
}
d['components']['schemas'] = schemas
paths = d['paths']
paths['/unidades'] = {'get': {'summary': 'Listar unidades', 'responses': {'200': response('Unidades', arr(ref('Unidade')))}}}
paths['/health'] = {'get': {'summary': 'Disponibilidade da API', 'responses': {'200': response('Disponível', obj({'status': string(enum=['ok'])}))}}}
for path, method, name in [('/auth/login','post','Login'),('/pedidos','post','NovoPedido')]:
 paths[path][method]['requestBody']['content']['application/json']['schema'] = ref(name)
paths['/pedidos/{id}/pagamento']['post']['requestBody']['content']['application/json']['schema'] = obj({'statusSimulado': string(enum=['APROVADO','RECUSADO'])})
paths['/pedidos/{id}/status']['patch']['requestBody']['content']['application/json']['schema'] = obj({'status': string(enum=status)})
paths['/produtos']['get']['parameters'] = [{'name': 'unidadeId', 'in': 'query', 'schema': integer(), 'description': 'Filtrar cardápio e estoque por unidade.'}]
paths['/pedidos']['get']['description'] = 'CLIENTE vê apenas seus pedidos. ATENDENTE, COZINHA e ADMIN consultam todos. Máximo de 100 itens por página.'
paths['/pedidos']['post']['description'] = 'CLIENTE, ATENDENTE ou ADMIN. Pedido pertence ao usuário autenticado. Itens repetidos são agrupados. Falhas revertem toda a reserva.'
paths['/pedidos/{id}/pagamento']['post']['description'] = 'CLIENTE (próprio pedido), ATENDENTE ou ADMIN. Recusa mantém reserva e permite nova tentativa. Aprovação avança para EM_PREPARO. Última tentativa em pagamento; histórico completo na auditoria.'
paths['/pedidos/{id}/status']['patch']['description'] = 'COZINHA ou ADMIN. EM_PREPARO → PRONTO → ENTREGUE; AGUARDANDO_PAGAMENTO → CANCELADO, com devolução de estoque. Este mock não implementa estorno de pedidos pagos.'
for param in paths['/pedidos']['get']['parameters']:
 if param['name'] in ('page','limit'): param['schema']['minimum'] = 1
 if param['name'] == 'limit': param['schema']['maximum'] = 100
for path, method, code, schema in [('/auth/login','post','200',ref('Autenticacao')),('/produtos','get','200',arr(ref('Produto'))),('/pedidos','post','201',ref('Pedido')),('/pedidos','get','200',ref('ListaPedidos')),('/pedidos/{id}/pagamento','post','200',ref('ResultadoPagamento')),('/pedidos/{id}/status','patch','200',ref('Pedido'))]:
 paths[path][method]['responses'][code] = response('Sucesso', schema)
errors = {'400':'JSON, canal ou status inválido','401':'Não autenticado','403':'Acesso negado','404':'Recurso não encontrado','409':'Conflito, estoque insuficiente ou transição inválida','413':'Corpo excede 100 KB','422':'Dados inválidos','500':'Falha interna'}
for path, operations in paths.items():
 for method, operation in operations.items():
  codes = ['500']
  if path not in ('/health','/unidades'): codes += ['400','422']
  if path.startswith('/pedidos'): codes += ['401','403','404','409','413']
  if path == '/auth/login': codes += ['401','413']
  if path == '/produtos': codes += ['404']
  for code in set(codes) | (set(operation['responses']) & set(errors)):
   operation['responses'][code] = response(errors[code], ref('Erro'))
for resource, entity in [('produtos','Produto'),('unidades','Unidade')]:
 operation = paths['/'+resource]['get']
 operation['parameters'] = [x for x in operation.get('parameters',[]) if x['name'] not in ('page','limit')] + [{'name':'page','in':'query','schema':integer(default=1)},{'name':'limit','in':'query','schema':integer(default=10,maximum=100)}]
 name = 'Lista'+resource.capitalize()
 schemas[name] = obj({'dados':arr(ref(entity)), 'paginacao':schemas['ListaPedidos']['properties']['paginacao']})
 operation['responses']['200'] = response('Página de resultados',ref(name))
 operation['responses']['422'] = response('Paginação inválida',ref('Erro'))
pay = paths['/pedidos/{id}/pagamento']['post']
pay['requestBody']['content']['application/json']['schema'] = obj({'statusSimulado':string(enum=['APROVADO','RECUSADO','INDISPONIVEL','TIMEOUT'])})
pay['description'] += ' INDISPONIVEL retorna 503; TIMEOUT retorna 504. Falhas técnicas preservam pedido, pagamento anterior e reserva e são auditadas. TIMEOUT é simulado imediatamente, sem espera real.'
for code,message in [('502','Resposta do gateway não corresponde à solicitação'),('503','Gateway simulado indisponível'),('504','Timeout simulado do gateway')]:
 pay['responses'][code] = response(message,ref('Erro'))
# Contratos de privacidade e auditoria (etapa 3).
auth = [{'bearerAuth': []}]
pag_params = [{'name':'page','in':'query','schema':integer(default=1)}, {'name':'limit','in':'query','schema':integer(default=10,maximum=100)}]
schemas['Consentimento'] = obj({'id':integer(),'usuarioId':integer(),'finalidade':string(enum=['FIDELIDADE']), 'versaoAviso':string(), 'aceito':{'type':'boolean'}, 'demonstracao':{'type':'boolean'}, 'registradoEm':date})
schemas['EstadoConsentimento'] = obj({'finalidade':string(enum=['FIDELIDADE']),'versaoAviso':string(),'aceito':{'type':'boolean'},'demonstracao':{'type':'boolean'},'ultimoRegistroId':integer(nullable=True)})
schemas['ListaConsentimentos'] = obj({'dados':arr(ref('Consentimento')),'paginacao':schemas['ListaPedidos']['properties']['paginacao'],'atual':ref('EstadoConsentimento')})
schemas['RegistroAuditoria'] = obj({'id':integer(),'acao':string(),'usuarioId':integer(nullable=True),'timestamp':date,'detalhes':{'type':'object','description':'Projeção de metadados operacionais permitidos; não contém senha, token, e-mail ou objeto de usuário.'}})
schemas['ListaAuditoria'] = obj({'dados':arr(ref('RegistroAuditoria')),'paginacao':schemas['ListaPedidos']['properties']['paginacao']})
schemas['AvisoPrivacidade'] = obj({'finalidade':string(enum=['FIDELIDADE']), 'versaoAviso':string(example='1.0'),'demonstracao':{'type':'boolean'},'opcional':{'type':'boolean'},'titulo':string(),'descricao':string(),'dados':arr(string()),'revogacao':string(),'retencao':string(),'compartilhamento':string()})
def privacy_responses(schema, codes):
 values = {'200':response('Sucesso',schema)}
 for code in codes: values[code] = response(errors.get(code,'Conflito na operação'),ref('Erro'))
 return values
paths['/privacidade/aviso'] = {'get':{'summary':'Consultar aviso opcional da demonstração de fidelidade','description':'Não ativa um programa real de pontos ou comunicação. Consulte a versão antes de aceitar.','responses':{'200':response('Aviso vigente',ref('AvisoPrivacidade'))}}}
paths['/privacidade/consentimentos'] = {'get':{'summary':'Consultar meu estado e histórico de consentimento','security':auth,'description':'Identidade vem do JWT. Histórico próprio paginado; estado atual é calculado independentemente da página. A consulta é auditada.','parameters':pag_params,'responses':privacy_responses(ref('ListaConsentimentos'),['401','409','422','500'])}}
paths['/privacidade/consentimentos/fidelidade'] = {'put':{'summary':'Registrar ou revogar minha decisão opcional','security':auth,'description':'Somente o titular autenticado. Aceite exige a versão vigente. Revogação não exige versão atual e não bloqueia pedidos. PUT repetido não duplica evento. Histórico é de demonstração; flag legada não é autorização.','requestBody':{'required':True,'content':{'application/json':{'schema':{**obj({'aceito':{'type':'boolean'},'versaoAviso':string()},['aceito']),'additionalProperties':False},'examples':{'aceitar':{'value':{'aceito':True,'versaoAviso':'1.0'}},'revogar':{'value':{'aceito':False}}}}}},'responses':privacy_responses(ref('Consentimento'),['400','401','409','422','500'])}}
paths['/auditoria'] = {'get':{'summary':'Consultar auditoria (ADMIN)','security':auth,'description':'Somente ADMIN. Filtro opcional por ação exata. Consulta gera seu próprio log depois de ler a página; esse novo log aparece em consultas subsequentes. Não retorna entidades de usuários.','parameters':pag_params+[{'name':'acao','in':'query','schema':string(pattern='^[A-Z_]{1,64}$'),'example':'CONSULTA_PEDIDOS'}],'responses':privacy_responses(ref('ListaAuditoria'),['401','403','409','422','500'])}}
paths['/pedidos']['get']['description'] += ' Consultas geram auditoria de ator, escopo e filtros, sem cópia dos pedidos no log.'
complete_contracts(d)
p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

collection = {'info': {'name': 'Raízes do Nordeste — fluxo completo', 'schema': 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json', 'description': 'Execute em ordem após o seed. Tokens e pedidoId são capturados automaticamente. Pagamento simulado.'}, 'variable': [{'key': k, 'value': v} for k,v in [('baseUrl','http://localhost:3000'),('token_cliente',''),('token_cozinha',''),('pedidoId','')]], 'item': []}
def add(name, method, path, code, body=None, token=None, extra=None):
 request = {'method': method, 'url': '{{baseUrl}}'+path, 'header': []}
 if body is not None:
  request['header'].append({'key':'Content-Type','value':'application/json'})
  request['body'] = {'mode':'raw','raw':json.dumps(body,ensure_ascii=False,indent=2),'options':{'raw':{'language':'json'}}}
 if token: request['header'].append({'key':'Authorization','value':'Bearer {{'+token+'}}'})
 collection['item'].append({'name':name,'request':request,'event':[{'listen':'test','script':{'type':'text/javascript','exec':[f'pm.test("HTTP {code}", () => pm.response.to.have.status({code}));']+(extra or [])}}]})
pedido = {'unidadeId':1,'canalPedido':'TOTEM','itens':[{'produtoId':1,'quantidade':1}]}
add('01 Login cliente','POST','/api/auth/login',200,{'email':'maria@cliente.com','senha':'123456'},extra=['pm.collectionVariables.set("token_cliente", pm.response.json().accessToken);'])
add('02 Login cozinha','POST','/api/auth/login',200,{'email':'cozinha@raizes.com','senha':'123456'},extra=['pm.collectionVariables.set("token_cozinha", pm.response.json().accessToken);'])
add('03 Senha incorreta','POST','/api/auth/login',401,{'email':'maria@cliente.com','senha':'errada'})
add('04 Cardápio','GET','/api/produtos',200)
add('05 Criar pedido TOTEM','POST','/api/pedidos',201,pedido,'token_cliente',['pm.collectionVariables.set("pedidoId", pm.response.json().id);'])
add('06 Filtrar pedidos','GET','/api/pedidos?canalPedido=TOTEM&page=1&limit=5',200,token='token_cliente')
add('07 Canal inválido','POST','/api/pedidos',400,{**pedido,'canalPedido':'INVALIDO'},'token_cliente')
add('08 Estoque insuficiente','POST','/api/pedidos',409,{**pedido,'itens':[{'produtoId':1,'quantidade':99999}]},'token_cliente')
add('09 Quantidade inválida','POST','/api/pedidos',422,{**pedido,'itens':[{'produtoId':1,'quantidade':0}]},'token_cliente')
add('10 Pagamento recusado','POST','/api/pedidos/{{pedidoId}}/pagamento',200,{'statusSimulado':'RECUSADO'},'token_cliente',['pm.test("Recusado", () => pm.expect(pm.response.json().sucesso).to.eql(false));'])
add('10a Gateway indisponível','POST','/api/pedidos/{{pedidoId}}/pagamento',503,{'statusSimulado':'INDISPONIVEL'},'token_cliente',['pm.test("Erro gateway", () => pm.expect(pm.response.json().error).to.eql("GATEWAY_INDISPONIVEL"));'])
add('10b Timeout gateway','POST','/api/pedidos/{{pedidoId}}/pagamento',504,{'statusSimulado':'TIMEOUT'},'token_cliente',['pm.test("Erro timeout", () => pm.expect(pm.response.json().error).to.eql("GATEWAY_TIMEOUT"));'])
add('11 Nova tentativa aprovada','POST','/api/pedidos/{{pedidoId}}/pagamento',200,{'statusSimulado':'APROVADO'},'token_cliente',['pm.test("Em preparo", () => pm.expect(pm.response.json().statusPedido).to.eql("EM_PREPARO"));'])
add('12 Cliente sem permissão','PATCH','/api/pedidos/{{pedidoId}}/status',403,{'status':'PRONTO'},'token_cliente')
add('13 Cozinha marca pronto','PATCH','/api/pedidos/{{pedidoId}}/status',200,{'status':'PRONTO'},'token_cozinha')
add('14 Entrega','PATCH','/api/pedidos/{{pedidoId}}/status',200,{'status':'ENTREGUE'},'token_cozinha')
add('15 Pagamento repetido','POST','/api/pedidos/{{pedidoId}}/pagamento',409,{'statusSimulado':'APROVADO'},'token_cliente')
add('16 Sem autenticação','GET','/api/pedidos',401)
add('17 Produtos paginados','GET','/api/produtos?unidadeId=1&page=2&limit=1',200,extra=['pm.test("Página 2", () => pm.expect(pm.response.json().paginacao.pagina).to.eql(2));','pm.test("Um produto", () => pm.expect(pm.response.json().dados.length).to.eql(1));'])
add('18 Unidades paginadas','GET','/api/unidades?page=1&limit=1',200,extra=['pm.test("Uma unidade", () => pm.expect(pm.response.json().dados.length).to.eql(1));'])
collection['variable'].append({'key':'token_admin','value':''})
add('19 Aviso opcional','GET','/api/privacidade/aviso',200,extra=['pm.test("Opcional", () => pm.expect(pm.response.json().opcional).to.eql(true));'])
add('20 Aceitar demonstração','PUT','/api/privacidade/consentimentos/fidelidade',200,{'aceito':True,'versaoAviso':'1.0'},'token_cliente',['pm.test("Aceito", () => pm.expect(pm.response.json().aceito).to.eql(true));'])
add('21 Histórico próprio','GET','/api/privacidade/consentimentos',200,token='token_cliente',extra=['pm.test("Estado ativo", () => pm.expect(pm.response.json().atual.aceito).to.eql(true));'])
add('22 Revogar decisão','PUT','/api/privacidade/consentimentos/fidelidade',200,{'aceito':False},'token_cliente',['pm.test("Revogado", () => pm.expect(pm.response.json().aceito).to.eql(false));'])
add('23 Histórico sem token','GET','/api/privacidade/consentimentos',401)
add('24 Cliente não acessa auditoria','GET','/api/auditoria',403,token='token_cliente')
add('25 Login administrador','POST','/api/auth/login',200,{'email':'admin@raizes.com','senha':'123456'},extra=['pm.collectionVariables.set("token_admin", pm.response.json().accessToken);'])
add('26 Evidência de revogação auditada','GET','/api/auditoria?acao=CONSENTIMENTO_REVOGADO&limit=1',200,token='token_admin',extra=['pm.test("Evento registrado", () => pm.expect(pm.response.json().dados[0].acao).to.eql("CONSENTIMENTO_REVOGADO"));'])
add('27 Canal obrigatório ausente','POST','/api/pedidos',400,{'unidadeId':1,'itens':[{'produtoId':1,'quantidade':1}]},'token_cliente')
add('28 Tipo de quantidade inválido','POST','/api/pedidos',422,{**pedido,'itens':[{'produtoId':1,'quantidade':'1'}]},'token_cliente')
add('29 Unidade inexistente','POST','/api/pedidos',404,{**pedido,'unidadeId':99999},'token_cliente')
add('30 Produto inexistente','POST','/api/pedidos',404,{**pedido,'itens':[{'produtoId':99999,'quantidade':1}]},'token_cliente')
complete_postman(collection,root)
(root/'postman/raizes_nordeste_clean_collection.json').write_text(json.dumps(collection,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Swagger e coleção Postman atualizados.')
