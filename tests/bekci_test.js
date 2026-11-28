// Isik Bekcisi: ifadeler, fener rengi, mekan fenerleri, surpriz oduller, kayit, 13 dil (0.14)
const puppeteer = require('puppeteer-core');
const html = process.env.OYUN_HTML || process.argv[2] || 'D:/Prismio/Prismio.html';
const bekle = ms => new Promise(r => setTimeout(r, ms));
let bad = 0; const chk = (k, v) => { if (!v) bad++; console.log((v ? 'OK   ' : 'HATA ') + k); };
const DILLER = ['tr', 'en', 'es', 'pt', 'de', 'fr', 'ru', 'zhTW', 'zhCN', 'ja', 'ko', 'it', 'az'];
const IFADELER = ['notr', 'sevinc', 'saskin', 'uykulu', 'kararli'];
const ANAHTARLAR = ['bk_baslik', 'bk_ifadeler', 'bk_ifade_say', 'bk_if_notr', 'bk_if_sevinc', 'bk_if_saskin', 'bk_if_uykulu', 'bk_if_kararli', 'bk_if_kilit',
  'bk_fener_rengi', 'bk_f_kehribar', 'bk_f_nane', 'bk_f_gul', 'bk_f_gok', 'bk_f_mor', 'bk_esik', 'bk_kilitli', 'bk_secildi', 'bk_koleksiyon', 'bk_koleksiyon_say',
  'bk_koleksiyon_ack', 'bk_fener_yok', 'bk_fener_kazandin', 'bk_surpriz_fener', 'bk_surpriz_ses', 'bk_surpriz_ses_acildi', 'bk_ses_parca',
  'b_fener10', 'ba_fener10', 'b_fener50', 'ba_fener50', 'b_ifade5', 'ba_ifade5', 'b_fener_renk', 'ba_fener_renk'];
