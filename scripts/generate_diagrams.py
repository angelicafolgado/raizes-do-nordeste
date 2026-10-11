"""Gera fontes editáveis dos diagramas a partir do schema/código local."""
from pathlib import Path
import re, json, html

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/diagramas'
OUT.mkdir(parents=True, exist_ok=True)
schema = (ROOT/'prisma/schema.prisma').read_text(encoding='utf-8')
models = dict(re.findall(r'model (\w+) \{(.*?)\n\}', schema, re.S))
scalar = {'Int','String','Float','Boolean','DateTime'}
fields = {name: [(field, kind, rest.strip()) for field,kind,rest in re.findall(r'^\s*(\w+)\s+(\w+\??)([^\n]*)',body,re.M) if kind.rstrip('?') in scalar] for name,body in models.items()}
relations = []
for child,body in models.items():
    for parent,optional,fk,pk in re.findall(r'^\s*\w+\s+(\w+)(\?)?\s+@relation\(fields: \[(\w+)\], references: \[(\w+)\]',body,re.M):
        unique=any(field==fk and '@unique' in rest for field,_,rest in fields[child])
        relations.append((parent,child,fk,'0..1' if optional else '1','0..1' if unique else '0..*'))
assert len(models)==9 and len(relations)==9,(len(models),len(relations))

def q(value): return json.dumps(value,ensure_ascii=False)
def graph(title,subtitle,rankdir='LR'):
    return ['digraph G {',f'graph [rankdir={rankdir}, bgcolor="white", pad="0.4", nodesep="0.45", ranksep="1.0", splines=polyline, outputorder=edgesfirst, fontname="Arial", labelloc=t, labeljust=l, label=<<FONT POINT-SIZE="24" COLOR="#123447"><B>{html.escape(title)}</B></FONT><BR/><FONT POINT-SIZE="12" COLOR="#46616f">{html.escape(subtitle)}</FONT><BR/><BR/>>];',
      'node [shape=plain, fontname="Arial", fontsize=12];',
      'edge [fontname="Arial", fontsize=11, color="#71858e", fontcolor="#24475a", penwidth=1.2, arrowsize=0.7];']
def table(name,header,rows,footer=None):
    cells=[f'<TR><TD ALIGN="LEFT" BGCOLOR="#163e50"><FONT COLOR="white" POINT-SIZE="15"><B>{html.escape(header)}</B></FONT></TD></TR>']
    cells += [f'<TR><TD ALIGN="LEFT" BGCOLOR="{("#edf6f5" if i%2==0 else "#ffffff")}">{html.escape(row)}</TD></TR>' for i,row in enumerate(rows)]
    if footer: cells.append('<TR><TD ALIGN="LEFT" BGCOLOR="#fff3d9"><FONT POINT-SIZE="10">'+html.escape(footer)+'</FONT></TD></TR>')
    return f'{name} [label=<<TABLE BORDER="1" COLOR="#92acb6" CELLBORDER="0" CELLSPACING="0" CELLPADDING="6">'+''.join(cells)+'</TABLE>>];'

der=graph('01 | Modelo de dados — DER','SQLite / Prisma 5 • cardinalidades da persistência • PK: primária · FK: estrangeira · UQ: única')
for name,body in models.items():
    rows=[]
    fks=[r[2] for r in relations if r[1]==name]
    for field,kind,rest in fields[name]:
        tags=[]
        if '@id' in rest: tags.append('PK')
        if field in fks: tags.append('FK')
        if '@unique' in rest: tags.append('UQ')
        rows.append(f'{field}: {kind}'+('  ['+', '.join(tags)+']' if tags else ''))
    table_name=re.search(r'@@map\("([^"]+)"\)',body)[1]
    footer=table_name
    if name=='EstoqueUnidade': footer+=' | UQ(unidadeId, produtoId)'
    if name=='Consentimento': footer+=' | índice(usuarioId, finalidade, id)'
    if name=='Usuario': footer+=' | consentimentoLGPD: legado, não autoriza'
    der.append(table(name,name,rows,footer))
for parent,child,fk,left,right in relations:
    der.append(f'{parent} -> {child} [dir=none, taillabel={q(left)}, headlabel={q(right)}, label={q(fk)}, labeldistance=1.9];')
der+=['{rank=same; Usuario; Unidade; Produto;}','{rank=same; Pedido; EstoqueUnidade; Consentimento; AuditLog;}','{rank=same; ItemPedido; Pagamento;}','}']
(OUT/'01_der.dot').write_text('\n'.join(der),encoding='utf-8')

