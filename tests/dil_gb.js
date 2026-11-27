const puppeteer=require('puppeteer-core'); const bekle=ms=>new Promise(r=>setTimeout(r,ms));
const DILLER=['tr','en','es','pt','de','fr','ru','zhTW','zhCN','ja','ko','it','az'];
let sorun=0;
(async()=>{
  const b=await puppeteer.launch({executablePath:process.env.CHROME_PATH||'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',headless:'new',args:['--no-sandbox']});
  for(const dil of DILLER){
    const ctx=await b.createBrowserContext(); const p=await ctx.newPage(); await p.setViewport({width:360,height:640,deviceScaleFactor:1,isMobile:true,hasTouch:true});
    const hata=[]; p.on('pageerror',e=>hata.push(e.message));
    const eski=Date.now()-8*864e5;
    await p.evaluateOnNewDocument((d,e)=>{ localStorage.setItem('prismio_v2',JSON.stringify({_v:2,level:12,enYuksek:12,nasilGordu:1,ogreticiBitti:1,coins:100,lives:5,toplamCozulen:11})); localStorage.setItem('prismio_dil',d); localStorage.setItem('prismio_gb',JSON.stringify({ilk:e,son:e}));
      window.PrismioNative={uygulamaSurumu:function(){return '0.11.3 (1)';},geriBildirimGonder:function(){},playDegerlendir:function(){}}; },dil,eski);
    await p.goto('file:///'+(process.env.OYUN_HTML||'D:/Prismio/Prismio.html'),{waitUntil:'load'}); await bekle(1200);
    const olc=()=>p.evaluate(()=>{ const k=document.querySelector('#katman .kart'); const r=k.getBoundingClientRect(); const bt=[...k.querySelectorAll('button')].filter(x=>x.offsetParent!==null).map(x=>{const q=x.getBoundingClientRect(); return {sol:q.left, sag:q.right, alt:q.bottom, tasma:x.scrollWidth>x.clientWidth+1};}); return {kartTasma:k.scrollWidth>k.clientWidth+1, kartSag:r.right, kartAlt:r.bottom, vw:innerWidth, vh:innerHeight, bt, baslik:document.getElementById('kBaslik').textContent}; });
    // 1) ayarlar satiri
    await p.evaluate(()=>pr_git('ayar')); await bekle(500);
    const satir=await p.evaluate(()=>{ const e=document.getElementById('btnGeriBildirim'); const r=e.getBoundingClientRect(); return {gorunur:e.offsetParent!==null, sag:r.right, vw:innerWidth, tasma:e.scrollWidth>e.clientWidth+1, ad:e.querySelector('.od-s-ad').textContent}; });
    // 2) form
    await p.click('#btnGeriBildirim'); await bekle(400);
    const f=await olc();
    await p.evaluate(()=>{ document.getElementById('katman').classList.remove('acik'); });
    // 3) degerlendirme karti
    await p.evaluate(()=>{ pr_level(12); }); await bekle(500); await p.evaluate(()=>{ pr_solve(); }); await bekle(4200);
    await p.evaluate(()=>{ document.getElementById('katman').classList.remove('acik'); pr_git('harita'); }); await bekle(1500);
    const k=await olc();
    const kusur=[];
    if(!satir.gorunur||satir.sag>satir.vw+1||satir.tasma) kusur.push('ayar satiri tasiyor');
    for(const [ad,o] of [['form',f],['kart',k]]){
      if(o.kartTasma) kusur.push(ad+': kart yatay tasiyor');
      if(o.kartSag>o.vw+1) kusur.push(ad+': kart ekran disi');
      if(o.bt.some(x=>x.sol<-1||x.sag>o.vw+1)) kusur.push(ad+': dugme ekran disi');
      if(o.bt.some(x=>x.tasma)) kusur.push(ad+': dugme metni tasiyor');
      if(o.bt.some(x=>x.alt>o.vh+1)) kusur.push(ad+': dugme ekran altinda');
    }
    if(!/./.test(f.baslik)) kusur.push('form basligi bos');
    if(hata.length) kusur.push('sayfa hatasi');
    console.log(dil.padEnd(5), kusur.length?('SORUN: '+kusur.join(', ')):'temiz', '| baslik:',f.baslik,'| ayar:',satir.ad);
    sorun+=kusur.length; await ctx.close();
  }
  await b.close(); console.log('TOPLAM SORUN:',sorun);
})().catch(e=>{console.error('HATA',e.message);process.exit(1)});
