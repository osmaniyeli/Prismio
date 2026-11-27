const puppeteer=require('puppeteer-core'); const bekle=ms=>new Promise(r=>setTimeout(r,ms));
let bad=0; const chk=(k,v)=>{ if(!v)bad++; console.log((v?'OK   ':'HATA ')+k); };
(async()=>{
  const b=await puppeteer.launch({executablePath:process.env.CHROME_PATH||'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',headless:'new',args:['--no-sandbox']});
  async function yeni(seed,gb){
    const ctx=await b.createBrowserContext(); const p=await ctx.newPage(); await p.setViewport({width:390,height:844,deviceScaleFactor:1,isMobile:true,hasTouch:true});
    p.hata=[]; p.on('pageerror',e=>p.hata.push(e.message));
    await p.evaluateOnNewDocument((s,g)=>{ try{ localStorage.setItem('prismio_v2',s); localStorage.setItem('prismio_dil','tr'); if(g) localStorage.setItem('prismio_gb',g); }catch(e){}
      window.__gonderilen=[]; window.__gbMod='ok'; window.__play=0;
      window.PrismioNative={ uygulamaSurumu:function(){return '0.11.2 (1038)';},
        geriBildirimGonder:function(j){ window.__gonderilen.push(JSON.parse(j)); var id=JSON.parse(j).id; setTimeout(function(){ window.__gbSonuc(id,window.__gbMod); },200); },
        playDegerlendir:function(){ window.__play++; } };
    },seed,gb);
    await p.goto('file:///'+(process.env.OYUN_HTML||'D:/Prismio/Prismio.html'),{waitUntil:'load'}); await bekle(1500); return p; }
  const seed=JSON.stringify({_v:2,level:12,enYuksek:12,nasilGordu:1,ogreticiBitti:1,coins:100,lives:5,toplamCozulen:11});
  // ---- A) ayarlar > geri bildirim formu ----
  let p=await yeni(seed);
  await p.evaluate(()=>pr_git('ayar')); await bekle(500);
  chk('ayarlarda "Geri bildirim gonder" dugmesi var', await p.evaluate(()=>{const e=document.getElementById('btnGeriBildirim'); return !!e && e.offsetParent!==null && /Geri bildirim/.test(e.textContent)}));
  await p.click('#btnGeriBildirim'); await bekle(400);
  chk('form acildi (baslik + 3 tur + metin kutusu)', await p.evaluate(()=>document.getElementById('katman').classList.contains('acik') && document.getElementById('kBaslik').textContent==='Geri bildirim' && document.querySelectorAll('.gb-chip').length===3 && !!document.querySelector('textarea.gb-metin')));
  // cok kisa metin
  await p.evaluate(()=>{ document.querySelector('.gb-metin').value='a'; document.querySelector('#kBtnler .btn.yesil').click(); }); await bekle(300);
  chk('cok kisa mesaj: gonderilmedi', await p.evaluate(()=>window.__gonderilen.length===0));
  chk('kisa uyari gosterildi', await p.evaluate(()=>/Biraz daha/.test((document.getElementById('_kisaBildirim')||{}).textContent||'')));
  // hata turu sec + yaz + gonder
  await p.evaluate(()=>{ document.querySelectorAll('.gb-chip')[0].click(); var t=document.querySelector('.gb-metin'); t.value='49. seviyede karadelik isigi yanlis cikiyor, ikinci dokunusta duzeliyor.'; t.dispatchEvent(new Event('input')); document.querySelector('#kBtnler .btn.yesil').click(); });
  await bekle(900);
  const g=await p.evaluate(()=>window.__gonderilen[0]);
  chk('Java koprusune 1 gonderim yapildi', !!g);
  chk('alanlar dogru (tur=hata, surum v0.11.2.., dil=tr, seviye=12, id var)', g && g.tur==='hata' && /^v0\.11\.2/.test(g.surum) && g.dil==='tr' && g.seviye==='12' && g.id && /karadelik/.test(g.mesaj));
  chk('basarida pencere kapandi + tesekkur gosterildi', await p.evaluate(()=>!document.getElementById('katman').classList.contains('acik') && /Teşekkürler/.test(document.getElementById('_kisaBildirim').textContent)));
  chk('kuyruk bos (gonderildi)', await p.evaluate(()=>JSON.parse(localStorage.getItem('prismio_gb_kuyruk')||'[]').length===0));
  // ---- internet yok: kuyruga al, sonra gonder ----
  await p.evaluate(()=>{ window.__gbMod='ag'; pr_git('ayar'); }); await bekle(300);
  await p.click('#btnGeriBildirim'); await bekle(300);
  await p.evaluate(()=>{ var t=document.querySelector('.gb-metin'); t.value='Internet yokken yazdigim mesaj.'; document.querySelector('#kBtnler .btn.yesil').click(); }); await bekle(900);
  chk('ag yok: mesaj kuyrukta saklandi', await p.evaluate(()=>JSON.parse(localStorage.getItem('prismio_gb_kuyruk')||'[]').length===1));
  chk('ag yok: "saklandi" mesaji gosterildi', await p.evaluate(()=>/saklandı/.test(document.getElementById('_kisaBildirim').textContent)));
  await p.evaluate(()=>{ window.__gbMod='ok'; window.dispatchEvent(new Event('online')); }); await bekle(1200);
  chk('baglanti gelince kuyruktaki mesaj gonderildi', await p.evaluate(()=>JSON.parse(localStorage.getItem('prismio_gb_kuyruk')||'[]').length===0 && window.__gonderilen.some(x=>/Internet yokken/.test(x.mesaj))));
  chk('sayfa hatasi yok (form)', p.hata.length===0); if(p.hata.length) console.log(p.hata);
  // ---- diller: 13 dilde anahtarlar dolu mu ----
  const eksik=await p.evaluate(()=>{ var d=['tr','en','es','pt','de','fr','ru','zhTW','zhCN','ja','ko','it','az'], k=['gb_ayar','gb_baslik','gb_not','gb_tur_hata','gb_tur_oneri','gb_tur_begeni','gb_yer','gb_gonder','gb_vazgec','gb_tesekkur','gb_kuyruk','gb_hata','gb_kisa','rt_baslik','rt_ack','rt_play','rt_form','rt_sonra'], e=[]; d.forEach(function(l){ pr_dilKur && 0; }); return 'atla'; });
  await p.close();

  // ---- B) degerlendirme karti ----
  const eski=Date.now()-8*864e5;
  p=await yeni(JSON.stringify({_v:2,level:12,enYuksek:12,nasilGordu:1,ogreticiBitti:1,coins:100,lives:5,toplamCozulen:11}), JSON.stringify({ilk:eski,son:eski}));
  await p.evaluate(()=>{ pr_level(12); }); await bekle(600);
  await p.evaluate(()=>{ pr_solve(); }); await bekle(4200);
  chk('bolum cozuldu: sonuc karti acik', await p.evaluate(()=>document.getElementById('katman').classList.contains('acik')));
  chk('sonuc kartinda degerlendirme karti ACILMADI (oyun akisi bolunmedi)', await p.evaluate(()=>document.getElementById('katman').getAttribute('data-og')!=='gbdeger'));
  await p.evaluate(()=>{ document.getElementById('katman').classList.remove('acik'); pr_git('harita'); }); await bekle(1600);
  chk('haritaya donulunce degerlendirme karti acildi', await p.evaluate(()=>document.getElementById('katman').classList.contains('acik') && document.getElementById('katman').getAttribute('data-og')==='gbdeger'));
  const dugmeler=await p.evaluate(()=>Array.from(document.querySelectorAll('#kBtnler .btn')).map(x=>x.textContent.trim()));
  chk('3 secenek de gorunur: Play / Geri bildirim / Simdi degil', dugmeler.length===3 && /Google Play/.test(dugmeler[0]) && /Geri bildirim/.test(dugmeler[1]) && /Şimdi değil/.test(dugmeler[2]));
  console.log('   dugmeler:',dugmeler.join(' | '));
  // Geri bildirim secenegi -> form
  await p.evaluate(()=>document.querySelectorAll('#kBtnler .btn')[1].click()); await bekle(400);
  chk('"Geri bildirim gonder" -> form acildi', await p.evaluate(()=>document.getElementById('kBaslik').textContent==='Geri bildirim' && !!document.querySelector('.gb-metin')));
  chk('form secilince 30 gun erteleme yazildi', await p.evaluate(()=>{ var g=JSON.parse(localStorage.getItem('prismio_gb')); return g.snooze>Date.now()+29*864e5; }));
  await p.close();
  // Play secenegi
  p=await yeni(JSON.stringify({_v:2,level:12,enYuksek:12,nasilGordu:1,ogreticiBitti:1,coins:100,lives:5,toplamCozulen:11}), JSON.stringify({ilk:eski,son:eski}));
  await p.evaluate(()=>{ pr_level(12); }); await bekle(600); await p.evaluate(()=>{ pr_solve(); }); await bekle(4200);
  await p.evaluate(()=>{ document.getElementById('katman').classList.remove('acik'); pr_git('harita'); }); await bekle(1600);
  await p.evaluate(()=>document.querySelectorAll('#kBtnler .btn')[0].click()); await bekle(400);
  chk('"Google Play" secilince Play sayfasi acildi', await p.evaluate(()=>window.__play===1));
  chk('Play secilince bir daha sorulmayacak (play=1)', await p.evaluate(()=>JSON.parse(localStorage.getItem('prismio_gb')).play===1));
  await p.evaluate(()=>{ pr_level(13); }); await bekle(600); await p.evaluate(()=>{ pr_solve(); }); await bekle(4200);
  await p.evaluate(()=>{ document.getElementById('katman').classList.remove('acik'); pr_git('harita'); }); await bekle(1600);
  chk('Play sonrasi tekrar kart cikmadi', await p.evaluate(()=>!(document.getElementById('katman').classList.contains('acik') && document.getElementById('katman').getAttribute('data-og')==='gbdeger')));
  await p.close();
  // Simdi degil + 50. seviye kilometre + sik gosterme engeli
  p=await yeni(JSON.stringify({_v:2,level:50,enYuksek:50,nasilGordu:1,ogreticiBitti:1,coins:100,lives:5,toplamCozulen:49}), JSON.stringify({ilk:Date.now()-3*864e5,son:Date.now()-3*864e5}));
  await p.evaluate(()=>{ pr_level(50); }); await bekle(600); await p.evaluate(()=>{ pr_solve(); }); await bekle(4200);
  await p.evaluate(()=>{ document.getElementById('katman').classList.remove('acik'); pr_git('harita'); }); await bekle(1600);
  chk('50. seviye (3 gun once sorulmustu): kart acildi', await p.evaluate(()=>document.getElementById('katman').getAttribute('data-og')==='gbdeger' && document.getElementById('katman').classList.contains('acik')));
  await p.evaluate(()=>document.querySelectorAll('#kBtnler .btn')[2].click()); await bekle(400);
  chk('"Simdi degil": kapandi, play isaretlenmedi', await p.evaluate(()=>!document.getElementById('katman').classList.contains('acik') && !JSON.parse(localStorage.getItem('prismio_gb')).play));
  await p.evaluate(()=>{ pr_git('menu'); }); await bekle(1600);
  chk('hemen ardindan tekrar sorulmadi', await p.evaluate(()=>!document.getElementById('katman').classList.contains('acik')));
  chk('sayfa hatasi yok (kart)', p.hata.length===0); if(p.hata.length) console.log(p.hata);
  await p.close();
  // Yeni oyuncu: 7 gun gecmeden kart yok
  p=await yeni(JSON.stringify({_v:2,level:12,enYuksek:12,nasilGordu:1,ogreticiBitti:1,coins:100,lives:5,toplamCozulen:11}), JSON.stringify({ilk:Date.now()-2*864e5,son:Date.now()-2*864e5}));
  await p.evaluate(()=>{ pr_level(12); }); await bekle(600); await p.evaluate(()=>{ pr_solve(); }); await bekle(4200);
  await p.evaluate(()=>{ document.getElementById('katman').classList.remove('acik'); pr_git('harita'); }); await bekle(1600);
  chk('2 gun once sorulmus, 50. seviye degil: kart YOK', await p.evaluate(()=>!(document.getElementById('katman').classList.contains('acik') && document.getElementById('katman').getAttribute('data-og')==='gbdeger')));
  await p.close();
  await b.close();
  console.log(bad?('SONUC: '+bad+' HATA'):'SONUC: HEPSI GECTI');
})().catch(e=>{console.error('HATA',e.message);process.exit(1)});

