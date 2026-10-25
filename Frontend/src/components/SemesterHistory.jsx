import {useCallback,useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import api from '../services/ApiService';
import {progressionLabels} from './progressionLabels';
export default function SemesterHistory({onChanged}) {
  const [data,setData]=useState(null),[term,setTerm]=useState(''),[confirmed,setConfirmed]=useState(false);
  const [busy,setBusy]=useState(false),[message,setMessage]=useState(''),[error,setError]=useState('');
  const load=useCallback(async()=>{const [catalog,subjects,groups]=await Promise.all([api.requestJson('/academic'),api.requestJson('/subjects/with-sections'),api.getGroups()]);setData({catalog,subjects,groups});},[]);
  useEffect(()=>{load().catch(e=>setError(e.message));},[load]);
  const archived=data?.catalog.archivedTerms||[];
  const terms=data?[...new Set([...data.catalog.sections.map(s=>s.term),...data.catalog.groups.map(g=>g.term),...data.catalog.deliverables.map(d=>d.term)].filter(Boolean))].sort().reverse():[];
  const history=archived.includes(term);
  const sections=(data?.subjects||[]).flatMap(s=>(s.sections||[]).filter(c=>c.academicTerm===term).map(c=>({...c,subject:s.code})));
  const groups=(data?.groups||[]).filter(g=>g.academicTerm===term);
  const requirements=(data?.catalog.deliverables||[]).filter(d=>d.term===term);
  const change=async()=>{setBusy(true);setError('');setMessage('');try{await api.requestJson('/academic/semesters',{method:'PUT',body:JSON.stringify({term,archived:!history})});await load();await onChanged();setConfirmed(false);setMessage(history?'Semester reopened. Corrections are now allowed.':'Semester finished. Records are preserved in history. Sync Google Calendar to remove its active reminders.');}catch(e){setError(e.message);}finally{setBusy(false);}};
  return <section className="semester-history"><h2>Semesters & history</h2><p>Finish a semester to preserve its records in history. Import a new classlist to begin the next semester.</p>
    <p><Link to="/coordinator/classlist">Import a new semester classlist</Link></p>
    {error&&<p className="tracking-error" role="alert">{error}</p>}{message&&<p className="tracking-notice" role="status">{message}</p>}
    {!data?<p>Loading semesters...</p>:<><label>Semester<select disabled={busy} value={term} onChange={e=>{setTerm(e.target.value);setConfirmed(false);setMessage('');}}><option value="">Choose a semester</option>{terms.map(t=><option key={t} value={t}>{t} — {archived.includes(t)?'History':'Active'}</option>)}</select></label>
    {!terms.length&&<p>No semesters yet. Import your first classlist.</p>}
    {terms.length>0&&!term&&<div className="tracking-empty"><h3>Select a semester</h3><p>Review its classlists, groups and deadlines. Active semesters can be finished; archived semesters can be reopened for corrections.</p></div>}
    {term&&<><section className="tracking-group"><h3>{term} — {history?'Semester history':'Active semester'}</h3><p>{sections.length} sections · {groups.length} groups · {sections.reduce((n,s)=>n+(s.enrollments||[]).length,0)} enrollments · {requirements.length} requirements</p>
    <p>{history?'These records are read-only. Reopen this semester to correct records or resolve Pending / INC cases.':'Finish when teaching and submissions for this semester are complete. Pending / INC outcomes remain recorded; reopen later if needed. New submissions and academic edits will be blocked while archived.'}</p>
    {!history&&<label><input style={{width:'auto'}} type="checkbox" checked={confirmed} disabled={busy} onChange={e=>setConfirmed(e.target.checked)}/> Finish all subjects in this semester and preserve their records in history.</label>}
    <button className="btn btn-primary" disabled={busy||(!history&&!confirmed)} onClick={change}>{busy?'Saving...':history?'Reopen semester':'Finish semester'}</button>
    <p><Link to={`/coordinator/analytics?term=${encodeURIComponent(term)}`}>View submissions, analytics and export reports</Link></p></section>
    <h3>Classlists</h3>{sections.map(s=><details className="tracking-group" key={s.id}><summary>{s.subject} · {s.section} · {(s.enrollments||[]).length} students</summary><table><thead><tr><th>Student</th><th>Student ID</th><th>Recorded outcome</th></tr></thead><tbody>{(s.enrollments||[]).map(e=><tr key={e.id}><td>{e.fullName}</td><td>{e.studentId}</td><td>{progressionLabels[e.progression||'PENDING']}{e.progressionReason&&` — ${e.progressionReason}`}</td></tr>)}</tbody></table></details>)}
    <h3>Groups & advisers</h3>{groups.map(g=><details className="tracking-group" key={g.id}><summary>{g.title} · {g.subjectCode} · {g.adviserName||'No adviser'}</summary><ul>{(g.students||[]).map(e=><li key={e.id}>{e.fullName} · {e.sectionName}</li>)}</ul></details>)}
    <h3>Requirements & deadlines</h3><ul>{requirements.map(d=><li key={d.id}>{data.catalog.subjects.find(s=>s.id===d.subjectId)?.code} · {d.name} · {d.dueAt?.replace('T',' ')||'No deadline'} (Asia/Manila){!d.active?' — inactive':''}</li>)}</ul>
    </>}</>}
  </section>;
}
