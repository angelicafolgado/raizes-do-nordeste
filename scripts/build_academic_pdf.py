"""Compila o relatório acadêmico a partir dos artefatos locais revisados."""
from pathlib import Path
import re, html, json
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, Image, Preformatted
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus.tableofcontents import TableOfContents
from PIL import Image as PILImage

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/pdf'; OUT.mkdir(parents=True,exist_ok=True)
TMP=ROOT/'tmp/pdfs'; TMP.mkdir(parents=True,exist_ok=True)
for name,file in [('Arial','arial.ttf'),('Arial-Bold','arialbd.ttf')]:
    pdfmetrics.registerFont(TTFont(name,str(Path('C:/Windows/Fonts')/file)))
pdfmetrics.registerFontFamily('Arial',normal='Arial',bold='Arial-Bold',italic='Arial',boldItalic='Arial-Bold')
styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='BodyA',fontName='Arial',fontSize=10.5,leading=15,spaceAfter=8))
styles.add(ParagraphStyle(name='CellA',fontName='Arial',fontSize=8,leading=11,spaceAfter=3,wordWrap='CJK'))
styles.add(ParagraphStyle(name='TitleA',fontName='Arial-Bold',fontSize=21,leading=28,spaceAfter=20,textColor=colors.HexColor('#163e50')))
styles.add(ParagraphStyle(name='HeadingA',fontName='Arial-Bold',fontSize=15,leading=20,spaceAfter=14,keepWithNext=True))
styles.add(ParagraphStyle(name='SubA',fontName='Arial-Bold',fontSize=11,leading=16,spaceBefore=10,spaceAfter=7,keepWithNext=True))
styles.add(ParagraphStyle(name='CodeA',fontName='Courier',fontSize=7.5,leading=10,spaceAfter=9))

def read(name): return (ROOT/'docs'/name).read_text(encoding='utf-8')
def section(s,start,end=None):
    v=s[s.index(start):]
    return v[:v.index(end)] if end and end in v else v
def clean(s):
    return s.replace('—','-').replace('–','-').replace('\u2011','-')
def inline(s):
    s=re.sub(r'\[([^\]]+)\]\(([^)]+)\)',r'\1',s)
    s=html.escape(clean(s))
    s=re.sub(r'\*\*(.+?)\*\*',r'<b>\1</b>',s)
    return s.replace('`','')

story=[]; sources=[]
def para(s,style='BodyA'): story.append(Paragraph(inline(s),styles[style]))
def heading(s): story.append(Paragraph(inline(s),styles['HeadingA']))
def markdown(s):
    lines=s.strip().splitlines(); i=0
    while i<len(lines):
        line=lines[i].strip()
        if not line: i+=1; continue
        if line.startswith('```'):
            code=[];i+=1
            while i<len(lines) and not lines[i].startswith('```'): code.append(clean(lines[i]));i+=1
            # Wrap long command/JSON lines to the printable area.
            import textwrap
            story.append(Preformatted('\n'.join(part for x in code for part in (textwrap.wrap(x,100,replace_whitespace=False) or [''])),styles['CodeA']))
        elif line.startswith('|'):
            rows=[]
            while i<len(lines) and lines[i].strip().startswith('|'):
                row=[c.strip() for c in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r'[: -]+',c) for c in row): rows.append(row)
                i+=1
            n=max(map(len,rows)); widths=[475/n]*n
            if n==4: widths=[58,125,130,162]
            if n==5: widths=[35,90,95,120,135]
            cells=[[Paragraph(inline(c),styles['CellA']) for c in row] for row in rows]
            t=Table(cells,colWidths=widths,repeatRows=1,hAlign='LEFT')
            t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#dcebea')),('VALIGN',(0,0),(-1,-1),'TOP'),('GRID',(0,0),(-1,-1),.3,colors.HexColor('#afc1c7')),('LEFTPADDING',(0,0),(-1,-1),5),('RIGHTPADDING',(0,0),(-1,-1),5),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5)]))
            story.extend([t,Spacer(1,12)]);continue
        elif line.startswith('#'): para(re.sub(r'^#+\s*','',line),'SubA')
        elif line.startswith('!['): pass
        else: para(line)
        i+=1

def chapter(title,body):
    story.append(PageBreak()); heading(title); markdown(body)
    sources.append('# '+title+'\n\n'+body)

