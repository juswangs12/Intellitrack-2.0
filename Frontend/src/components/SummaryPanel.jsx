import {useState} from 'react';
import api from '../services/ApiService';
import WorkspaceHistory from './WorkspaceHistory';
export default function SummaryPanel({parameters='',disabled=false}){
 const [busy,setBusy]=useState(false),[error,setError]=useState(''),[result,setResult]=useState(null),[revision,setRevision]=useState(0);
 const generate=async()=>{setBusy(true);setError('');try{const value=await api.requestJson(`/tracking/summary${parameters?'?'+parameters:''}`,{method:'POST'});setResult(value);setRevision(n=>n+1);}catch(e){setError(e.message);}finally{setBusy(false);}};
 return <section className="tracking-group summary-panel"><h2>AI progress summary</h2><p>A saved snapshot of submission progress and deadlines. Document content is not read.</p><button className="btn btn-primary" disabled={busy||disabled} onClick={generate}>{busy?'Generating...':'Generate summary'}</button>{error&&<p role="alert">{error}</p>}{result&&<div role="status"><strong>{result.source}</strong><p className="summary-text">{result.text}</p><small>{result.notice}</small></div>}<WorkspaceHistory revision={revision}/></section>;
}
