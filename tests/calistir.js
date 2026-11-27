// Tum testleri sirayla calistirir. Bir test "HATA" satiri basarsa ya da cokerse sonuc basarisizdir.
// Ortam: OYUN_HTML (oyun dosyasi, varsayilan assets/index_test.html), CHROME_PATH (Chrome yolu)
const { spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const kok = path.resolve(__dirname, '..');
process.env.OYUN_HTML = process.env.OYUN_HTML || path.join(__dirname, 'oyun_kaynak.html').replace(/\\/g, '/');
const testler = (process.env.TESTLER || 'regresyon,tahta_test,kare_test,gb_test,dil_gb,ilk_acilis,hamle_test,muzik_test')
  .split(',').filter(t => fs.existsSync(path.join(__dirname, t + '.js')));
let basarisiz = 0;
for (const t of testler) {
  let son;
  for (let deneme = 1; deneme <= 2; deneme++) {   // kararsizlik icin tek yeniden deneme
    son = spawnSync(process.execPath, [path.join(__dirname, t + '.js')], { encoding: 'utf8', timeout: 240000, env: process.env });
    const cikti = (son.stdout || '') + (son.stderr || '');
    son.hatalar = cikti.split(/\r?\n/).filter(l => /^HATA\b/.test(l));
    son.cikti = cikti;
    if (son.status === 0 && !son.hatalar.length) break;
    if (deneme === 1) console.log('[yeniden deneniyor] ' + t);
  }
  const ok = son.status === 0 && !son.hatalar.length;
  console.log((ok ? 'GECTI  ' : 'KALDI  ') + t);
  if (!ok) { basarisiz++; console.log(son.hatalar.join('\n') || son.cikti.slice(-1500)); }
}
console.log(basarisiz ? ('\n' + basarisiz + ' test basarisiz') : '\nHepsi gecti');
process.exit(basarisiz ? 1 : 0);