class Doc(SimpleDocTemplate):
    def afterFlowable(self,flow):
        if isinstance(flow,Paragraph) and flow.style.name=='HeadingA':
            self.notify('TOCEntry',(0,flow.getPlainText(),self.page))
def page(canvas,doc):
    if doc.page>1:
        canvas.setFont('Arial',8);canvas.setFillColor(colors.HexColor('#52636a'))
        canvas.drawString(60,812,'RAÍZES DO NORDESTE | PROJETO BACK-END')
        canvas.drawString(60,28,'Angelica Dias Folgado | RU 4937552')
        canvas.drawRightString(535,28,str(doc.page))

story.append(Spacer(1,70));para('CENTRO UNIVERSITÁRIO INTERNACIONAL UNINTER','HeadingA')
story.append(Spacer(1,65));para('Angelica Dias Folgado','TitleA');para('RU: 4937552')
story.append(Spacer(1,70));para('RAÍZES DO NORDESTE','TitleA');para('Projeto de API REST para pedidos multicanal, estoque por unidade e pagamento simulado','HeadingA')
story.append(Spacer(1,75));para('Projeto Multidisciplinar - Trilha Back-End');para('2026')
chapter('Apresentação e identificação', '''Relatório acadêmico de Angelica Dias Folgado, RU 4937552. O projeto implementa uma API REST para a rede fictícia Raízes do Nordeste, com foco no fluxo de pedido, pagamento mock e acompanhamento operacional.

esta versão reúne o código e as evidências locais disponíveis em 11/10/2026. O roteiro fornecido com o projeto foi adotado como referência oficial desta entrega por orientação do aluno. Curso, polo e cidade não foram informados e não foram presumidos. A instalação em cópia limpa foi validada. O conteúdo final está consolidado em um único commit de Angelica Dias Folgado, pronto para substituir a história anterior no repositório público.

O material distingue funcionalidades implementadas de propostas futuras. Os resultados de desempenho se limitam ao ambiente local e não caracterizam garantia de produção.''')
story.append(PageBreak());para('Sumário','TitleA');toc=TableOfContents();toc.levelStyles=[ParagraphStyle(name='TOCA',fontName='Arial',fontSize=10,leading=16,spaceBefore=4)];story.append(toc)
chapter('1. Introdução, contexto e objetivos', '''Uma rede de alimentação com matriz e unidades precisa integrar pedidos recebidos por diferentes canais, disponibilidade local de produtos e acompanhamento do preparo. Registros inconsistentes podem gerar venda sem saldo e alterações indevidas de status.

O objetivo é demonstrar um backend com autenticação, autorização por perfil, persistência real, reserva transacional e integração simulada de pagamento. Os canais APP, WEB, TOTEM, BALCAO e PICKUP são registrados no domínio e podem ser usados para filtrar pedidos.

O MVP escolhido é o Fluxo A: Pedido → Pagamento mock → Status. Essa escolha permite validar uma operação completa sem depender de adquirentes, dados de cartão ou serviços financeiros reais. TypeScript, Express, Prisma e SQLite viabilizam uma demonstração local reproduzível.

ADMIN representa a administração da rede. Não há entidade Matriz nem vínculo obrigatório de funcionários a uma unidade; os perfis operacionais consultam a rede. Restrição operacional por unidade é uma evolução futura.''')
req=read('01_REQUISITOS_E_ESCOPO.md')
rf=section(req,'## 4. Requisitos funcionais','## 6.')
rf=rf.replace('P1 / Parcial','P1 / Implementado').replace('P1 / Planejado','P1 / Implementado').replace('P2 / Planejado','P2 / Implementado').replace('P2 / Parcial','P2 / Verificado localmente')
rf=rf.replace('Meta ainda não medida','Meta atingida no ensaio local posterior').replace('Paginação será completada pelo RF16.','Paginação implementada pelo RF16.')
chapter('2. Requisitos e priorização', 'P1 identifica requisitos essenciais; P2, qualidade e consistência; P3, evolução conceitual. Os critérios abaixo especificam o comportamento esperado. O estado final e suas evidências são detalhados na seção de rastreabilidade.\n\n'+rf)
chapter('3. Propostas fora do MVP',section(req,'## 7.','## 8.'))
manual=read('06_DOCUMENTACAO_TECNICA.md')
chapter('4. Arquitetura e decisões técnicas',section(manual,'## Arquitetura e decisões','## Funcionalidade crítica'))
chapter('5. Modelagem do sistema','Os diagramas representam a implementação atual. Fontes: schema Prisma, interfaces de domínio, rotas e casos de uso do projeto. Elaboração a partir dos artefatos técnicos locais, 2026.\n\nO DER utiliza cardinalidades de persistência; a aplicação exige pelo menos um item por pedido. As classes são interfaces TypeScript, com comportamento nos casos de uso. O gateway aparece como participante de apoio, mas é executado localmente como mock.')
for name,title in [('01_der','Figura 1 - Modelo de dados (DER)'),('02_casos_de_uso','Figura 2 - Casos de uso'),('03_classes','Figura 3 - Classes de domínio')]:
    story.append(PageBreak());para(title,'SubA')
    file=ROOT/'docs/diagramas'/f'{name}.png'; im=PILImage.open(file); w,h=im.size;scale=min(475/w,660/h)
    story.append(Image(str(file),width=w*scale,height=h*scale));para('Fonte: artefatos do projeto, 2026.','CellA')
