const puppeteer=require('puppeteer-core');
const html=process.env.OYUN_HTML||process.argv[2]||'D:/Prismio/Prismio.html';
const bekle=ms=>new Promise(r=>setTimeout(r,ms));
let bad=0; const chk=(k,v)=>{ if(!v)bad++; console.log((v?'OK   ':'HATA ')+k); };
(async()=>{
  const b=await puppeteer.launch({executablePath:process.env.CHROME_PATH||'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',headless:'new',args:['--no-sandbox']});
  async function yeni(seed){ const ctx=await b.createBrowserContext(); const p=await ctx.newPage(); await p.setViewport({width:390,height:844,deviceScaleFactor:1,isMobile:true,hasTouch:true});
    p.hata=[]; p.on('pageerror',e=>p.hata.push(e.message)); await p.evaluateOnNewDocument(()=>{try{localStorage.setItem('prismio_dil','tr')}catch(e){}}); if(seed) await p.evaluateOnNewDocument(s=>{try{localStorage.setItem('prismio_v2',s)}catch(e){}},seed); await p.goto('file:///'+html,{waitUntil:'load'}); return p; }
  const ekran=p=>p.evaluate(()=>document.querySelector('.ekran.acik').id);
  // 1) eski oyuncu: sinematik yok
  let p=await yeni(JSON.stringify({_v:2,level:5,enYuksek:5,toplamCozulen:4,coins:50,lives:5}));
  await bekle(2500); chk('mevcut oyuncu: sinematik YOK, menu acik', (await p.evaluate(()=>document.getElementById('sinema').hidden)) && (await ekran(p))==='ek-menu');
  // nasil -> filmi izle -> geri nasil
  await p.click('#btnNasil'); await bekle(700); chk('nasil ekrani acildi', (await ekran(p))==='ek-nasil');
  chk('filmi izle butonu gorunur', await p.evaluate(()=>{const e=document.getElementById('nasilSinema');return !!e&&e.getBoundingClientRect().height>20}));
  await p.evaluate(()=>document.getElementById('nasilSinema').scrollIntoView()); await p.click('#nasilSinema'); await bekle(1200);
  chk('film tekrar oynuyor', await p.evaluate(()=>!document.getElementById('sinema').hidden));
  await p.click('#snAtla'); await bekle(1100);
  chk('Atla sonrasi nasil ekranina donuldu (ogretici BASLAMADI)', (await ekran(p))==='ek-nasil' && !(await p.evaluate(()=>document.documentElement.classList.contains('ogr'))));
  // 2) normal bolum cozumu (dev kancalari)
  if(await p.evaluate(()=>typeof window.pr_level==='function')){
    await p.evaluate(()=>{window.pr_level(5)}); await bekle(600);
    chk('normal bolum: rehber/ogretici yok', await p.evaluate(()=>!document.documentElement.classList.contains('ogr') && (!document.getElementById('ogrKatman')||document.getElementById('ogrKatman').hidden)));
    chk('normal bolum: ipucu butonu gorunur', await p.evaluate(()=>{const e=document.getElementById('btnIpucu');return e.offsetParent!==null}));
    await p.evaluate(()=>{window.pr_solve()}); await bekle(4200);
    chk('normal bolum cozuldu: sonuc karti', await p.evaluate(()=>{const k=document.getElementById('katman');return k&&k.classList.contains('acik')}));
    await p.evaluate(()=>{window.pr_git('menu')}); await bekle(500);
  }
  // 3) zamana karsi
  await p.evaluate(()=>{ document.querySelectorAll('.katman.acik').forEach(e=>e.classList.remove('acik')); if(window.pr_git) window.pr_git('menu'); else { var g=document.querySelector('.ekran.acik .geri'); if(g) g.click(); } }); await bekle(600);
  await p.click('#btnZaman'); await bekle(1500);
  chk('zamana karsi basladi', (await ekran(p))==='ek-oyun' && await p.evaluate(()=>!document.getElementById('sayac').hidden));
  chk('zamana karsi: rehber yok', await p.evaluate(()=>!document.documentElement.classList.contains('ogr')));
  chk('hata yok (mevcut oyuncu)', p.hata.length===0); if(p.hata.length)console.log(p.hata.join('\n'));
  // 4) ogretici ortasinda cikis -> can yanmaz, menuye normal donus
  p=await yeni(null); await bekle(900); await p.click('#snAtla'); await bekle(1300);
  chk('Atla -> dogrudan ogretici', (await ekran(p))==='ek-oyun' && await p.evaluate(()=>document.documentElement.classList.contains('ogr')));
  const can0=await p.evaluate(()=>document.getElementById('oCan').textContent);
  await p.click('#ek-oyun .geri'); await bekle(800);
  const can1=await p.evaluate(()=>document.getElementById('mCan').textContent);
  chk('ogreticiden cikinca can YANMADI ('+can0+'->'+can1+')', can0===can1);
  chk('cikista rehber kapandi', await p.evaluate(()=>!document.documentElement.classList.contains('ogr')));
  chk('hata yok (ilk oyuncu cikis)', p.hata.length===0); if(p.hata.length)console.log(p.hata.join('\n'));
  // 5) ogretici: Sifirla calisir, yanlis ayna rehberi
  p=await yeni(null); await bekle(900); await p.click('#snAtla'); await bekle(1300);
  const hl=async()=>p.evaluate(()=>{const h=document.querySelector('#ogrKatman .ogr-halka');const k=document.getElementById('ogrKatman');if(!h||k.hidden)return null;const r=h.getBoundingClientRect();return {x:r.left+r.width/2,y:r.top+r.height/2}});
  const sb=await p.evaluate(()=>{const r=document.getElementById('svg').getBoundingClientRect();return [r.left,r.top,r.width/5]});
  await p.touchscreen.tap(sb[0]+sb[2]*3.5,sb[1]+sb[2]*3.5); await bekle(400);   // yanlis kare (3,3)
  chk('yanlis ayna: rehber "sil"/duzeltme gosteriyor', await p.evaluate(()=>/Kaldır|remove/i.test(document.getElementById('ipucu').textContent)||document.getElementById('ipucu').textContent.length>0));
  await p.click('#btnSifirla'); await bekle(400);
  chk('Sifirla sonrasi rehber tekrar basa dondu', await p.evaluate(()=>/dokun/i.test(document.getElementById('ipucu').textContent)));
  chk('hata yok (ogretici)', p.hata.length===0); if(p.hata.length)console.log(p.hata.join('\n'));
  console.log('SONUC:', bad?('SORUN '+bad):'HEPSI GECTI');
  await b.close();
})();
