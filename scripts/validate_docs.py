"""Valida schemas/exemplos locais e consistência da coleção. Requer jsonschema."""
from pathlib import Path
import json
import re
from jsonschema import Draft4Validator, FormatChecker, RefResolver

root=Path(__file__).resolve().parents[1]
doc=json.loads((root/'src/api/docs/swagger.json').read_text(encoding='utf-8'))
def normalize(value):
    if isinstance(value,list): return [normalize(v) for v in value]
    if not isinstance(value,dict): return value
    result={k:normalize(v) for k,v in value.items()}
    if result.pop('nullable',False):
        result['type']=[result.get('type','object'),'null']
        if 'enum' in result: result['enum'].append(None)
    return result
normalized=normalize(doc)
resolver=RefResolver.from_schema(normalized)
count=0
operations=0
def validate_content(content, label):
    global count
    for media in content.values():
        assert 'schema' in media,label
        values=([media['example']] if 'example' in media else [])+[item['value'] for item in media.get('examples',{}).values()]
        assert values,'Exemplo ausente: '+label
        validator=Draft4Validator(normalize(media['schema']),resolver=resolver,format_checker=FormatChecker())
        for value in values:
            validator.validate(value)
            count+=1
def references(value):
    if isinstance(value,dict):
        if '$ref' in value:
            target=doc
            assert value['$ref'].startswith('#/'),'Referencia externa não esperada'
            for key in value['$ref'][2:].split('/'): target=target[key]
        for child in value.values(): references(child)
    elif isinstance(value,list):
        for child in value: references(child)
references(doc)
ids=set()
for path,methods in doc['paths'].items():
    for method,op in methods.items():
        operations+=1
        assert op['operationId'] not in ids
        ids.add(op['operationId'])
        assert 'security' in op,(path,'permissoes ausentes')
        if 'requestBody' in op: validate_content(op['requestBody']['content'],method+' '+path+' request')
        for code,res in op['responses'].items(): validate_content(res['content'],method+' '+path+' '+code)

c=json.loads((root/'postman/raizes_nordeste_clean_collection.json').read_text(encoding='utf-8'))
def flatten(items):
    return [request for item in items for request in (flatten(item['item']) if 'item' in item else [item])]
cases=flatten(c['item'])
positive=0
for n,item in enumerate(cases,1):
    assert item['name'].startswith(f'T{n:02d} - ')
    assert 'Pré-condição:' in item['request']['description']
    code=int(re.search(r'HTTP (\d+)',item['event'][0]['script']['exec'][0])[1])
    positive+=code<400
    path=item['request']['url'].replace('{{baseUrl}}/api','').split('?')[0].replace('{{pedidoId}}','{id}')
    assert item['request']['method'].lower() in doc['paths'][path],path
    assert str(code) in doc['paths'][path][item['request']['method'].lower()]['responses'],(path,code)
assert len(cases)>=10 and positive>=6 and len(cases)-positive>=4
assert all('item' in folder for folder in c['item'])
print(f'OpenAPI: {operations} operacoes, {count} exemplos validados; referencias e IDs consistentes.')
print(f'Postman: {len(cases)} cenarios em {len(c["item"])} pastas; {positive} respostas 2xx e {len(cases)-positive} erros.')