im=PILImage.open(ROOT/'docs/diagramas/04_sequencia.png')
for i,(top,bottom) in enumerate([(0,2100),(2100,im.height)]):
    file=TMP/f'sequence-{i}.png'; crop=im.crop((0,top,im.width,bottom));crop.save(file)
    story.append(PageBreak());para(f'Figura 4 - Sequência do fluxo crítico (parte {i+1}/2)','SubA')
    if i: para('Participantes: Cliente/Atendente, API/JWT, Casos de uso, Store/Prisma/SQLite, MockPaymentGateway e Cozinha/ADMIN.','CellA')
    w,h=crop.size;scale=min(475/w,640/h);story.append(Image(str(file),width=w*scale,height=h*scale))
    para('Fonte: artefatos do projeto, 2026. Partes consecutivas do mesmo diagrama.','CellA')
chapter('6. Funcionalidade crítica',section(manual,'## Funcionalidade crítica','## Segurança,'))
swagger=json.loads((ROOT/'src/api/docs/swagger.json').read_text(encoding='utf-8'))
chapter('7. Contratos da API','Base local: http://localhost:3000/api. Swagger interativo: http://localhost:3000/api-docs. O contrato completo possui 12 operações e 81 exemplos explícitos. Os exemplos abaixo são fictícios, extraídos do contrato do projeto; tokens ilustrativos não são credenciais válidas.')
for path,methods in swagger['paths'].items():
    for method,op in methods.items():
        if method not in ['get','post','put','patch','delete']:continue
        para(method.upper()+' '+path,'SubA');para(op.get('summary',''))
        if op.get('description'):para(op['description'])
        para('Autenticação: '+('JWT Bearer; permissões conforme descrição da operação.' if op.get('security') else 'Pública.'))
        for param in op.get('parameters',[]):
            if '$ref' in param: param=swagger['components']['parameters'][param['$ref'].split('/')[-1]]
            para(f"Parâmetro {param.get('name')} ({param.get('in')}): {param.get('description','')} ")
        def example(content):
            media=content.get('application/json',{})
            if 'example' in media:return media['example']
            examples=media.get('examples',{})
            return next(iter(examples.values())).get('value') if examples else None
        body=example(op.get('requestBody',{}).get('content',{}))
        if body is not None: markdown('Request (exemplo):\n```json\n'+json.dumps(body,ensure_ascii=False,indent=2)+'\n```')
        para('Respostas documentadas: '+', '.join(op.get('responses',{})))
        for status,res in op.get('responses',{}).items():
            if str(status).startswith('2'):
                ex=example(res.get('content',{}))
                if ex is not None:markdown('Response '+status+' (exemplo):\n```json\n'+json.dumps(ex,ensure_ascii=False,indent=2)+'\n```')
                break