# UML de domínio: interfaces reais, sem métodos de negócio inventados nas entidades.
interfaces={}
for file in (ROOT/'src/domain/entities').glob('*.ts'):
    text=file.read_text(encoding='utf-8')
    for name,body in re.findall(r'export interface (\w+)\s*\{(.*?)\}',text,re.S):
        interfaces[name]=[f'{a}: {b.strip()}' for a,b in re.findall(r'(\w+)\s*:\s*([^;]+)',body)]
assert len(interfaces)==9
classes=graph('03 | Classes — visão de domínio','«interface» = contrato TypeScript • relações por IDs; Pedido contém itens • regras de negócio residem nos casos de uso')
for name,rows in interfaces.items():
    classes.append(table(name,'«interface» '+name,rows))
for parent,child,fk,left,right in relations:
    child='RegistroAuditoria' if child=='AuditLog' else child
    if child=='ItemPedido' and parent=='Pedido':
        classes.append('Pedido -> ItemPedido [dir=both, arrowtail=diamond, arrowhead=none, taillabel="1", headlabel="1..*", label="itens", labeldistance=1.9];')
    else:
        classes.append(f'{parent} -> {child} [dir=none, taillabel={q(left)}, headlabel={q(right)}, label={q(fk)}, labeldistance=1.9];')
classes+=['{rank=same; Usuario; Unidade; Produto;}','{rank=same; Pedido; EstoqueUnidade; Consentimento; RegistroAuditoria;}','{rank=same; ItemPedido; Pagamento;}','}']
(OUT/'03_classes.dot').write_text('\n'.join(classes),encoding='utf-8')

uc=graph('02 | Casos de uso — API implementada','Atores em notação retangular UML • generalização = seta vazada para Usuário • associações indicam participação')
actors={'usuario':'«actor»\nUsuário da rede','cliente':'«actor»\nCliente\nApp / Web / Totem','atendente':'«actor»\nAtendente\nBalcão','cozinha':'«actor»\nCozinha','admin':'«actor»\nGerente / Administrador\nperfil ADMIN','gateway':'«actor»\nGateway de pagamento\nsimulado por adaptador local'}
for key,label in actors.items():
    uc.append(f'{key} [shape=box, style="rounded,filled", fillcolor="#e8f1f5", color="#406579", margin="0.16", label={q(label)}];')
uc.append('subgraph cluster_api { label="Raízes do Nordeste — fronteira da API"; color="#a4bbc5"; style=rounded; fontcolor="#123447"; fontsize=16;')
usecases={'login':'UC01\nAutenticar usuário','catalogo':'UC02\nConsultar unidades,\ncardápio e aviso público','historico':'UC03\nConsultar pedidos\n(próprios ou da rede)','consentir':'UC04\nRegistrar / revogar decisão\ne consultar próprio histórico','criar':'UC05\nCriar pedido multicanal','reservar':'UC06\nValidar itens e\nreservar estoque','pagar':'UC07\nSolicitar pagamento mock','status':'UC08\nAvançar preparo / entrega','cancelar':'UC09\nCancelar pedido não pago','auditoria':'UC10\nConsultar auditoria'}
for key,label in usecases.items():
    uc.append(f'{key} [shape=ellipse, style=filled, fillcolor="#f1f8f6", color="#43857e", margin="0.16", label={q(label)}];')
uc.append('}')
for child in ['cliente','atendente','cozinha','admin']:
    uc.append(f'{child} -> usuario [arrowhead=empty, color="#8a9ba3"];')
for target in ['login','catalogo','historico','consentir']:
    uc.append(f'usuario -> {target} [dir=none];')
for actor in ['cliente','atendente','admin']:
    for target in ['criar','pagar']: uc.append(f'{actor} -> {target} [dir=none];')
for actor in ['cozinha','admin']:
    for target in ['status','cancelar']: uc.append(f'{actor} -> {target} [dir=none];')
uc+=['admin -> auditoria [dir=none];','criar -> reservar [style=dashed, label="«include»", arrowhead=open, color="#238577"];','pagar -> gateway [dir=none];','{rank=same; cliente; atendente; cozinha; admin;}','}']
(OUT/'02_casos_de_uso.dot').write_text('\n'.join(uc),encoding='utf-8')

