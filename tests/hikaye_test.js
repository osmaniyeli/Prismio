// Isik Bekcisi hikaye kartlari (0.12): bolge giris karti (bir kez), cikis cumlesi, sablon, diller
const puppeteer = require('puppeteer-core');
const html = process.env.OYUN_HTML || process.argv[2] || 'D:/Prismio/Prismio.html';
const bekle = ms => new Promise(r => setTimeout(r, ms));
let bad = 0; const chk = (k, v) => { if (!v) bad++; console.log((v ? 'OK   ' : 'HATA ') + k); };
(async () => {
  const b = await puppeteer.launch({ executablePath: process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', headless: 'new', args: ['--no-sandbox'] });
  async function yeni(dil, seed) {
    const ctx = await b.createBrowserContext(); const p = await ctx.newPage();
    await p.setViewport({ width: 390, height: 844, deviceScaleFactor: 1, isMobile: true, hasTouch: true });
    p.hata = []; p.on('pageerror', e => p.hata.push(e.message));
    await p.evaluateOnNewDocument((dil, seed) => {
      try { localStorage.setItem('prismio_v2', JSON.stringify(Object.assign({ _v: 2, level: 8, enYuksek: 300, nasilGordu: 1, ogreticiBitti: 1, coins: 240, lives: 5, toplamCozulen: 200, ayar: { dil: '', animasyon: false, parcacik: false, otoGec: false } }, seed || {}))); if (dil) localStorage.setItem('prismio_dil', dil); } catch (e) {}
      window.PrismioNative = { uygulamaSurumu: () => '0.12 (1)', geriBildirimGonder() {}, playDegerlendir() {} };
    }, dil, seed);
    await p.goto('file:///' + html, { waitUntil: 'load' }); await bekle(1200);
    return p;
  }
  const kart = p => p.evaluate(() => { const k = document.getElementById('katman'); return k.classList.contains('acik') ? { mod: k.getAttribute('data-og'), baslik: document.getElementById('kBaslik').textContent, alt: document.getElementById('kAlt').textContent, ust: document.getElementById('kUstEt').textContent } : null; });
  const git = (p, n) => p.evaluate(n => window.pr_level(n), n);

  // 1) bolgenin ilk bolumu: giris karti bir kez
  let p = await yeni('tr');
  await git(p, 11); await bekle(500);
  let k = await kart(p);
  chk('Bolum 11 (2. bolge): giris karti acildi', !!k && k.mod === 'hikaye');
  chk('kart: mekan adi Galata Kulesi + ozgun metin', !!k && k.baslik === 'Galata Kulesi' && /fener/i.test(k.alt));
  chk('kart ustunde bolum etiketi', !!k && /BÖLÜM 02/i.test(k.ust));
  await p.evaluate(() => document.querySelector('#kBtnler button').click()); await bekle(400);
  chk('buton karti kapatir', (await kart(p)) === null);
  await git(p, 11); await bekle(400);
  chk('ayni bolge ikinci kez gosterilmez', (await kart(p)) === null);
  await git(p, 12); await bekle(400);
  chk('bolge ortasinda (12) kart yok', (await kart(p)) === null);

  // 2) 11. bolge (indeks 10) -> sablon, yerel mekan adi
  await git(p, 101); await bekle(500); k = await kart(p);
  chk('Bolum 101 (11. bolge): sablon kart', !!k && k.baslik === 'Kapalıçarşı' && /karardı/.test(k.alt));
  await p.evaluate(() => document.querySelector('#kBtnler button').click()); await bekle(300);

  // 3) bolge sonu: sonuc kartinda cikis cumlesi
  await git(p, 10); await bekle(400);
  await p.evaluate(() => { window.pr_solve(); }); await bekle(4500);
  const ek10 = await p.evaluate(() => document.getElementById('kHediye').textContent);
  chk('Bolum 10 (bolge sonu): cikis cumlesi (Saat Kulesi)', /Saat Kulesi aydınlık/.test(ek10));
  await p.evaluate(() => { document.getElementById('katman').classList.remove('acik'); });
  await git(p, 9); await bekle(400);
  await p.evaluate(() => { window.pr_solve(); }); await bekle(4500);
  const ek9 = await p.evaluate(() => document.getElementById('kHediye').textContent);
  chk('Bolum 9: cikis cumlesi yok', !/aydınlık|yeniden/.test(ek9) || !/Saat Kulesi/.test(ek9));
  chk('hata yok (tr)', p.hata.length === 0); if (p.hata.length) console.log(p.hata.join('\n'));

  // 4) diller: EN ozgun, ES sablon (yerel mekan adi)
  p = await yeni('en'); await git(p, 11); await bekle(500); k = await kart(p);
  chk('EN: ozgun ingilizce metin', !!k && /lantern/i.test(k.alt) && /Galata/i.test(k.baslik));
  p = await yeni('es'); await git(p, 11); await bekle(500); k = await kart(p);
  chk('ES: sablon metin + yerel ad', !!k && /oscuras/.test(k.alt) && k.alt.indexOf(k.baslik) >= 0);
  for (const dil of ['pt', 'de', 'fr', 'ru', 'zhTW', 'zhCN', 'ja', 'ko', 'it', 'az']) {
    p = await yeni(dil); await git(p, 11); await bekle(400); k = await kart(p);
    chk(dil + ': giris karti metni var', !!k && k.alt.length > 8 && k.alt.indexOf('hk_') < 0 && k.alt.indexOf(k.baslik) >= 0);
    await p.browserContext().close();
  }

  // 5) zamana karsi ve ogretici: kart yok
  p = await yeni('tr', { ogreticiBitti: 0, nasilGordu: 0, enYuksek: 1, level: 1 });
  await bekle(1500); chk('ogretici bitmeden hikaye karti yok', (await kart(p)) === null || (await kart(p)).mod !== 'hikaye');

  console.log(bad ? ('SONUC: ' + bad + ' HATA') : 'SONUC: HEPSI GECTI');
  await b.close(); process.exit(bad ? 1 : 0);
})().catch(e => { console.error('HATA', e.message); process.exit(1); });
