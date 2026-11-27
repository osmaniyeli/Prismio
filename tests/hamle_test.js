// Hamle siniri, ekstra hamle (isik/reklam), 2x odul testleri (0.11.4)
const puppeteer = require('puppeteer-core');
const html = process.env.OYUN_HTML || process.argv[2] || 'D:/Prismio/Prismio.html';
const bekle = ms => new Promise(r => setTimeout(r, ms));
let bad = 0; const chk = (k, v) => { if (!v) bad++; console.log((v ? 'OK   ' : 'HATA ') + k); };
const GUN = () => { const d = new Date(); return d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate(); };
(async () => {
  const b = await puppeteer.launch({ executablePath: process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', headless: 'new', args: ['--no-sandbox'] });
  async function yeni(reklam, seed) {
    const ctx = await b.createBrowserContext(); const p = await ctx.newPage();
    await p.setViewport({ width: 390, height: 844, deviceScaleFactor: 1, isMobile: true, hasTouch: true });
    p.hata = []; p.on('pageerror', e => p.hata.push(e.message));
    await p.evaluateOnNewDocument((reklam, seed) => {
      try { localStorage.setItem('prismio_v2', JSON.stringify(Object.assign({ _v: 2, level: 8, enYuksek: 300, nasilGordu: 1, ogreticiBitti: 1, coins: 240, lives: 5, toplamCozulen: 200, ayar: { dil: '', animasyon: false, parcacik: false, otoGec: false } }, seed || {}))); } catch (e) {}
      window.PrismioNative = { uygulamaSurumu: () => '0.11.4 (1)', geriBildirimGonder() {}, playDegerlendir() {} };
      if (reklam) { window.PrismioNative.reklamHazir = () => true; window.PrismioNative.reklamGoster = () => setTimeout(() => window.__reklamSonuc(true), 60); }
    }, reklam, seed);
    await p.goto('file:///' + html, { waitUntil: 'load' }); await bekle(1200);
    await p.evaluate(() => { window.pr_level(8); }); await bekle(600);
    return p;
  }
  const etiket = p => p.evaluate(() => { const e = document.querySelector('#bolumAlt .hamle-sayac'); return e ? e.textContent.trim() : null; });
  const st = (p, f) => p.evaluate(f);
  const kart = p => p.evaluate(() => { const k = document.getElementById('katman'); return k.classList.contains('acik') ? k.getAttribute('data-og') : null; });
  const dugmeler = p => p.evaluate(() => [...document.querySelectorAll('#kBtnler button')].map(x => x.textContent.trim()));
  async function tap(p, x, y) { await p.evaluate((x, y) => window.pr_cycle(x, y), x, y); await bekle(80); }
  async function bitir(p) { // cozumu bozmayan, bos bir kareye art arda dokunarak hamleleri tuket
    const n = await st(p, () => 5); let i = 0;
    while ((await st(p, () => { const s = window.pr_state(); return (s.hamle || 0); })) < (await toplam(p)) && i++ < 40) { await tap(p, 0, 3); }
  }
  async function toplam(p) { const e = await etiket(p); return e ? +e.split('/')[1] : 0; }

  // 1) sinir formulu + sayac + geri al + sifirla
  let p = await yeni(false);
  const sol = await st(p, () => window.pr_state().solution);
  const cozum = Object.values(sol).reduce((a, v) => a + (v === '\\' ? 2 : 1), 0);
  const sinir = cozum + Math.max(2, Math.ceil(cozum * 0.5));
  chk('etiket: Hamle 0/' + sinir + ' (cozum ' + cozum + ')', /0\/(\d+)/.test((await etiket(p)) || '') && (await toplam(p)) === sinir);
  await tap(p, 0, 3); chk('1 dokunus: 1/' + sinir, /1\/\d+/.test(await etiket(p)));
  await p.click('#btnGeriAl'); await bekle(150);
  chk('Geri al hamleyi iade eder (0)', /0\/\d+/.test(await etiket(p)));
  await tap(p, 0, 3); await tap(p, 0, 3);
  await p.click('#btnSifirla'); await bekle(200);
  chk('Sifirla hamleyi sifirlar', /0\/\d+/.test(await etiket(p)));

  // 2) hamle bitince kart: reklam yok -> isik + tekrar + harita
  await bitir(p); await bekle(1200);
  chk('hamle bitti karti acildi', (await kart(p)) === 'hamle');
  let d = await dugmeler(p);
  chk('reklam yokken 3 dugme (isik, tekrar, harita)', d.length === 3 && !d.some(x => /İzle|Watch/.test(x)));
  chk('kilit: yeni dokunus hamle saymaz', await (async () => { const o = await st(p, () => window.pr_state().hamle); await tap(p, 1, 3); return (await st(p, () => window.pr_state().hamle)) === o; })());
  // isikle +5
  const c0 = await st(p, () => window.pr_state().coins);
  await p.evaluate(() => { [...document.querySelectorAll('#kBtnler button')].find(x => /ışık|light/i.test(x.textContent)).click(); }); await bekle(400);
  const c1 = await st(p, () => window.pr_state().coins);
  chk('isikle +5 hamle: 25 isik dustu', c0 - c1 === 25 && (await toplam(p)) === sinir + 5 && (await kart(p)) === null);
  // yetersiz isik
  await bitir(p); await bekle(1200);
  await st(p, () => { window.pr_state().coins = 3; });
  await p.evaluate(() => { [...document.querySelectorAll('#kBtnler button')].find(x => /ışık|light/i.test(x.textContent)).click(); }); await bekle(400);
  chk('yetersiz isik: hamle eklenmez', (await toplam(p)) === sinir + 5);
  chk('hata yok (reklamsiz akis)', p.hata.length === 0); if (p.hata.length) console.log(p.hata.join('\n'));

  // 3) reklam var: izle +5, kota 5/gun
  p = await yeni(true);
  await bitir(p); await bekle(1200);
  d = await dugmeler(p);
  chk('reklam varken 4 dugme (izle dahil)', d.length === 4 && /İzle|Watch/.test(d[0]));
  await p.evaluate(() => { document.querySelector('#kBtnler button').click(); }); await bekle(700);
  chk('reklamla +5 hamle', (await toplam(p)) === sinir + 5 && (await kart(p)) === null);
  const kota = await st(p, () => { const m = window.pr_state().magaza || {}; return m.hamle_reklam_kota; });
  chk('kota sayaci 1', !!kota && kota.n === 1);
  await st(p, g => { window.pr_state().magaza.hamle_reklam_kota = { g: g, n: 5 }; }, GUN());
  await p.evaluate(g => { window.pr_state().magaza.hamle_reklam_kota = { g: g, n: 5 }; }, GUN());
  await bitir(p); await bekle(1200);
  d = await dugmeler(p);
  chk('kota doldu: izle dugmesi yok', d.length === 3 && !d.some(x => /İzle|Watch/.test(x)));
  chk('hata yok (reklamli akis)', p.hata.length === 0);

  // 4) 2x odul: kazanma kartinda; ortak kota; tek sefer
  p = await yeni(true, { stars: {} });
  await st(p, () => { const s = window.pr_state(); s.coins = 100; s.magaza = {}; });
  const once = await st(p, () => window.pr_state().coins);
  await p.evaluate(() => { window.pr_solve(); }); await bekle(4500);
  const kazanc = await st(p, () => +document.getElementById('kOdulSayi').textContent);
  const mid = await st(p, () => window.pr_state().coins);
  chk('bolum kazanildi + kart acildi', (await kart(p)) !== null && kazanc > 0 && mid - once === kazanc);
  d = await dugmeler(p);
  chk('kartta "Isigi 2 katla" dugmesi', d.length >= 1 && /2 kat|Double/i.test(d[0]));
  await p.evaluate(() => { document.querySelector('#kBtnler button').click(); }); await bekle(800);
  const son = await st(p, () => window.pr_state().coins);
  chk('2x: ek ' + kazanc + ' isik, ekran toplam ' + (kazanc * 2), son - mid === kazanc && (await st(p, () => +document.getElementById('kOdulSayi').textContent)) === kazanc * 2);
  chk('2x tek sefer: dugme kalktı', !(await dugmeler(p)).some(x => /2 kat|Double/i.test(x)));
  const ortak = await st(p, () => { const m = window.pr_state().magaza || {}; return m.isik_reklam_kota; });
  chk('ortak gunluk kota (isik_reklam) 1', !!ortak && ortak.n === 1);
  chk('hata yok (2x)', p.hata.length === 0);

  // 5) reklam yokken 2x yok
  p = await yeni(false, { stars: {} });
  await p.evaluate(() => { window.pr_solve(); }); await bekle(4500);
  d = await dugmeler(p);
  chk('reklam yok: kartta 2x dugmesi yok', !d.some(x => /2 kat|Double/i.test(x)));

  // 6) zaman modunda hamle sinirsiz
  p = await yeni(false);
  await p.evaluate(() => { window.pr_git && window.pr_git('menu'); }); await bekle(500);
  await p.click('#btnZaman'); await bekle(1500);
  chk('zamana karsi: hamle etiketi yok', (await etiket(p)) === null);

  console.log(bad ? ('SONUC: ' + bad + ' HATA') : 'SONUC: HEPSI GECTI');
  await b.close(); process.exit(bad ? 1 : 0);
})().catch(e => { console.error('HATA', e.message); process.exit(1); });
