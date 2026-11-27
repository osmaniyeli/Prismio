const puppeteer=require('puppeteer-core'); const bekle=ms=>new Promise(r=>setTimeout(r,ms));
let bad=0; const chk=(k,v)=>{ if(!v)bad++; console.log((v?'OK   ':'HATA ')+k); };
(async()=>{
  const b=await puppeteer.launch({executablePath:process.env.CHROME_PATH||'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',headless:'new',args:['--no-sandbox']});
  const ctx=await b.createBrowserContext(); const p=await ctx.newPage(); await p.setViewport({width:390,height:844,deviceScaleFactor:2,isMobile:true,hasTouch:true});
  const hata=[]; p.on('pageerror',e=>hata.push(e.message));
  await p.evaluateOnNewDocument(()=>{ localStorage.setItem('prismio_v2',JSON.stringify({_v:2,level:12,enYuksek:12,nasilGordu:1,ogreticiBitti:1,coins:100,lives:5,toplamCozulen:11})); localStorage.setItem('prismio_dil','tr'); });
  await p.goto('file:///'+(process.env.OYUN_HTML||'D:/Prismio/Prismio.html'),{waitUntil:'load'}); await bekle(1500);
  await p.evaluate(()=>{ pr_level(12); }); await bekle(1200);
  chk('"Ayna yerlestir" dugmesi gorunmuyor', await p.evaluate(()=>{const e=document.getElementById('btnYerlestir'); return !e || getComputedStyle(e).display==='none';}));
  chk('alt yazisi ("Bir kare sec...") gorunmuyor', await p.evaluate(()=>{const e=document.getElementById('ogYuvaAlt'); return !e || getComputedStyle(e).display==='none';}));
  chk('"Tum kristalleri aydinlat." gorunmuyor', await p.evaluate(()=>{const e=document.querySelector('#ek-oyun .og-hedef-yazi'); return !e || getComputedStyle(e).display==='none';}));
  chk('hedef gostergesi (kristal noktalari, N/M yandi) hala gorunuyor', await p.evaluate(()=>{const e=document.getElementById('hedefler'); return e.offsetParent!==null && /yandı/.test(e.textContent);}));
  chk('"N ayna kaldi. Isigi yonlendir." yok (yonlendirme satiri bos)', await p.evaluate(()=>!/ayna kaldı|Işığı yönlendir/.test(document.getElementById('ipucu').textContent)));
  chk('kalan ayna sayisi sol kutuda duruyor (x N)', await p.evaluate(()=>/×\s*\d+/.test(document.getElementById('ogKalan').textContent)));
  // bir bos kareye dokun -> ayna, tekrar -> donsun, tekrar -> kalksin
  const bilgi=await p.evaluate(()=>{ const s=document.getElementById('svg').getBoundingClientRect(); return {l:s.left,t:s.top,w:s.width,h:s.height,n:pr_state().n}; });
  const ayna=()=>p.evaluate(()=>Object.keys(pr_state().mirrors).length);
  let hedef=null;
  for(let y=0;y<bilgi.n && !hedef;y++) for(let x=0;x<bilgi.n && !hedef;x++){
    const once=await ayna(); await p.mouse.click(bilgi.l+(x+.5)*bilgi.w/bilgi.n, bilgi.t+(y+.5)*bilgi.h/bilgi.n); await bekle(200);
    if(await ayna()>once){ hedef={x,y}; }
  }
  chk('kareye dokununca ayna yerlesti (dugme olmadan)', !!hedef);
  const cx=bilgi.l+(hedef.x+.5)*bilgi.w/bilgi.n, cy=bilgi.t+(hedef.y+.5)*bilgi.h/bilgi.n;
  chk('aynali karede secim cercevesi var', await p.evaluate(()=>document.querySelectorAll('#svg .og-sec-kutu').length===1));
  await p.mouse.click(cx,cy); await bekle(250);
  chk('2. dokunus: ayna dondu (hala 1 ayna)', await ayna()===1);
  await p.mouse.click(cx,cy); await bekle(250);
  chk('3. dokunus: ayna kaldirildi (0 ayna)', await ayna()===0);
  chk('kaldirinca soluk (hayalet) ayna KALMADI', await p.evaluate(()=>document.querySelectorAll('#svg .og-sec-hayalet').length===0));
  chk('kaldirinca secim cercevesi de kalmadi', await p.evaluate(()=>document.querySelectorAll('#svg .og-sec, #svg .og-sec-kutu').length===0));
  // arka plan silueti
  const bg=await p.evaluate(()=>{ const r=document.documentElement; const cs=getComputedStyle(document.body,'::after'); return {desen:r.getAttribute('data-bolge-desen'), opak:r.style.getPropertyValue('--bolge-opak'), boy:cs.maskSize||cs.webkitMaskSize}; });
  console.log('   arka plan:',JSON.stringify(bg));
  chk('arka plan deseni acik', bg.desen!==null);
  chk('silüet karosu buyudu (176px) ve opaklik arttı (>=.16 acik/.11 koyu)', /176px/.test(bg.boy||'') && parseFloat(bg.opak)>=.11);
  await p.screenshot({path:'tahta_son.png'});
  chk('sayfa hatasi yok', hata.length===0); if(hata.length) console.log(hata);
  await b.close(); console.log(bad?('SONUC: '+bad+' HATA'):'SONUC: HEPSI GECTI');
})().catch(e=>{console.error('HATA',e.message);process.exit(1)});
