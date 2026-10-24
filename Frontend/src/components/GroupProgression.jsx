import {useState} from 'react';
import {Link} from 'react-router-dom';
import api from '../services/ApiService';
import {progressionLabels} from './EnrollmentProgression';
export default function GroupProgression({groups,onChanged}) {
  const [id,setId]=useState(''),[status,setStatus]=useState('ELIGIBLE'),[reason,setReason]=useState('');
  const [busy,setBusy]=useState(false),[message,setMessage]=useState('');
  const group=groups.find(g=>String(g.id)===id),members=group?.students||[];
  const counts=Object.keys(progressionLabels).map(key=>[key,members.filter(e=>(e.progression||'PENDING')===key).length]);
  const run=async(fn,text)=>{setBusy(true);setMessage('');try{await fn();await onChanged();setMessage(text);}catch(e){setMessage(e.message);}finally{setBusy(false);}};
  return <section className="tracking-group"><h2>Group progression</h2><p>Review a group once and apply its outcome to all current members. Removed members are excluded.</p>
    <label>Group<select disabled={busy} value={id} onChange={e=>{setId(e.target.value);setReason('');setMessage('');}}><option value="">Choose a group</option>{groups.filter(g=>g.students?.length).map(g=><option key={g.id} value={g.id}>{g.code} · {g.subjectCode} · {g.academicTerm}</option>)}</select></label>
    {group&&<><p>{counts.filter(([,n])=>n).map(([key,n])=>`${n} ${progressionLabels[key]}`).join(' · ')}</p>
    <div className="tracking-filters"><label>Group outcome<select disabled={busy} value={status} onChange={e=>setStatus(e.target.value)}>{Object.entries(progressionLabels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label><label>Reason for the group<input disabled={busy} maxLength={255} value={reason} onChange={e=>setReason(e.target.value)}/></label></div>
    <p>This replaces the progression status of all {members.length} current members, including any previous individual decisions.</p>
    <button type="button" className="btn btn-primary" disabled={busy||!reason.trim()} onClick={()=>run(()=>api.requestJson(`/academic/groups/${id}/progression`,{method:'PUT',body:JSON.stringify({status,reason})}),`Updated all ${members.length} members.`)}>Apply to whole group</button>
    <p>Outcomes are recorded here. Import the next semester classlist to create its enrollments.</p><p><Link to="/coordinator/setup?tab=semesters">Finish semester / Semester history</Link> · <Link to="/coordinator/classlist-view">Manage individual exceptions</Link></p></>}
    {message&&<p role="status">{message}</p>}
  </section>;
}