const EMOJI = /\p{Extended_Pictographic}/u;
(async () => {
  const b = await puppeteer.launch({ executablePath: process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', headless: 'new', args: ['--no-sandbox'] });
  const BAZ = { _v: 2, level: 8, enYuksek: 300, nasilGordu: 1, ogreticiBitti: 1, coins: 240, lives: 5, toplamCozulen: 200, ayar: { dil: '', animasyon: true, parcacik: false, otoGec: false } };
  // seed: nesne ya da ham metin; dil: localStorage.prismio_dil (sabit); sahteTarih: ms (Date sabitlenir)
  async function yeni(seed, opt) {
    opt = opt || {};
    const ctx = await b.createBrowserContext(); const p = await ctx.newPage();
    await p.setViewport({ width: 390, height: 844, deviceScaleFactor: 1, isMobile: true, hasTouch: true });
    p.hata = []; p.on('pageerror', e => p.hata.push(e.message));
    await p.evaluateOnNewDocument((ham, dil, sahte) => {
      try { localStorage.setItem('prismio_v2', ham); localStorage.setItem('prismio_dil', dil); } catch (e) {}
      window.PrismioNative = { uygulamaSurumu: () => '0.14 (1)', geriBildirimGonder() {}, playDegerlendir() {} };   // reklamHazir YOK: reklam kapali
      if (sahte) { const RD = Date; window.Date = class extends RD { constructor(...a) { if (a.length) super(...a); else super(sahte); } static now() { return sahte; } }; }
    }, typeof seed === 'string' ? seed : JSON.stringify(Object.assign({}, BAZ, seed || {})), opt.dil || 'tr', opt.sahteTarih || 0);
    await p.goto('file:///' + html, { waitUntil: 'load' }); await bekle(1100);
    return p;
  }
  const st = (p, f, a) => p.evaluate(f, a);
  const kartInfo = p => p.evaluate(() => { const k = document.getElementById('katman'); if (!k.classList.contains('acik')) return null;
    const ik = document.getElementById('kIkon'); const sv = ik.querySelector('svg.bk-yuz');
    return { mod: k.getAttribute('data-og'), ifade: sv ? (sv.getAttribute('class').match(/bk-e-(\w+)/) || [])[1] : null, ikGizli: ik.hidden, aria: sv ? sv.getAttribute('aria-hidden') : null, hediye: document.getElementById('kHediye').textContent, odul: +document.getElementById('kOdulSayi').textContent }; });
  const kaydet = p => p.evaluate(() => JSON.parse(localStorage.getItem('prismio_v2')));
  async function coz(p, lv, eksikAyna) {
    await p.evaluate(n => window.pr_level(n), lv); await bekle(500);
    await p.evaluate(e => { if (e) window.pr_state().best = Math.max(1, window.pr_state().best - e); window.pr_solve(); }, eksikAyna || 0); await bekle(4500);
  }

  // ---- 1) bekciYuz: tek kaynak, 5 ifade, emoji yok, dekoratif ----
  let p = await yeni();
  const yuzler = await st(p, ifs => ifs.map(e => window.pr_bekciYuz(e, 40)), IFADELER);
  chk('5 ifade 5 farkli SVG', new Set(yuzler).size === 5);
  chk('her ifade kendi sinifini tasir (bk-e-*)', yuzler.every((y, i) => y.indexOf('bk-e-' + IFADELER[i]) > 0));
  chk('dekoratif: aria-hidden=true + boy=40', yuzler.every(y => /aria-hidden="true"/.test(y) && /width="40"/.test(y)));
  chk('satir ici SVG, Unicode emoji yok', yuzler.every(y => y.startsWith('<svg') && !EMOJI.test(y)));
  chk('bilinmeyen ifade -> notr', (await st(p, () => window.pr_bekciYuz('xyz', 30))).indexOf('bk-e-notr') > 0);
  const fenerFarki = await st(p, () => ['kehribar', 'nane', 'gul', 'gok', 'mor'].map(f => window.pr_bekciYuz('notr', 30, f)));
  chk('5 fener rengi 5 farkli yuz', new Set(fenerFarki).size === 5);

  // ---- 2) baglam: kartlarda dogru ifade ----
  await st(p, () => window.pr_level(11)); await bekle(700);
  let k = await kartInfo(p);
  chk('bolge giris karti: kararli bekci', !!k && k.mod === 'hikaye' && k.ifade === 'kararli' && !k.ikGizli && k.aria === 'true');
  await st(p, () => document.querySelector('#kBtnler button').click()); await bekle(300);
  await coz(p, 5); k = await kartInfo(p);
  chk('3 yildiz sonuc karti: sevinc', !!k && k.mod === 'sonuc' && k.ifade === 'sevinc');
  await st(p, () => { document.getElementById('katman').classList.remove('acik'); });
  await coz(p, 6, 1); k = await kartInfo(p);
  chk('az yildizli sonuc karti: notr (dikkatli)', !!k && k.mod === 'sonuc' && k.ifade === 'notr');
  await st(p, () => { document.getElementById('katman').classList.remove('acik'); });
  // hamle bitti
  await st(p, () => window.pr_level(8)); await bekle(600);
  for (let i = 0; i < 60; i++) { const o = await st(p, () => { const s = window.pr_state(); const m = document.querySelector('#bolumAlt .hamle-sayac'); return m ? [s.hamle || 0, +m.textContent.split('/')[1]] : null; }); if (!o || o[0] >= o[1]) break; await st(p, () => window.pr_cycle(0, 3)); await bekle(60); }
  await bekle(1200); k = await kartInfo(p);
  chk('hamle bitti karti: saskin', !!k && k.mod === 'hamle' && k.ifade === 'saskin');
  await st(p, () => { document.getElementById('katman').classList.remove('acik'); window.pr_state()._hamleKart = false; });
  // can bitti
  await st(p, () => { window.pr_state().lives = 0; window.pr_level(8); }); await bekle(900); k = await kartInfo(p);
  chk('can bitti karti: uykulu', !!k && k.mod === 'can' && k.ifade === 'uykulu');
  const sd = await kaydet(p);
  chk('gorulen 5 ifade kayitli', IFADELER.every(e => sd.bekci.gorulen[e] === 1));
  chk('basarim: 5 farkli ifade (a-ifade5)', !!sd.basari['a-ifade5']);
  chk('hata yok (kartlar)', p.hata.length === 0); if (p.hata.length) console.log(p.hata.join('\n'));
  await p.browserContext().close();

  // ---- 3) fener odulu: bolge sonunda bir kez, kayitli ----
  p = await yeni({ level: 10, enYuksek: 10, coins: 100, stars: {}, ayar: Object.assign({}, BAZ.ayar) });
  chk('yeni oyuncu: fener yok', (await st(p, () => window.pr_bekciFenerler().sayi)) === 0);
  await coz(p, 9);
  chk('bolum 9 (bolge ortasi): fener yok', (await st(p, () => window.pr_bekciFenerler().sayi)) === 0);
  await st(p, () => { document.getElementById('katman').classList.remove('acik'); });
  const c0 = await st(p, () => window.pr_state().coins);
  await coz(p, 10); k = await kartInfo(p);
  const c1 = await st(p, () => window.pr_state().coins);
  chk('bolum 10 (bolge sonu): fener 1 yandi', (await st(p, () => window.pr_bekciFenerler().fenerler[1])) === 1);
  chk('isik = kart kazanci + 5 fener bonusu', !!k && c1 - c0 === k.odul + 5);
  chk('sonuc kartinda fener satiri (Saat Kulesi, +5 isik)', !!k && /Mekân feneri yandı: Saat Kulesi · \+5 ışık/.test(k.hediye));
  chk('fener satirinda SVG muhur', await st(p, () => !!document.querySelector('#kHediye .og-ek.bk svg.bk-fener')));
  let sd2 = await kaydet(p);
  chk('kayit: fenerler {1:1} saklandi', sd2.fenerler && sd2.fenerler['1'] === 1);
  await st(p, () => { document.getElementById('katman').classList.remove('acik'); });
  const c2 = await st(p, () => window.pr_state().coins);
  await coz(p, 10);
  const c3 = await st(p, () => window.pr_state().coins);
  chk('ayni bolge tekrar: fener bonusu ikinci kez yok', c3 - c2 === 0 && (await st(p, () => window.pr_bekciFenerler().sayi)) === 1);
  chk('tekrarda fener satiri yok', !/Mekân feneri yandı/.test((await kartInfo(p)).hediye));
  chk('101. bolge ve 0: fener verilmez', await st(p, () => window.pr_bekciFenerVer(101) === null && window.pr_bekciFenerVer(0) === null && window.pr_bekciFenerVer(100) !== null));
  chk('hata yok (fener)', p.hata.length === 0); if (p.hata.length) console.log(p.hata.join('\n'));
  // yeniden yukleme: kayit kalir
  await st(p, () => window.pr_coins(0));   // save()
  const ham = await st(p, () => localStorage.getItem('prismio_v2'));
  await p.browserContext().close();
  p = await yeni(ham);
  chk('yeniden yukleme: fener 1 + 100 duruyor', await st(p, () => { const f = window.pr_bekciFenerler(); return f.fenerler[1] === 1 && f.fenerler[100] === 1 && f.sayi === 2; }));
  await p.browserContext().close();

  // ---- 4) koleksiyon ekrani, geri doldurma, fener rengi secimi ----
  const coinOnce = 240;
  p = await yeni({ enYuksek: 38, level: 8 });
  chk('eski kayit (enYuksek 38): 3 bolge feneri sessizce yandi, bonus yok', await st(p, () => window.pr_bekciFenerler().sayi === 3 && window.pr_state().coins === 240));
  await st(p, () => window.pr_git('lig')); await bekle(500);
  const kol = await st(p, () => ({ toplam: document.querySelectorAll('#bkBolum .bk-fz').length, yanan: document.querySelectorAll('#bkBolum .bk-fz svg.bk-fener:not(.bk-kilit)').length,
    sayi: [...document.querySelectorAll('#bkBolum .bk-baslik .tnum')].map(e => e.textContent).join('|'), ifadeBtn: document.querySelectorAll('#bkBolum .bk-if').length, renkBtn: document.querySelectorAll('#bkBolum .bk-renk').length,
    gizli: [...document.querySelectorAll('#bkBolum svg')].every(s => s.getAttribute('aria-hidden') === 'true') }));
  chk('koleksiyon: 100 mekan, 3 yanik, "3/100 fener"', kol.toplam === 100 && kol.yanan === 3 && /3\/100 fener/.test(kol.sayi));
  chk('5 ifade + 5 fener rengi dugmesi, SVG dekoratif', kol.ifadeBtn === 5 && kol.renkBtn === 5 && kol.gizli);
  chk('kilitli fener rengi secilemez (gul: 8 fener)', await (async () => { await st(p, () => document.querySelector('#bkBolum [data-v="gul"]').click()); await bekle(200); return (await st(p, () => window.pr_state().bekci.fener)) === 'kehribar'; })());
  await st(p, () => document.querySelector('#bkBolum [data-v="nane"]').click()); await bekle(200);
  chk('3 fener: nane secilebildi', (await st(p, () => window.pr_state().bekci.fener)) === 'nane');
  chk('secilen renk bekci yuzunde (nane #7BDDAA)', await st(p, () => window.pr_bekciYuz('notr', 20).indexOf('#7BDDAA') > 0 && document.querySelector('#bkBolum .bk-buyuk').innerHTML.indexOf('#7BDDAA') > 0));
  const ham2 = await st(p, () => localStorage.getItem('prismio_v2'));
  await p.browserContext().close();
  p = await yeni(ham2);
  chk('yeniden yukleme: secili renk nane kaldi', (await st(p, () => window.pr_state().bekci.fener)) === 'nane');
  // disa aktar -> baska kayda ice aktar
  const kod = await st(p, () => { window.pr_state().bekci.gorulen.sevinc = 1; window.pr_state().fenerler[77] = 1; return window.pr_exportSave(); });
  await p.browserContext().close();
  p = await yeni({ enYuksek: 5, level: 5 });
  chk('temiz kayitta fener yok', (await st(p, () => window.pr_bekciFenerler().sayi)) === 0);
  const ic = await st(p, k2 => window.pr_importSave(k2), kod);
  chk('ice aktar: bekci + fenerler tasindi', !!ic && ic.ok !== false && await st(p, () => { const s = window.pr_state(); return s.bekci.fener === 'nane' && s.bekci.gorulen.sevinc === 1 && s.fenerler[77] === 1 && s.fenerler[1] === 1; }));
  await p.browserContext().close();
  // bozuk kayit
  p = await yeni({ enYuksek: 5, bekci: { fener: 'zzz', gorulen: { x: 1, notr: 1 }, acik: { gul: 1, hile: 1, kehribar: 1 }, sesParca: 99 }, fenerler: { '5': 1, '500': 1, abc: 1, '2.5': 1, '0': 1, '7': 0 } });
  chk('bozuk kayit: gecersiz alanlar atilir', await st(p, () => { const s = window.pr_state(); return s.bekci.fener === 'kehribar' && Object.keys(s.bekci.gorulen).join() === 'notr' && Object.keys(s.bekci.acik).join() === 'gul' && s.bekci.sesParca === 0 && Object.keys(s.fenerler).join() === '5'; }));
  chk('hata yok (bozuk kayit)', p.hata.length === 0);
  await p.browserContext().close();

  // ---- 5) surpriz oduller: deterministik, kozmetik, reklamsiz ----
  p = await yeni();
  const taraO = await st(p, () => {
    const gunler = []; for (let i = 0; i < 400; i++) { const d = new Date(2027, 0, 1 + i); gunler.push(d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate()); }
    const sonuc = (a) => gunler.map(g => window.pr_bekciSurpriz(a, g));
    const seri = sonuc('seri'), gorev = sonuc('gorev:g-coz3');
    const tekrar = sonuc('seri');
    return { seriVur: seri.filter(Boolean).length, gorevVur: gorev.filter(Boolean).length, ayni: JSON.stringify(seri) === JSON.stringify(tekrar), turler: [...new Set(seri.concat(gorev).filter(Boolean).map(x => x.tur))].sort().join(),
      ilkVur: gunler[seri.findIndex(Boolean)], ilkYok: gunler[seri.findIndex(x => !x)] };
  });
  chk('surpriz deterministik (ayni gun = ayni sonuc)', taraO.ayni);
  chk('seri surprizi ~%30, gorev ~%12 (400 gun)', taraO.seriVur > 90 && taraO.seriVur < 150 && taraO.gorevVur > 25 && taraO.gorevVur < 75);
  chk('surpriz turleri: fener rengi + ses parcasi', taraO.turler === 'fener,ses');
  // uygulama: ilk "ses" ve "fener" sonuclari
  const uyg = await st(p, () => {
    const out = { renk: null, ses: [] };
    const gun = i => { const d = new Date(2028, 0, 1 + i); return d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate(); };
    for (let i = 0; i < 400 && (!out.renk || out.ses.length < 6); i++) {
      const r = window.pr_bekciSurpriz('seri', gun(i)); if (!r) continue;
      const v = window.pr_bekciSurprizVer('seri', gun(i)); const s = window.pr_state();
      if (r.tur === 'fener' && !out.renk) out.renk = { id: r.id, acik: !!s.bekci.acik[r.id], metin: v.metin };
      if (r.tur === 'ses') out.ses.push({ id: r.id, parca: s.bekci.sesParca, ahsap: !!s.paket.ses.ahsap, retro: !!s.paket.ses.retro, metin: v.metin });
    }
    out.fener = s2(); function s2() { const s = window.pr_state(); return Object.keys(s.bekci.acik).sort().join(); }
    return out;
  });
  chk('surpriz fener rengi acildi + metin', !!uyg.renk && uyg.renk.acik && /Sürpriz: yeni fener rengi/.test(uyg.renk.metin));
  chk('ses parcasi: 1/3, 2/3, sonra ses tipi acilir (ahsap), sonra retro', uyg.ses.length >= 6 && uyg.ses[0].parca === 1 && uyg.ses[1].parca === 2 && uyg.ses[2].ahsap && uyg.ses[2].parca === 0 && uyg.ses[5].retro && /ses rengi parçası 1\/3/.test(uyg.ses[0].metin) && /yeni ses tipi/.test(uyg.ses[2].metin));
  const sd3 = await kaydet(p);
  chk('surpriz sonucu kayitli (acik renkler, ses paketi)', Object.keys(sd3.bekci.acik).length >= 1 && sd3.paket.ses.ahsap === 1);
  chk('hepsi acik olunca surpriz cikmaz (null)', await st(p, () => { for (let i = 0; i < 200; i++) { const d = new Date(2029, 0, 1 + i); const g = d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate(); if (window.pr_bekciSurpriz('seri', g) || window.pr_bekciSurpriz('gorev:x', g)) return false; } return true; }));
  chk('hata yok (surpriz)', p.hata.length === 0); if (p.hata.length) console.log(p.hata.join('\n'));
  await p.browserContext().close();

  // seri odulu kartinda surpriz satiri (reklam yok): vurdugu gun gorunur, vurmadigi gun gizli
  p = await yeni();
  const gunler = await st(p, () => { let vur = null, yok = null; for (let i = 0; i < 200 && !(vur && yok); i++) { const d = new Date(2027, 5, 1 + i, 12); const g = d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate(); const r = window.pr_bekciSurpriz('seri', g); if (r && !vur) vur = d.getTime(); if (!r && !yok) yok = d.getTime(); } return { vur, yok }; });
  await p.browserContext().close();
  for (const [ad, ms, beklenen] of [['vurdugu gun', gunler.vur, true], ['vurmadigi gun', gunler.yok, false]]) {
    p = await yeni({ coins: 50 }, { sahteTarih: ms });
    await st(p, () => { const s = window.pr_state(); const g = Math.floor((Date.now() - new Date().getTimezoneOffset() * 60000) / 86400000); s.seri = { gun: g, sayi: 3, enUzun: 3, toplamGun: 3 }; });
    await st(p, () => window.pr_git('basari')); await bekle(600);
    const c = await st(p, () => window.pr_state().coins);
    await st(p, () => { const b2 = document.getElementById('odIlkAl'); if (b2) b2.click(); }); await bekle(700);
    const sonuc = await st(p, () => { const e = document.getElementById('oeEk'); const ef = document.getElementById('odEfekt'); return { acik: !!ef && ef.classList.contains('acik'), gorunur: !!e && !e.hidden, metin: e ? e.textContent : '', svg: !!(e && e.querySelector('svg, img.i3d')), coins: window.pr_state().coins }; });
    chk('seri odulu (' + ad + '): efekt acildi, isik geldi', sonuc.acik && sonuc.coins > c);
    chk('seri odulu (' + ad + '): surpriz satiri ' + (beklenen ? 'gorunur + SVG' : 'gizli'), sonuc.gorunur === beklenen && (!beklenen || (/Sürpriz/.test(sonuc.metin) && sonuc.svg)));
    chk('hata yok (seri odulu ' + ad + ')', p.hata.length === 0); if (p.hata.length) console.log(p.hata.join('\n'));
    await p.browserContext().close();
  }

  // ---- 6) basarimlar ----
  p = await yeni({ enYuksek: 300, bekci: { fener: 'kehribar', gorulen: { notr: 1, sevinc: 1, saskin: 1, uykulu: 1, kararli: 1 }, acik: {}, sesParca: 0 } });
  const bas = await st(p, () => { const m = {}; window.pr_achievements().forEach(a => { m[a.id] = a; }); return m; });
  chk('4 yeni basarim tabloda', ['a-fener10', 'a-fener50', 'a-ifade5', 'a-fener-renk'].every(i => !!bas[i]));
  chk('29 fener: a-fener10 acik, a-fener50 kapali (29/50)', bas['a-fener10'].acik && !bas['a-fener50'].acik && bas['a-fener50'].ilerleme === 29);
  chk('5 ifade: a-ifade5 acik; 3 renk (3/8/15 fener esigi): a-fener-renk acik', bas['a-ifade5'].acik && bas['a-fener-renk'].acik);
  await st(p, () => window.pr_taskDraw()); await bekle(300);
  const rozet = await st(p, () => document.querySelector('#basariListe').textContent);
  chk('Odul odasinda yeni basarimlar gorunur (TR adlar)', /Mekân Koleksiyoncusu/.test(rozet) && /Fener Ustası/.test(rozet) && /Bekçinin Yüzleri/.test(rozet) && /Renkli Fener/.test(rozet));
  await p.browserContext().close();

  // ---- 7) hareket ayarlari: animasyon kapaliyken yuz animasyonu yok ----
  for (const [ad, ayarlar, beklenenAnim] of [['animasyon acik', { animasyon: true, azHareket: false }, true], ['animasyon kapali', { animasyon: false }, false], ['az hareket', { animasyon: true, azHareket: true }, false]]) {
    p = await yeni({ ayar: Object.assign({}, BAZ.ayar, ayarlar) });
    const an = await st(p, () => { const d = document.createElement('div'); d.innerHTML = window.pr_bekciYuz('kararli', 60); document.body.appendChild(d); const g = d.querySelector('.bk-parla'); const r = getComputedStyle(g).animationName; d.remove(); return r; });
    chk('bekci animasyonu: ' + ad + (beklenenAnim ? ' -> calisir' : ' -> yok'), beklenenAnim ? an !== 'none' : an === 'none');
    await p.browserContext().close();
  }

  // ---- 8) 13 dil: metinler dolu, yer tutucular korunuyor, ekran tasmiyor ----
  p = await yeni();
  const trT = await st(p, a => a.map(k2 => window.pr_T(k2)), ANAHTARLAR);
  const tokenler = s => (s.match(/\{\w+\}/g) || []).sort().join();
  for (const dil of DILLER) {
    await st(p, d => window.pr_dilKur(d), dil);
    const r = await st(p, a => a.map(k2 => window.pr_T(k2)), ANAHTARLAR);
    const eksik = ANAHTARLAR.filter((k2, i) => !r[i] || r[i] === k2 || /^(bk_|b_|ba_)/.test(r[i]));
    const yerTutucu = ANAHTARLAR.filter((k2, i) => tokenler(r[i]) !== tokenler(trT[i]));
    chk(dil + ': ' + ANAHTARLAR.length + ' yeni metin dolu' + (eksik.length ? ' (eksik: ' + eksik.join(',') + ')' : ''), eksik.length === 0);
    chk(dil + ': yer tutucular ({a} {n} {ad}) TR ile ayni' + (yerTutucu.length ? ' (' + yerTutucu.join(',') + ')' : ''), yerTutucu.length === 0);
    await st(p, () => { window.pr_git('lig'); }); await bekle(250);
    const ekran = await st(p, () => { const k2 = document.getElementById('bkBolum'); const d = document.querySelector('.lig-kaydir'); return { tasma: k2.scrollWidth > k2.clientWidth + 1 || d.scrollWidth > d.clientWidth + 1, bk: /\bbk_|\bb_fener|\bba_/.test(k2.textContent), bos: k2.querySelectorAll('small').length === 10 && [...k2.querySelectorAll('small')].every(e => e.textContent.trim().length > 0) }; });
    chk(dil + ': Lig bekci bolumu tasmiyor, anahtar sizmiyor', !ekran.tasma && !ekran.bk && ekran.bos);
  }
  chk('hata yok (13 dil)', p.hata.length === 0); if (p.hata.length) console.log(p.hata.join('\n'));
  await p.browserContext().close();

  // ---- 9) reklam acikken de (kapali iken de) akis ayni; reklamsiz fener + sonuc karti ----
  p = await yeni({ level: 10, enYuksek: 10, stars: {} });
  await coz(p, 10); k = await kartInfo(p);
  chk('reklam yokken: bolge sonu karti + fener + 5 satir tamam', !!k && k.mod === 'sonuc' && /Mekân feneri yandı/.test(k.hediye) && /Saat Kulesi aydınlık/.test(k.hediye));
  chk('hata yok (reklamsiz)', p.hata.length === 0);
  await p.browserContext().close();

  console.log(bad ? ('SONUC: ' + bad + ' HATA') : 'SONUC: HEPSI GECTI');
  await b.close(); process.exit(bad ? 1 : 0);
})().catch(e => { console.error('HATA', e.message); process.exit(1); });
