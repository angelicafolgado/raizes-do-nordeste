from pathlib import Path
import pymupdf
from PIL import Image, ImageOps, ImageDraw
root=Path(__file__).resolve().parents[1]
images=[]
for source in sorted((root/'docs/diagramas').glob('*.svg')):
    doc=pymupdf.open(source)
    # Exportação com ao menos 2600 px de largura para inserir no trabalho acadêmico.
    scale=max(1.5,2600/doc[0].rect.width)
    pix=doc[0].get_pixmap(matrix=pymupdf.Matrix(scale,scale),alpha=False)
    target=source.with_suffix('.png');pix.save(target)
    images.append((source.stem,Image.open(target).convert('RGB')))
    print(source.stem,pix.width,pix.height)
contact=Image.new('RGB',(1600,1800),'#dbe3e8')
for i,(name,im) in enumerate(images):
    thumb=ImageOps.contain(im,(780,840))
    x=(i%2)*800+(800-thumb.width)//2;y=(i//2)*900+40
    contact.paste(thumb,(x,y))
    ImageDraw.Draw(contact).text(((i%2)*800+15,(i//2)*900+10),name,fill='#102d40')
tmp=root/'tmp/diagramas';tmp.mkdir(parents=True,exist_ok=True)
contact.save(tmp/'contato.png')
