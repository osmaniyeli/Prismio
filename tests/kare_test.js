const puppeteer=require('puppeteer-core'); const bekle=ms=>new Promise(r=>setTimeout(r,ms));
let bad=0; const chk=(k,v)=>{ if(!v)bad++; console.log((v?'OK   ':'HATA ')+k); };
(async()=>{
  const b=await puppeteer.launch({executablePath:process.env.CHROME_PATH||'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',headless:'new',args:['--no-sandbox']});
  const boyutlar=[[360,640],[360,740],[390,844],[412,915],[768,1024]];
  for(const [w,h] of boyutlar){
    const ctx=await b.createBrowserContext(); const p=await ctx.newPage(); await p.setViewport({width:w,height:h,deviceScaleFactor:2,isMobile:true,hasTouch:true});
    const hata=[]; p.on('pageerror',e=>hata.push(e.message));
    await p.evaluateOnNewDocument(()=>{ localStorage.setItem('prismio_v2',JSON.stringify({_v:2,level:12,enYuksek:13,nasilGordu:1,ogreticiBitti:1,coins:72,lives:4,toplamCozulen:12})); localStorage.setItem('prismio_dil','tr'); });
    await p.goto('file:///'+(process.env.OYUN_HTML||'D:/Prismio/Prismio.html'),{waitUntil:'load'}); await bekle(1200);
    await p.evaluate(()=>{ pr_level(13); }); await bekle(1200);
    const o=await p.evaluate(()=>{ const e=document.getElementById('ek-oyun'), a=document.querySelector('#ek-oyun .oyunalt'); const r=a.getBoundingClientRect(); const bt=[...document.querySelectorAll('#ek-oyun .og-arac-btn')].map(x=>{const q=x.getBoundingClientRect(); return [Math.round(q.width),Math.round(q.height)];}); const ik=document.querySelector('#ek-oyun .oyunalt'); const sonrasi=getComputedStyle(ik,'::after').height; const sc=document.scrollingElement; return {vh:innerHeight, altaKalan:Math.round(innerHeight-r.bottom), btn:bt, ayrilan:sonrasi, tasma:Math.max(0,sc.scrollHeight-innerHeight), yatay:sc.scrollWidth>innerWidth}; });
    console.log(`${w}x${h}:`,JSON.stringify(o));
    chk(`${w}x${h}: sayfa dikey tasmiyor`, o.tasma<=2); chk(`${w}x${h}: yatay tasma yok`, !o.yatay);
    chk(`${w}x${h}: butonlar kare`, o.btn.every(x=>Math.abs(x[0]-x[1])<=2));
    chk(`${w}x${h}: sayfa hatasi yok`, hata.length===0);
    if(w===390) await p.screenshot({path:'kare_390.png'}); if(w===360&&h===640) await p.screenshot({path:'kare_360.png'});
    await ctx.close();
  }
  await b.close(); console.log(bad?('SONUC: '+bad+' HATA'):'SONUC: HEPSI GECTI');
})().catch(e=>{console.error('HATA',e.message);process.exit(1)});
