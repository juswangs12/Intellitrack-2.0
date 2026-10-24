import {useState} from 'react';
import api from '../services/ApiService';
export const progressionLabels={PENDING:'Pending / INC',ELIGIBLE:'Eligible to proceed',REPEAT:'Repeat subject'};
export default function EnrollmentProgression({enrollment,sections,onChanged}) {
  const [status,setStatus]=useState(enrollment.progression||'PENDING');
  const [reason,setReason]=useState('');
  const [busy,setBusy]=useState(false),[message,setMessage]=useState(''),[history,setHistory]=useState(null);
  const run=async(fn,text)=>{setBusy(true);setMessage('');try{await fn();await onChanged();setMessage(text);}catch(e){setMessage(e.message);}finally{setBusy(false);}};
  return <details><summary>{progressionLabels[enrollment.progression||'PENDING']} · Individual exception</summary>
    {enrollment.removedFromGroup&&<p>Removed from group. Progression is assessed separately.</p>}
    {enrollment.progressionReason&&<p>Last reason: {enrollment.progressionReason}</p>}
    <label>Progression<select value={status} disabled={busy} onChange={e=>setStatus(e.target.value)}>{Object.entries(progressionLabels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
    <label>Reason<input maxLength={255} value={reason} disabled={busy} onChange={e=>setReason(e.target.value)}/></label>
    <button type="button" className="btn btn-secondary btn-sm" disabled={busy||!reason.trim()} onClick={()=>run(()=>api.requestJson(`/academic/enrollments/${enrollment.id}/progression`,{method:'PUT',body:JSON.stringify({status,reason})}),'Progression saved.')}>Save progression</button>
    <button type="button" className="btn btn-secondary btn-sm" disabled={busy} onClick={()=>run(async()=>setHistory(await api.requestJson(`/academic/enrollments/${enrollment.id}/history`)),'History loaded.')}>Progression history</button>
    {message&&<p role="status">{message}</p>}{history&&<ul>{history.map((h,i)=><li key={i}>{h.timestamp}: {h.details}</li>)}</ul>}
  </details>;
}
