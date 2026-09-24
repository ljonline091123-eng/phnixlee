import {mkdir} from 'node:fs/promises';
import {createRequire} from 'node:module';
import {fileURLToPath,pathToFileURL} from 'node:url';
import path from 'node:path';
const root=path.dirname(fileURLToPath(import.meta.url)); const out=path.join(root,'cards'); await mkdir(out,{recursive:true});
const require=createRequire(import.meta.url); const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright'); const browser=await chromium.launch({headless:true,channel:'msedge'});
const url=pathToFileURL(path.join(root,'index.html')).href; const ids=['real-01-etype','real-02-gullwing','real-03-mustang','real-04-911'];
for(const id of ids){const page=await browser.newPage({viewport:{width:680,height:960},deviceScaleFactor:1}); await page.goto(`${url}?card=${id}`,{waitUntil:'load'}); await page.locator('.card.capture').screenshot({path:path.join(out,`${id}.png`)}); await page.close()}
const overview=await browser.newPage({viewport:{width:2480,height:1400},deviceScaleFactor:1}); await overview.goto(url,{waitUntil:'load'}); await overview.locator('#catalog').screenshot({path:path.join(out,'classic-real-overview.png')}); await overview.close(); await browser.close(); console.log('Rendered 4 real classic-car cards.');
