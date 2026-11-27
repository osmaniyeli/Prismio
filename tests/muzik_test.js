// Prosedurel arka plan muzigi testleri (0.11.4): baslama, ayardan kapatma/acma, arka plana gecince durma, 13 dil metni
const puppeteer = require('puppeteer-core');
const html = process.env.OYUN_HTML || process.argv[2] || 'D:/Prismio/Prismio.html';
const bekle = ms => new Promise(r => setTimeout(r, ms));
let bad = 0; const chk = (k, v) => { if (!v) bad++; console.log((v ? 'OK   ' : 'HATA ') + k); };
(async () => {
  const b = await puppeteer.launch({ executablePath: process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', headless: 'new', args: ['--no-sandbox', '--autoplay-policy=no-user-gesture-required'] });
  async function yeni(ayar) {
    const ctx = await b.createBrowserContext(); const p = await ctx.newPage();
    await p.setViewport({ width: 390, height: 844, deviceScaleFactor: 1, isMobile: true, hasTouch: true });
    p.hata = []; p.on('pageerror', e => p.hata.push(e.message));
    await p.evaluateOnNewDocument(a => {
      try { localStorage.setItem('prismio_v2', JSON.stringify({ _v: 2, level: 8, enYuksek: 300, nasilGordu: 1, ogreticiBitti: 1, coins: 240, lives: 5, toplamCozulen: 200, ayar: Object.assign({ dil: '', animasyon: false, parcacik: false }, a || {}) })); } catch (e) {}
      if (a && a.dil) { try { localStorage.setItem('prismio_dil', a.dil); } catch (e) {} }
      window.PrismioNative = { uygulamaSurumu: () => '0.11.4 (1)', geriBildirimGonder() {}, playDegerlendir() {} };
      const Gercek = window.AudioContext; window.__ac = { ctx: null, osc: 0, susp: 0, res: 0 };
      window.AudioContext = function () {
        const c = new Gercek(); window.__ac.ctx = c;
        const so = c.createOscillator.bind(c); c.createOscillator = function () { window.__ac.osc++; return so(); };
        const ss = c.suspend.bind(c); c.suspend = function () { window.__ac.susp++; return ss(); };
        const rs = c.resume.bind(c); c.resume = function () { window.__ac.res++; return rs(); };
        return c;
      };
    }, ayar);
    await p.goto('file:///' + html, { waitUntil: 'load' }); await bekle(1200);
    return p;
  }
  const osc = p => p.evaluate(() => window.__ac.osc);
  const ac = p => p.evaluate(() => ({ susp: window.__ac.susp, res: window.__ac.res }));

  // 1) ilk dokunustan once muzik yok, sonra baslar (varsayilan acik, eski kayitta 'muzik' yok)
  let p = await yeni();
  chk('dokunmadan once ses baglami yok / muzik baslamadi', (await osc(p)) === 0);
  await p.mouse.click(200, 400); await bekle(1500);
  chk('ilk dokunus: pad kuruldu (6 osilator)', (await osc(p)) >= 6);
  await bekle(6500);
  chk('birkac sn sonra cam cinlamalari eklendi', (await osc(p)) >= 8);

  // 2) ayardan kapat -> cinlamalar durur; ac -> tekrar baslar
  await p.evaluate(() => window.pr_git('ayar')); await bekle(600);
  const sayi = n => p.evaluate(n => { const s = document.querySelector('.od-satir[data-od="muzik"]'); if (s) s.click(); return !!s; }, n);
  chk('ayar listesinde Muzik anahtari var', await sayi(0));
  await bekle(2200);
  const kapaliOnce = await osc(p); await bekle(7000);
  chk('muzik kapaliyken yeni nota uretilmez', (await osc(p)) === kapaliOnce);
  chk('state.ayar.muzik=false', await p.evaluate(() => window.pr_state().ayar.muzik === false));
  await sayi(1); await bekle(2500);
  chk('tekrar acinca pad yeniden kuruldu', (await osc(p)) > kapaliOnce);

  // 3) arka plana gecis: Java onPause -> __sesDurakla ; onResume -> __sesDevam
  await p.evaluate(() => window.__sesDurakla()); await bekle(500);
  const a1 = await ac(p);
  chk('onPause: ses baglami askiya alindi', a1.susp >= 1);
  await p.evaluate(() => window.__sesDevam()); await bekle(600);
  const a2 = await ac(p);
  chk('onResume: ses baglami devam etti', a2.res > a1.res);
  chk('hata yok', p.hata.length === 0); if (p.hata.length) console.log(p.hata.join('\n'));

  // 4) ayar kapali baslayan oyuncu: hic ses yok
  p = await yeni({ muzik: false });
  await p.mouse.click(200, 400); await bekle(1500);
  chk('ayar kapali: pad/cinlama yok (yalniz dokunus sesi)', (await osc(p)) <= 2);

  // 5) 13 dilde ayar metni
  for (const dil of ['tr', 'en', 'es', 'pt', 'de', 'fr', 'ru', 'zhTW', 'zhCN', 'ja', 'ko', 'it', 'az']) {
    p = await yeni({ dil });
    const t = await p.evaluate(() => (window.pr_T ? window.pr_T('a_muzik') : null));
    chk('dil ' + dil + ': a_muzik = ' + t, !!t && t !== 'a_muzik' && (dil === 'tr' || t !== 'Müzik' || dil === 'tr'));
    await p.browserContext().close();
  }

  console.log(bad ? ('SONUC: ' + bad + ' HATA') : 'SONUC: HEPSI GECTI');
  await b.close(); process.exit(bad ? 1 : 0);
})().catch(e => { console.error('HATA', e.message); process.exit(1); });