# Sequência vetorial com posições explícitas para legibilidade em página larga.
W=1840; H=2640
parts=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
'<defs><marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="5" orient="auto"><path d="M0,0 L10,5 L0,10" fill="none" stroke="#24475a" stroke-width="1.5"/></marker></defs>',
'<rect width="100%" height="100%" fill="white"/>']
def text(x,y,lines,size=17,color='#24475a',anchor='middle',bold=False):
    for i,line in enumerate(lines if isinstance(lines,list) else [lines]):
        parts.append(f'<text x="{x}" y="{y+i*(size+5)}" font-family="Arial, sans-serif" font-size="{size}" fill="{color}" text-anchor="{anchor}" font-weight="{700 if bold else 400}">{html.escape(line)}</text>')
def rect(x,y,w,h,fill='#edf6f5',stroke='#96b1bb'):
    parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{fill}" stroke="{stroke}"/>')
text(40,48,'04 | Sequência — Pedido → Pagamento mock → Status',30,anchor='start',bold=True)
text(40,82,'Mensagens agrupadas por responsabilidade; transações e alternativas refletem o código atual.',18,anchor='start')
xs={'cliente':160,'api':440,'app':750,'store':1080,'gateway':1410,'cozinha':1700}
labels={'cliente':['Cliente / Atendente'],'api':['API / JWT'],'app':['Casos de uso'],'store':['Store / Prisma / SQLite'],'gateway':['MockPaymentGateway'],'cozinha':['Cozinha / ADMIN']}
for actor,x in xs.items():
    rect(x-125,110,250,55,'#163e50','#163e50');text(x,144,labels[actor],17,'white',bold=True)
    parts.append(f'<line x1="{x}" x2="{x}" y1="165" y2="2550" stroke="#a6bac2" stroke-dasharray="6 7"/>')
def band(y,label):
    rect(35,y,W-70,38,'#dbeeed','#b1ccca');text(52,y+26,label,18,anchor='start',bold=True)
def message(a,b,y,lines,dashed=False):
    x1,x2=xs[a],xs[b]
    direction=1 if x2>x1 else -1
    parts.append(f'<path d="M{x2-direction*11},{y-6} L{x2},{y} L{x2-direction*11},{y+6}" fill="none" stroke="#24475a" stroke-width="1.6"/>')
    parts.append(f'<line x1="{x1}" y1="{y}" x2="{x2}" y2="{y}" stroke="#24475a" stroke-width="1.6" marker-end="url(#arrow)"'+(' stroke-dasharray="7 5"' if dashed else '')+'/>')
    lines=lines if isinstance(lines,list) else [lines]
    width=min(abs(x2-x1)-12,max(len(line) for line in lines)*8.6+16)
    x=(x1+x2)/2
    rect(x-width/2,y-24-len(lines)*22,width,len(lines)*22+10,'white','white')
    text(x,y-14-(len(lines)-1)*22,lines,16)
def note(y,lines,h=55):
    rect(55,y,W-110,h,'#fff4dd','#d9bd7d');text(73,y+23,lines,16,anchor='start')
band(190,'1. Criar pedido — autenticação e autorização antes do caso de uso')
message('cliente','api',295,['POST /pedidos + JWT','unidade, canal, itens'])
message('api','app',365,['Identidade do token','+ DTO validado'])
message('app','store',445,['BEGIN; unidade e produtos;','reserva condicional do saldo'])
message('app','store',525,['Criar Pedido + ItemPedido;','auditar CRIACAO_PEDIDO'])
message('store','app',595,['COMMIT; pedido','AGUARDANDO_PAGAMENTO'],True)
message('app','api',660,'Pedido persistido',True)
message('api','cliente',725,'201 + pedido',True)
note(747,['Item inválido / unidade ou produto ausente / estoque insuficiente: erro 400/422, 404 ou 409; rollback integral.'])

band(835,'2. Solicitar pagamento — uma chamada; alternativas mutuamente exclusivas')
message('cliente','api',940,['POST /pedidos/{id}/pagamento','JWT + statusSimulado'])
message('api','app',1005,['Executar pagamento','com identidade autenticada'])
message('app','store',1080,['BEGIN; ler pedido;','validar titular e estado'])
message('store','app',1145,'Pedido aguardando pagamento',True)
message('app','gateway',1220,['Solicitação: pedidoId, valorCentavos, BRL,','correlacaoId e cenário simulado'])
message('gateway','app',1290,['Resultado + pedidoId + correlação;','transacaoId em aprovação / recusa'],True)
note(1310,['Correlação incompatível: rollback e 502. Token/perfil/titular/estado inválido: rejeição antes de solicitar ao gateway.'])