markdown('### Envelope de erro\n```json\n'+json.dumps({'error':'ESTOQUE_INSUFICIENTE','message':'Estoque insuficiente para o produto solicitado.','details':[],'timestamp':'2026-10-11T12:00:00.000Z','path':'/api/pedidos','requestId':'exemplo-ilustrativo'},ensure_ascii=False,indent=2)+'\n```\nExemplo ilustrativo de estrutura. HTTP 400/422 indica entrada inválida; 401/403, autenticação/autorização; 404, recurso ausente; 409, conflito; 502/503/504, falha de integração simulada.')
chapter('8. Segurança, privacidade e auditoria',read('03_PRIVACIDADE_E_SEGURANCA.md').split('\n',1)[1])
chapter('9. Plano de testes',read('04_PLANO_DE_TESTES.md').split('\n',1)[1])
chapter('10. Resultados e desempenho',read('06_CORRECAO_DESEMPENHO.md').split('\n',1)[1])
chapter('11. Instalação e recuperação',section(manual,'## Instalação e configuração','## Arquitetura')+'\n'+section(manual,'## Backup, restauração','## Validação'))
chapter('12. Rastreabilidade',section(read('06_RASTREABILIDADE.md'),'## Requisitos funcionais','## Entregáveis'))
chapter('13. Conclusão e situação da entrega', '''O projeto fecha o fluxo de pedido, pagamento simulado e atualização de status, com persistência, estoque por unidade e controle de acesso. A separação em camadas permite manter regras independentes do transporte HTTP e da implementação concreta do gateway.

A validação funcional reuniu 37 testes aprovados e 32 cenários Postman. No ensaio corrigido de consultas públicas, 20 clientes durante 60 segundos produziram 48.486 consultas, sem erros, com p95 de 39,37 ms. A mudança do cliente de carga também contribui para a diferença metodológica; não se atribui todo o ganho exclusivamente à API.

O modelo permanece demonstrativo: não há pagamento real, cadastro público, pontos/resgate, campanhas, expurgo automático nem garantia de disponibilidade contínua. Essas extensões foram identificadas e especificadas conceitualmente.

Este PDF constitui a versão acadêmica local. O roteiro fornecido com o projeto foi adotado como referência oficial desta entrega por orientação do aluno. A instalação foi verificada em uma cópia limpa: dependências pelo lockfile congelado, geração do Prisma, migrações e seed em banco novo, compilação TypeScript e aprovação dos 37 testes. O repositório está em https://github.com/angelicafolgado/raizes-do-nordeste. O conteúdo final foi consolidado em um único commit de Angelica Dias Folgado; o push que substitui a história antiga ainda está pendente.''')
chapter('14. Referências e artefatos consultados','''UNINTER. Roteiro de Atividade Prática de Projeto Multidisciplinar - Trilha Back-End. Arquivo fornecido pelo aluno e adotado como referência oficial desta entrega por orientação do aluno. Arquivo local: UNINTER_Roteiro_Back_End_Copia_Adaptada_Estudo.pdf. Consultado em 11 out. 2026.

RAÍZES DO NORDESTE. Código-fonte da API REST em TypeScript. Artefatos locais: src/, prisma/schema.prisma, prisma/migrations/ e tests/api.test.ts. Versão de trabalho consultada em 11 out. 2026.

RAÍZES DO NORDESTE. Documentação técnica do projeto. README.md e docs/01 a docs/06: requisitos, complementos, privacidade, plano de testes, diagramas, manual técnico e rastreabilidade. 2026.

RAÍZES DO NORDESTE. Contrato OpenAPI e coleção de testes. src/api/docs/swagger.json e postman/raizes_nordeste_clean_collection.json. 2026.

RAÍZES DO NORDESTE. Evidências operacionais. docs/06_RESULTADOS_OPERACIONAIS.json, relatório anterior e relatório do cliente sem reuso de conexões. 11 out. 2026.

Os diagramas foram gerados a partir do código e do schema local. As fontes editáveis DOT/Mermaid e as imagens PNG/SVG acompanham a pasta docs/diagramas/.''')
pdf=OUT/'4937552_Projeto_Back_End.pdf'
doc=Doc(str(pdf),pagesize=(595.28,841.89),rightMargin=60,leftMargin=60,topMargin=55,bottomMargin=55,title='Raízes do Nordeste - Projeto Back-End',author='Angelica Dias Folgado')
doc.multiBuild(story,onFirstPage=page,onLaterPages=page)
(OUT/'4937552_Projeto_Back_End_texto.md').write_text('# Angelica Dias Folgado\n\nRU: 4937552\n\n'+'\n\n'.join(sources),encoding='utf-8')
print(pdf)
