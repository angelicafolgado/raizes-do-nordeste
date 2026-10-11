const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
let vizModule;
try { vizModule = require('@viz-js/viz'); }
catch {
  vizModule = require(path.join(os.homedir(), '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/@viz-js/viz/dist/viz.cjs'));
}
(async () => {
  const viz = await vizModule.instance();
  const dir = path.join(__dirname, '../docs/diagramas');
  for (const name of ['01_der','02_casos_de_uso','03_classes']) {
    const svg = viz.renderString(fs.readFileSync(path.join(dir, name + '.dot'), 'utf8'), { format: 'svg', engine: 'dot' });
    fs.writeFileSync(path.join(dir, name + '.svg'), svg);
    console.log('SVG: ' + name);
  }
})().catch(err => { console.error(err); process.exitCode = 1; });