rect(55,1395,W-110,420,'none','#43857e');text(75,1425,'alt',18,anchor='start',bold=True)
text(130,1425,'[APROVADO ou RECUSADO]',18,anchor='start',bold=True)
message('app','store',1510,['Status condicional; upsert pagamento;','auditar PAGAMENTO_MOCK; COMMIT'])
message('store','app',1575,'Pagamento e estado persistidos',True)
message('app','api',1640,'Resultado do caso de uso',True)
message('api','cliente',1705,['200 + sucesso, transacaoId,','statusPedido e detalhes'],True)
note(1730,['APROVADO → EM_PREPARO. RECUSADO → AGUARDANDO_PAGAMENTO, mantendo reserva e permitindo nova tentativa.'])

rect(55,1840,W-110,250,'none','#43857e');text(75,1870,'else',18,anchor='start',bold=True)
text(135,1870,'[INDISPONIVEL ou TIMEOUT]',18,anchor='start',bold=True)
message('app','store',1950,['Auditar falha; COMMIT; preservar','pedido, pagamento e estoque'])
message('app','api',2010,'Erro após commit da auditoria',True)
message('api','cliente',2070,'503 / 504 + erro padronizado',True)

band(2130,'3. Operação — após aprovação, chamadas separadas para cada transição')
message('cozinha','api',2230,['PATCH /pedidos/{id}/status + JWT: PRONTO; depois ENTREGUE'])
message('api','app',2290,'Validar perfil e transição')
message('app','store',2350,['BEGIN; status + auditoria;','COMMIT'])
message('store','app',2410,'Pedido atualizado',True)
message('app','api',2470,'Pedido',True)
message('api','cozinha',2530,'200 + pedido',True)
text(40,2580,'Cancelamento: somente AGUARDANDO_PAGAMENTO → CANCELADO, com devolução única de estoque e auditoria na mesma transação.',16,anchor='start')
text(40,2610,'O gateway é local e imediato: TIMEOUT é um resultado simulado, sem espera de rede ou cobrança real.',16,anchor='start')
parts.append('</svg>')
(OUT/'04_sequencia.svg').write_text('\n'.join(parts),encoding='utf-8')

mermaid='''sequenceDiagram
    actor C as Cliente ou Atendente
    participant API as API/JWT
    participant A as Casos de uso
    participant DB as Store/Prisma/SQLite
    participant G as MockPaymentGateway
    actor K as Cozinha ou ADMIN
    C->>API: POST /pedidos + JWT (unidade, canal, itens)
    API->>A: CriarPedidoUseCase.executar(identidade, DTO)
    A->>DB: BEGIN; validar unidade/produtos; reservar saldo
    A->>DB: Criar pedido/itens e auditar
    DB-->>A: COMMIT; AGUARDANDO_PAGAMENTO
    A-->>API: Pedido persistido
    API-->>C: 201 + pedido
    Note over A,DB: Falha em item/estoque reverte toda a transação
    C->>API: POST /pedidos/{id}/pagamento + statusSimulado
    API->>A: ProcessarPagamentoMockUseCase.executar
    A->>DB: BEGIN; ler pedido; validar titular e estado
    DB-->>A: Pedido aguardando pagamento
    A->>G: pedidoId, valorCentavos, BRL, correlacaoId, cenario
    G-->>A: Resultado correlacionado (transacaoId em aprovação/recusa)
    Note over A,G: Correlação inválida: rollback e 502
    alt APROVADO ou RECUSADO
        A->>DB: Status condicional + upsert pagamento + auditoria
        DB-->>A: COMMIT
        A-->>API: Resultado
        API-->>C: 200; aprovado=EM_PREPARO, recusado=AGUARDANDO_PAGAMENTO
    else INDISPONIVEL ou TIMEOUT
        A->>DB: Auditoria da falha; COMMIT sem mudar pedido/estoque
        A-->>API: Erro após commit
        API-->>C: 503 ou 504 + erro padronizado
    end
    K->>API: PATCH status PRONTO; depois ENTREGUE (chamadas separadas)
    API->>A: AtualizarStatusPedidoUseCase.executar
    A->>DB: BEGIN; mudar estado + auditoria; COMMIT
    DB-->>A: Pedido atualizado
    A-->>API: Pedido
    API-->>K: 200 + pedido
    Note over A,DB: Cancelar apenas antes do pagamento; devolver saldo uma vez
'''
(OUT/'04_sequencia.mmd').write_text(mermaid,encoding='utf-8')
print('Fontes geradas: DER, casos de uso, classes e sequência.')
