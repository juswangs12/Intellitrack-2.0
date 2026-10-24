import {useCallback,useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import api from '../services/ApiService';
export default function AwaitingEnrollment({subjects=[],onChanged}){
  const [students,setStudents]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const [search,setSearch]=useState(''),[choices,setChoices]=useState({}),[busy,setBusy]=useState(null);
  const load=useCallback(async()=>{setLoading(true);setError('');try{setStudents(await api.requestJson('/student-enrollments/awaiting'));}catch(e){setError(e.message);}finally{setLoading(false);}},[]);
  useEffect(()=>{load();},[load]);
  const sections=subjects.flatMap(subject=>(subject.sections||[]).filter(section=>!section.semesterArchived).map(section=>({...section,label:`${subject.code} · ${section.section}`})));
  const unlinked=sections.flatMap(section=>(section.enrollments||[]).filter(e=>!e.userId).map(e=>({...e,label:`${e.fullName} · ${e.email||'No email'} · ${section.label}`})));
  const save=async student=>{
    const [kind,id]=(choices[student.id]||'').split(':');if(!id)return;
    setBusy(student.id);setError('');
    try{
      if(kind==='link')await api.linkUserToEnrollment(Number(id),student.id);
      else await api.addStudentManually({userId:student.id,classSectionId:Number(id)});
      await load();await onChanged();
    }catch(e){setError(e.message);}finally{setBusy(null);}
  };
  const visible=students.filter(s=>`${s.name} ${s.email} ${s.studentNumber||''}`.toLowerCase().includes(search.toLowerCase()));
  return <section className="tracking-group" aria-label="Awaiting enrollment">
    <div className="tracking-header"><div><h2>Awaiting enrollment ({students.length})</h2><p>Students who have signed in with Google but are not linked to any classlist. Once enrolled or linked, they leave this list.</p></div><button className="btn btn-secondary" disabled={loading||busy!==null} onClick={load}>Refresh awaiting list</button></div>
    {error&&<p role="alert" className="tracking-error">{error}</p>}
    <label className="form-label">Find an unenrolled student<input className="form-input" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Name, Google email, or student number"/></label>
    {loading?<p role="status">Loading Google sign-ins...</p>:!students.length?<p>No Google students are awaiting enrollment.</p>:!visible.length?<p>No students match your search.</p>:<>
    {!sections.length&&<p><Link to="/coordinator/setup?tab=subjects">Create a subject and section</Link> before adding these students.</p>}
    <div className="tracking-table"><table><thead><tr><th>Student</th><th>Google email</th><th>Enrollment action</th></tr></thead><tbody>{visible.map(student=><tr key={student.id}><td><strong>{student.name}</strong><small>{student.studentNumber||'Student number not provided'}</small></td><td>{student.email}</td><td><label className="form-label">Place {student.name}<select className="form-select" aria-label={`Enrollment for ${student.name}`} value={choices[student.id]||''} disabled={busy!==null} onChange={e=>setChoices({...choices,[student.id]:e.target.value})}><option value="">Choose an action</option>{unlinked.length>0&&<optgroup label="Link to an existing imported record">{unlinked.map(e=><option key={e.id} value={`link:${e.id}`}>{e.label}</option>)}</optgroup>}<optgroup label="Add as a new enrollment">{sections.map(s=><option key={s.id} value={`section:${s.id}`}>{s.label}</option>)}</optgroup></select></label><p>For an imported student, verify the name and email and link their existing record.</p><button className="btn btn-primary" disabled={busy!==null||!choices[student.id]} onClick={()=>save(student)}>{busy===student.id?'Saving...':'Confirm enrollment'}</button></td></tr>)}</tbody></table></div></>}
  </section>;
}
