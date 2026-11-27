const puppeteer=require('puppeteer-core');
const html=process.env.OYUN_HTML||process.argv[2]||'D:/Prismio/Prismio.html';
const diller=['tr','en','es','pt','de','fr','ru','zhTW','zhCN','ja','ko','it','az'];
const boyutlar=[[360,640],[390,844]];
const bekle=ms=>new Promise(r=>setTimeout(r,ms));
(async()=>{
  const b=await puppeteer.launch({executablePath:process.env.CHROME_PATH||'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',headless:'new',args:['--no-sandbox']});
  let sorun=0;
  for(const dil of diller){ for(const [w,h] of boyutlar){
    const ctx=await b.createBrowserContext(); const p=await ctx.newPage();
    await p.setViewport({width:w,height:h,deviceScaleFactor:1,isMobile:true,hasTouch:true});
    const hata=[]; p.on('pageerror',e=>hata.push(e.message));
    await p.evaluateOnNewDocument(d=>{try{localStorage.setItem('prismio_dil',d)}catch(e){}},dil);
    await p.goto('file:///'+html,{waitUntil:'load'}); await bekle(500);
    const r=[];
    // sinematik: her sahnede yazi sigiyor mu, anahtar kacagi var mi
    for(let i=0;i<5;i++){
      await bekle(i===0?2000:3600);
      const x=await p.evaluate(()=>{
        const k=document.getElementById('sinema'); const o=[];
        const on=k.querySelector('.sn.on'); if(!on) return ['SAHNE YOK'];
        const y=on.querySelector('.sn-yazi'); const nk=k.querySelector('.sn-nokta').getBoundingClientRect();
        const txt=(on.textContent||'').trim();
        if(/^sin_|ogr_|slogan/.test(txt)||/sin_\d|sin_basla|sin_atla/.test(txt)) o.push('ANAHTAR KACAGI: '+txt.slice(0,40));
        if(y){ const R=y.getBoundingClientRect(); if(R.left<0||R.right>innerWidth+1) o.push('YAZI TAŞIYOR'); if(R.bottom>nk.top-2) o.push('YAZI NOKTALARA BİNİYOR '+Math.round(R.bottom)+'>'+Math.round(nk.top)); if(R.top<innerHeight*0.11) o.push('YAZI ÜST BANDA GİRİYOR'); }
        const bs=on.querySelector('#snBasla'); if(bs){ const R=bs.getBoundingClientRect(); if(R.right>innerWidth||R.left<0||R.bottom>innerHeight*0.89) o.push('BAŞLA DÜĞMESİ SIĞMIYOR'); if(bs.scrollWidth>bs.clientWidth+1) o.push('BAŞLA KIRPILIYOR'); }
        const at=document.getElementById('snAtla'); if(at&&!at.hidden){const R=at.getBoundingClientRect(); if(R.right>innerWidth) o.push('ATLA TAŞIYOR');}
        return o;
      });
      x.forEach(m=>r.push(`sahne${i+1}: ${m}`));
    }
    if(process.env.PNG && (w===390)) await p.screenshot({path:`${process.env.PNG}/dl_${dil}_son.png`});
    await p.click('#snBasla'); await bekle(1300);
    const o=await p.evaluate(()=>{
      const o=[]; const ip=document.getElementById('ipucu'), ust=document.getElementById('ogrUst');
      const tx=(ip.textContent+' '+(ust?ust.textContent:''));
      if(/ogr_|sin_/.test(tx)) o.push('ANAHTAR KACAGI: '+tx.slice(0,50));
      const kutu=document.getElementById('tahtakutu').getBoundingClientRect(), R=ip.getBoundingClientRect(), U=ust.getBoundingClientRect();
      if(R.bottom>innerHeight) o.push('REHBER EKRAN DIŞI');
      if(R.top<kutu.bottom-1) o.push('REHBER TAHTAYA BİNİYOR');
      if(U.bottom>kutu.top+2) o.push('ÜST METİN TAHTAYA BİNİYOR '+Math.round(U.bottom)+'>'+Math.round(kutu.top));
      const bt=[...document.querySelectorAll('.oyunalt .btn')].filter(e=>e.offsetParent); if(bt.some(e=>e.getBoundingClientRect().bottom>innerHeight)) o.push('ALT DÜĞME EKRAN DIŞI');
      return o;
    });
    o.forEach(m=>r.push('öğretici: '+m));
    if(process.env.PNG && (w===390)) await p.screenshot({path:`${process.env.PNG}/dl_${dil}_ogr.png`});
    hata.forEach(m=>r.push('JS HATA: '+m));
    console.log(`${dil} ${w}x${h}: `+(r.length?'\n   '+r.join('\n   '):'temiz')); sorun+=r.length;
    await ctx.close();
  }}
  await b.close(); console.log('TOPLAM SORUN:',sorun);
})();
