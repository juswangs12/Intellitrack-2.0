import {useCallback,useEffect,useState} from 'react';
import {Link,useSearchParams} from 'react-router-dom';
import {useAuth} from '../context/AuthContext';
import api from '../services/ApiService';
import {dateTime} from './TrackingDashboard';
import AcademicTermFields,{initialTerm} from '../components/AcademicTermFields';
import '../styles/Tracking.css';
import SemesterHistory from '../components/SemesterHistory';
import '../styles/Coordinator.css';
const emptyForm=()=>({name:'',dueAt:'',reason:'',active:true});
export default function AcademicSetup(){
  const {user}=useAuth();const coordinator=user?.role==='coordinator';
  const [params]=useSearchParams();const tab=['subjects','requirements','semesters'].includes(params.get('tab'))?params.get('tab'):'subjects';
  const [catalog,setCatalog]=useState({subjects:[],sections:[],groups:[],deliverables:[]});
  const [term,setTerm]=useState(initialTerm),[subject,setSubject]=useState('');
  const [subjectForm,setSubjectForm]=useState({code:'',name:''});
  const [form,setForm]=useState(emptyForm),[section,setSection]=useState('');
  const [history,setHistory]=useState(null),[loading,setLoading]=useState(true);
  const [busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState('');
  const load=useCallback(async()=>setCatalog(await api.requestJson('/academic')),[]);
  useEffect(()=>{load().catch(e=>setError(e.message)).finally(()=>setLoading(false));},[load]);
  const run=async(fn,message)=>{setBusy(true);setError('');setNotice('');try{await fn();await load();setNotice(message);}catch(e){setError(e.message);}finally{setBusy(false);}};
  const save=e=>{e.preventDefault();run(async()=>{await api.requestJson('/academic/requirements',{method:'POST',body:JSON.stringify({...form,subjectId:Number(subject),term})});setForm(emptyForm());},'Requirement saved. Students see the updated deadline; Calendar updates on their next sync.');};
  const edit=d=>{setSubject(String(d.subjectId||''));setTerm(d.term||term);setForm({...d,dueAt:d.dueAt?.slice(0,16)||'',reason:''});setHistory(null);window.scrollTo({top:0,behavior:'smooth'});};
  const requirements=catalog.deliverables.filter(d=>String(d.subjectId)===subject&&(!d.term||d.term===term));
  const sections=catalog.sections.filter(s=>String(s.subjectId)===subject&&s.term===term);
  const semesterArchived=(catalog.archivedTerms||[]).includes(term);
  const changeScope=()=>{setForm(emptyForm());setHistory(null);};
  return <div className="tracking-page"><header className="tracking-header"><div><h1>Academic setup</h1><p>Review imported subjects, set deadlines, and manage semester history.</p></div></header>
    {error&&<p role="alert" className="tracking-error">{error}</p>}{notice&&<p role="status" className="tracking-notice">{notice}</p>}
    <nav className="setup-tabs" aria-label="Academic setup pages">{[['subjects','Subjects & sections'],['requirements','Requirements & deadlines'],['semesters','Semesters & history']].map(([id,label])=><Link key={id} to={`?tab=${id}`} aria-current={tab===id?'page':undefined}>{label}</Link>)}</nav>
    {tab==='semesters'&&<SemesterHistory onChanged={load}/>}
    {tab!=='semesters'&&(loading?<p role="status">Loading academic setup...</p>:<>
    <fieldset className="tracking-filters" disabled={busy||!!form.id}><AcademicTermFields value={term} onChange={v=>{setTerm(v);changeScope();}}/><label>Subject<select value={subject} onChange={e=>{setSubject(e.target.value);changeScope();}}><option value="">Choose a subject</option>{catalog.subjects.map(s=><option key={s.id} value={s.id}>{s.code} — {s.name}</option>)}</select></label></fieldset>
    {semesterArchived&&<p className="tracking-notice">This semester is archived. View or reopen it in <Link to="?tab=semesters">Semesters &amp; history</Link>.</p>}
    {tab==='subjects'&&<>
    <section className="tracking-group"><h2>Subjects & sections</h2><p>Subjects and sections are created when you import a classlist.</p>
    {!catalog.subjects.length?<p>No subjects yet. {coordinator&&<Link to="/coordinator/classlist">Import your first classlist</Link>}</p>:<div className="tracking-table"><table><thead><tr><th>Subject</th><th>Sections in {term}</th></tr></thead><tbody>{catalog.subjects.filter(s=>!subject||String(s.id)===subject).map(s=><tr key={s.id}><td><strong>{s.code}</strong><small>{s.name}</small></td><td>{catalog.sections.filter(c=>c.subjectId===s.id&&c.term===term).map(c=>c.name).join(', ')||'No sections in this term'}</td></tr>)}</tbody></table></div>}</section>
    <details className="manual-setup"><summary>Add manually</summary><p>Use this only when a subject or section is missing from your import.</p><div className="setup-columns"><form className="tracking-form" onSubmit={e=>{e.preventDefault();run(async()=>{const result=await api.requestJson('/academic/subjects',{method:'POST',body:JSON.stringify(subjectForm)});setSubject(String(result.id));setSubjectForm({code:'',name:''});},'Subject created. Add its sections next.');}}><h2>Create a subject</h2><p>Create each subject once; reuse it in later school years.</p><label>Subject code<input required maxLength={255} placeholder="IT332" value={subjectForm.code} onChange={e=>setSubjectForm({...subjectForm,code:e.target.value})}/></label><label>Subject name<input required maxLength={255} placeholder="Capstone and Research 1" value={subjectForm.name} onChange={e=>setSubjectForm({...subjectForm,name:e.target.value})}/></label><button disabled={busy||semesterArchived} className="btn btn-primary">Create subject</button></form>
    <form className="tracking-form" onSubmit={e=>{e.preventDefault();run(async()=>{await api.requestJson('/academic/sections',{method:'POST',body:JSON.stringify({subjectId:Number(subject),section,term})});setSection('');},'Section created. Import a classlist or add students in Students & sections.');}}><h2>Create a section</h2><p>{subject?'This section belongs to the selected subject and term.':'Create or choose a subject above to continue.'}</p><label>Section name<input required maxLength={255} value={section} onChange={e=>setSection(e.target.value)} placeholder="BSIT4A"/></label><button className="btn btn-primary" disabled={busy||semesterArchived||!subject}>Create section</button><ul className="setup-list">{sections.map(s=><li key={s.id}>{s.name}</li>)}</ul>{subject&&!sections.length&&<p>No sections in this subject and term yet.</p>}</form></div></details>
    {catalog.sections.some(s=>!s.term)&&<details className="tracking-group"><summary>Resolve sections without an academic term</summary><p>Assigns connected groups and sections together. Conflicting assignments are rejected.</p>{catalog.sections.filter(s=>!s.term).map(s=><p key={s.id}>{s.name} <button className="btn btn-secondary" disabled={busy||semesterArchived} onClick={()=>run(()=>api.requestJson(`/academic/sections/${s.id}/term`,{method:'PUT',body:JSON.stringify({term})}),'Term assigned.')}>Assign to {term}</button></p>)}</details>}</>}
    {tab==='requirements'&&<>{!subject?<div className="tracking-empty">Choose a subject above. Subjects are created from the imported classlist.</div>:<><form className="tracking-form" onSubmit={save}><h2>{form.id?'Edit requirement / extend deadline':'Assign a requirement'}</h2><p>One deadline applies to every section of this subject and term. Times are Asia/Manila.</p><label>Requirement name<input required maxLength={255} value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label><label>Deadline (Asia/Manila)<input required type="datetime-local" value={form.dueAt} onChange={e=>setForm({...form,dueAt:e.target.value})}/></label>{form.id&&<><label>Reason for deadline change or extension<input value={form.reason} onChange={e=>setForm({...form,reason:e.target.value})}/></label><label><span><input style={{width:'auto'}} type="checkbox" checked={form.active} onChange={e=>setForm({...form,active:e.target.checked})}/> Active requirement</span></label></>}<div className="tracking-actions"><button disabled={busy||semesterArchived} className="btn btn-primary">Save requirement</button>{form.id&&<button type="button" className="btn btn-secondary" onClick={()=>setForm(emptyForm())}>Cancel edit</button>}</div></form>
    <h2>Requirements for the selected subject and term</h2>{!requirements.length?<p className="tracking-empty">No requirements yet. Add the first one above.</p>:<div className="tracking-table"><table><thead><tr><th>Requirement</th><th>Term</th><th>Deadline</th><th>Actions</th></tr></thead><tbody>{requirements.map(d=><tr key={d.id}><td>{d.name}{!d.active?' (archived)':''}</td><td>{d.term||'Unassigned'}</td><td>{dateTime(d.dueAt)}</td><td><button disabled={busy||semesterArchived} className="btn btn-secondary" onClick={()=>edit(d)}>Edit</button> <button className="btn btn-secondary" disabled={busy||semesterArchived} onClick={()=>run(async()=>setHistory({name:d.name,rows:await api.requestJson(`/academic/requirements/${d.id}/history`)}),'History loaded.')}>History</button></td></tr>)}</tbody></table></div>}
    {history&&<section className="tracking-group"><h2>Deadline history: {history.name}</h2>{history.rows.length?<ul>{history.rows.map(log=><li key={log.id}>{dateTime(log.timestamp)} — {log.details}</li>)}</ul>:<p>No recorded changes yet.</p>}<button className="btn btn-secondary" onClick={()=>setHistory(null)}>Close history</button></section>}</>}</>}
    </>)}
  </div>;
}
