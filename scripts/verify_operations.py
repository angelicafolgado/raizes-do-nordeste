"""Verifica RNF09/RNF10 em banco isolado, sem usar prisma/dev.db."""
from pathlib import Path
import concurrent.futures, json, os, platform, secrets, shutil, socket
import sqlite3, subprocess, time, urllib.request, http.client
from collections import Counter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / '.test-output' / ('operations-' + secrets.token_hex(6))
OUT.mkdir(parents=True)
server_log = (OUT/'server.log').open('w', encoding='utf-8')
(OUT/'original.db').touch()
node = shutil.which('node') or str(Path.home()/'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe')
env = dict(os.environ, DATABASE_URL='file:../'+ (OUT/'original.db').relative_to(ROOT).as_posix(),
           JWT_SECRET=secrets.token_hex(48), NODE_ENV='test', JWT_EXPIRES_IN='8h')
with socket.socket() as sock:
    sock.bind(('127.0.0.1', 0))
    env['PORT'] = str(sock.getsockname()[1])
base = 'http://127.0.0.1:'+env['PORT']+'/api'
def command(*args):
    subprocess.run([node,*args], cwd=ROOT, env=env, check=True, stdout=subprocess.DEVNULL)
def request(path, body=None, token=None):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    req=urllib.request.Request(base+path, data=json.dumps(body).encode() if body is not None else None, headers=headers)
    with urllib.request.urlopen(req, timeout=10) as response:
        return json.load(response)
def start():
    proc=subprocess.Popen([node,'dist/server.js'],cwd=ROOT,env=env,stdout=server_log,stderr=server_log)
    for _ in range(100):
        if proc.poll() is not None: raise RuntimeError('Servidor encerrou antes do health')
        try:
            request('/health')
            return proc
        except OSError: time.sleep(.1)
    proc.terminate(); proc.wait(timeout=10)
    raise RuntimeError('Health indisponivel')
def stop(proc):
    proc.terminate(); proc.wait(timeout=10)
def token(): return request('/auth/login',{'email':'maria@cliente.com','senha':'123456'})['accessToken']

proc=None
try:
    command('node_modules/typescript/bin/tsc')
    command('node_modules/prisma/build/index.js','migrate','deploy')
    command('node_modules/tsx/dist/cli.mjs','prisma/seed.ts')
    proc=start()
    auth=token()
    order=request('/pedidos',{'unidadeId':1,'canalPedido':'WEB','itens':[{'produtoId':1,'quantidade':1}]},auth)
    stop(proc); proc=None
    proc=start()
    after=request('/pedidos',token=token())
    assert any(p['id']==order['id'] and p['total']==order['total'] and p['status']==order['status'] for p in after['dados'])
    stop(proc); proc=None
    with sqlite3.connect(OUT/'original.db') as src, sqlite3.connect(OUT/'backup.db') as dst: src.backup(dst)
    shutil.copy2(OUT/'backup.db',OUT/'restored.db')
    with sqlite3.connect(OUT/'restored.db') as db: assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
    env['DATABASE_URL']='file:../'+(OUT/'restored.db').relative_to(ROOT).as_posix()
    proc=start()
    restored=request('/pedidos',token=token())
    assert any(p['id']==order['id'] and p['total']==order['total'] and p['status']==order['status'] for p in restored['dados'])
    duration=int(os.environ.get('OPERATIONS_SECONDS','60'))
    start_time=time.perf_counter(); deadline=start_time+duration
    def worker(index):
        samples=[]; failures=0; server_errors=0; kinds=Counter()
        connection=http.client.HTTPConnection('127.0.0.1',int(env['PORT']),timeout=10)
        while time.perf_counter()<deadline:
            before=time.perf_counter()
            try:
                connection.request('GET','/api/produtos?page=1&limit=10&unidadeId=1' if index%2 else '/api/unidades?page=1&limit=10')
                response=connection.getresponse()
                body=json.loads(response.read())
                if response.status!=200:
                    failures+=1; kinds[str(response.status)]+=1
                    if response.status>=500: server_errors+=1
                else:
                    assert isinstance(body['dados'],list) and body['paginacao']['limite']==10
            except Exception as exc:
                failures+=1
                kinds[str(getattr(exc,'code',type(exc).__name__))]+=1
                if getattr(exc,'code',0)>=500: server_errors+=1
                connection.close()
            samples.append((time.perf_counter()-before)*1000)
        connection.close()
        return samples,failures,server_errors,kinds
    with concurrent.futures.ThreadPoolExecutor(max_workers=20) as pool: results=list(pool.map(worker,range(20)))
    elapsed=time.perf_counter()-start_time
    durations=sorted(v for r in results for v in r[0]); count=len(durations)
    report={'date':time.strftime('%Y-%m-%d'), 'platform':platform.platform(), 'cpu':platform.processor(),
            'logical_cpus':os.cpu_count(),'node':subprocess.check_output([node,'--version'],text=True).strip(),
            'clients':20,'target_seconds':duration,'elapsed_seconds':round(elapsed,2),'requests':count,
            'error_types':dict(sum((r[3] for r in results),Counter())),
            'p95_ms':round(durations[max(0,__import__('math').ceil(count*.95)-1)],2),
            'errors':sum(r[1] for r in results),'http_5xx':sum(r[2] for r in results),
            'restart_preserved_order':True,'restoration_preserved_order':True,'sqlite_integrity':'ok',
            'database_directory':str(OUT.relative_to(ROOT)),
            'scope':'20 closed-loop clients with persistent HTTP connections, public products/units, loopback, isolated seeded SQLite; forced process termination'}
    report['rnf09_target_met']=report['p95_ms']<=1000 and report['http_5xx']/count<.01 and report['errors']==0
    target=ROOT/'docs/06_RESULTADOS_OPERACIONAIS.json' if duration==60 else OUT/'diagnostic.json'
    target.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=True,indent=2))
    if not report['rnf09_target_met']:
        raise SystemExit('RNF09: meta nao atingida; consulte o relatorio.')
finally:
    if proc is not None: stop(proc)
    server_log.close()
