import {useEffect,useState} from 'react';
import api from '../services/ApiService';
const when=value=>new Date(value+'+08:00').toLocaleString('en-PH',{timeZone:'Asia/Manila',dateStyle:'medium',timeStyle:'short'});
export default function WorkspaceHistory({kind='summary',revision=0}){
 const [items,setItems]=useState([]),[error,setError]=useState('');
 const [attempt,setAttempt]=useState(0);
 useEffect(()=>{let active=true;setError('');api.requestJson(kind==='report'?'/google/reports/history':`/tracking/${kind}/history`).then(r=>{if(active)setItems(Array.isArray(r)?r:[]);}).catch(e=>{if(active)setError(e.message);});return()=>{active=false;};},[kind,revision,attempt]);
 const title=kind==='summary'?'Saved summaries':kind==='report'?'Exported reports':'Import history';
 return <section className="workspace-history"><details className="history-list"><summary>{title} ({items.length})</summary><p className="tracking-help">{kind==='import'?'Your last 50 completed imports, including partial imports.':kind==='report'?'Open your saved exports again. Each entry records the export time; the workbook may have been updated since.':'Your last 50 summaries are saved with their date and filters. Older snapshots may differ from current progress. Only scopes you can still access are shown.'}</p>
 {error&&<p role="alert">{error} <button className="btn btn-secondary" onClick={()=>setAttempt(x=>x+1)}>Retry history</button></p>}
 {!error&&!items.length&&<p>No saved {kind==='summary'?'summaries':kind==='report'?'exports':'imports'} yet.</p>}
 {items.map(item=><details key={item.id}><summary>{when(item.createdAt)}{kind==='report'?' - Export':kind==='import'?' - Import':` - ${item.result?.source||'Summary'}`}</summary>{item.scope&&<p>{Object.entries(item.scope).filter(([,v])=>v).map(([k,v])=>`${k}: ${v}`).join(' | ')||'All authorized work'}</p>}{kind==='import'?<p>{item.details}</p>:kind==='report'?<a className="btn btn-secondary" href={item.result?.url} target="_blank" rel="noreferrer">Open exported report</a>:<><p className="summary-text">{item.result?.text}</p><small>{item.result?.notice}</small></>}</details>)}
 </details>
 {kind==='report'&&items[0]?.result?.url&&<a className="btn btn-primary" href={items[0].result.url} target="_blank" rel="noreferrer">Open latest export</a>}
 </section>;
}
