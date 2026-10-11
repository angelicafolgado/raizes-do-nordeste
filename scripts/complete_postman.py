"""Organiza cenários sem alterar sua ordem e gera a matriz a partir da coleção."""
import json
import re

def complete_postman(collection, root):
    cases=collection['item']
    expected_errors={3:'CREDENCIAIS_INVALIDAS',7:'CANAL_INVALIDO',8:'ESTOQUE_INSUFICIENTE',9:'PARAMETRO_INVALIDO',11:'GATEWAY_INDISPONIVEL',12:'GATEWAY_TIMEOUT',14:'ACESSO_NEGADO',17:'ESTADO_INVALIDO',18:'NAO_AUTENTICADO',25:'NAO_AUTENTICADO',26:'ACESSO_NEGADO',29:'CANAL_INVALIDO',30:'PARAMETRO_INVALIDO',31:'UNIDADE_NAO_ENCONTRADA',32:'PRODUTO_NAO_ENCONTRADO'}
    groups=[('01 Auth',1,3),('02 Cardápio',4,4),('03 Pedidos',5,9),('04 Pagamento mock',10,13),('05 Status e duplicidade',14,17),('06 Acesso e paginação',18,20),('07 Privacidade',21,25),('08 Auditoria',26,28),('09 Validações adicionais',29,32)]
    expected_success={1:'accessToken presente; CLIENTE',2:'accessToken presente; COZINHA',4:'dados e paginacao; estoque por unidade',5:'id capturado; AGUARDANDO_PAGAMENTO; total 38,50',6:'canal TOTEM nos resultados',10:'sucesso=false; AGUARDANDO_PAGAMENTO',13:'sucesso=true; EM_PREPARO',15:'status=PRONTO',16:'status=ENTREGUE',19:'pagina=2; um produto',20:'uma unidade',21:'opcional=true; versão 1.0',22:'aceito=true',23:'atual.aceito=true',24:'aceito=false',27:'accessToken presente; ADMIN',28:'evento CONSENTIMENTO_REVOGADO'}
    expectations={
      1:['pm.test("Perfil cliente", () => pm.expect(pm.response.json().usuario.role).to.eql("CLIENTE"));'],
      2:['pm.test("Perfil cozinha", () => pm.expect(pm.response.json().usuario.role).to.eql("COZINHA"));'],
      4:['pm.test("Lista paginada", () => pm.expect(Array.isArray(pm.response.json().dados)).to.eql(true));','pm.test("Total informado", () => pm.expect(typeof pm.response.json().paginacao.total).to.eql("number"));'],
      5:['pm.test("Pedido criado", () => pm.expect(pm.response.json().status).to.eql("AGUARDANDO_PAGAMENTO"));','pm.test("Total calculado", () => pm.expect(pm.response.json().total).to.eql(38.5));'],
      6:['pm.test("Filtro TOTEM", () => pm.expect(pm.response.json().dados.every(p => p.canalPedido === "TOTEM")).to.eql(true));'],
      10:['pm.test("Aguardando após recusa", () => pm.expect(pm.response.json().statusPedido).to.eql("AGUARDANDO_PAGAMENTO"));'],
      15:['pm.test("Pronto", () => pm.expect(pm.response.json().status).to.eql("PRONTO"));'],
      16:['pm.test("Entregue", () => pm.expect(pm.response.json().status).to.eql("ENTREGUE"));'],
      27:['pm.test("Perfil administrador", () => pm.expect(pm.response.json().usuario.role).to.eql("ADMIN"));']
    }
    rows=[]
    for n,item in enumerate(cases,1):
      title=re.sub(r'^\d+[a-z]?\s+', '', item['name'])
      item['name']=f'T{n:02d} - {title}'
      req=item['request']; path=req['url'].replace('{{baseUrl}}','')
      scripts=item['event'][0]['script']['exec']
      code=int(re.search(r'HTTP (\d+)',scripts[0])[1])
      body=json.loads(req['body']['raw']) if 'body' in req else None
      auth=next((h['value'] for h in req['header'] if h['key']=='Authorization'),None)
      pre='API iniciada; migrations e seed aplicados'
      if auth: pre+='; '+auth.replace('Bearer ','')+' obtido no login anterior'
      if '{{pedidoId}}' in path:
        pre+='; pedidoId capturado em T05'
        if n in [10,11,12,13]: pre+='; pedido aguardando pagamento'
        elif n in [14,15]: pre+='; pagamento aprovado em T13'
        elif n==16: pre+='; pedido PRONTO após T15'
        elif n==17: pre+='; pedido já ENTREGUE'
      if n==23: pre+='; aceite registrado em T22'
      if n==28: pre+='; revogação registrada em T24'
      expected=expected_errors.get(n,expected_success.get(n,'Resposta conforme contrato'))
      scripts += expectations.get(n,[])
      if code>=400:
        scripts += ['pm.test("Envelope de erro", () => pm.expect(["error","message","details","timestamp","path","requestId"].every(k => Object.prototype.hasOwnProperty.call(pm.response.json(), k))).to.eql(true));']
        if n in expected_errors: scripts += [f'pm.test("Código de erro", () => pm.expect(pm.response.json().error).to.eql({json.dumps(expected_errors[n])}));']
      if n in (1,2,27): scripts += ['pm.test("Token recebido", () => pm.expect(typeof pm.response.json().accessToken).to.eql("string"));']
      description=f'Pré-condição: {pre}.\nResultado esperado: HTTP {code}; {expected}.\nEvidência: asserções da requisição {item["name"]}, executada em ordem no Collection Runner.'
      req['description']=description
      folder=next(name for name,start,end in groups if start<=n<=end)
      input_value=json.dumps(body,ensure_ascii=False,separators=(',',':')) if body is not None else 'Sem body; parâmetros na rota'
      rows.append(f'| T{n:02d} | {title} | `{req["method"]} {path}` | {pre} | `{input_value}` | {code}; {expected} | {folder}/{item["name"]} |')
    collection['item']=[{'name':name,'description':'Execute as pastas e suas requisições na ordem apresentada.','item':cases[start-1:end]} for name,start,end in groups]
    collection['info']['description']='Execute a coleção completa em ordem, com banco populado. Cada requisição contém pré-condições e asserções de status/conteúdo. baseUrl padrão http://localhost:3000; tokens e pedidoId são capturados automaticamente. Não há token real salvo na coleção.'
    positive=sum(1 for item in cases if int(re.search(r'HTTP (\d+)',item['event'][0]['script']['exec'][0])[1])<400)
    text=f'''# Etapa 4 — Plano de testes da API

## Ambiente e execução

Requisitos: Node.js 20+, dependências instaladas, migrations aplicadas e seed. API padrão: `http://localhost:3000`. Rodar `npm run setup` e `npm run dev`; importar `postman/raizes_nordeste_clean_collection.json` no Postman e executar **a coleção inteira, em ordem**, com uma iteração. As pastas preservam dependências de login, criação, pagamento, status e consentimento.

`baseUrl` é a única variável que precisa de ajuste para outra porta. `token_cliente`, `token_cozinha`, `token_admin` e `pedidoId` são capturados pelas requisições. Todas as contas são fictícias, com senha de demonstração `123456`. Não exporte a coleção com valores reais de tokens preenchidos.

A coleção tem **{len(cases)} cenários**, **{positive} com resposta 2xx** e **{len(cases)-positive} cenários de erro**, em {len(groups)} pastas. Recusa de pagamento é resultado de negócio respondido com HTTP 200 e possui asserção explícita `sucesso=false`.

Para validação automatizada: `npm run build` e `npm test`. A suíte HTTP usa banco SQLite temporário e executa também todas as requisições e asserções desta coleção. A execução automatizada reproduz os recursos `pm` usados aqui; não é uma alegação de execução pela interface do aplicativo Postman. O uso manual no Postman é reproduzível com o arquivo entregue.

A coleção manual usa a base da API em execução e consome uma unidade do produto 1 por execução bem-sucedida. O seed preserva saldo e pedidos; ele não repõe estoque. Use uma base separada nova para avaliações repetidas, sem apagar a base existente. IDs 99999 usados nos casos 404 pressupõem o conjunto do seed, sem recursos com esses IDs.

## Matriz de cenários e evidências

| ID | Cenário | Endpoint e método | Pré-condição | Entrada | Saída esperada | Evidência na coleção |
|---|---|---|---|---|---|---|
'''
    text+='\n'.join(rows)+'''

## Cobertura e rastreabilidade

| Requisito | Cenários principais |
|---|---|
| RF01–RF02: autenticação e autorização | T01–T03, T14, T18, T25–T27 |
| RF04–RF05/RF16: unidades, cardápio e paginação | T04, T19–T20 |
| RF06–RF08: pedido, canal, estoque e validação | T05–T09, T29–T32 |
| RF09: consulta por canal | T06; isolamento por cliente nos testes automatizados |
| RF10–RF11: gateway mock e falhas | T10–T13, T17 |
| RF12: transições operacionais | T14–T16 |
| RF13: cancelamento e devolução | Teste automatizado de cancelamento, devolução única e bloqueio de pagamento |
| RF14: auditoria | T28 comprova evento persistido; testes automatizados verificam ator, filtros e consultas |
| RF15: consentimento | T21–T25; testes automatizados verificam histórico, isolamento, revogação e rollback |
| RNF05/RNF08: erros e evidência executável | Todos os cenários de erro verificam status, envelope e código de erro |

## Testes automatizados complementares

`tests/api.test.ts` cobre itens repetidos, rollback de reserva, estoque de outra unidade, IDs parcialmente numéricos, paginação inválida, cliente acessando pedido alheio, cancelamento, seed idempotente, concorrência na última unidade e no pagamento, contrato enviado ao gateway, falha de correlação e preservação de pagamento anterior.

Também cobre flag legada ignorada, versão inválida de aviso, tentativa de definir identidade pelo body, histórico próprio, revogação sem bloquear pedido, permissões de auditoria, projeção segura de logs, rollback em falha de auditoria e erro interno sem exceção/query sensível. Os artefatos OpenAPI e Postman têm verificação de contratos e exemplos.

## Critério de conclusão

Build e suíte devem terminar com código zero, sem testes falhando; coleção precisa manter os cenários acima e executar suas asserções; exemplos OpenAPI devem corresponder aos schemas. Não considerar prints ou contagens isoladas como substitutos da evidência executável. A execução final e suas contagens estão registradas em `docs/04_RESULTADO_VALIDACAO.md`.
'''
    (root/'docs/04_PLANO_DE_TESTES.md').write_text(text,encoding='utf-8')
