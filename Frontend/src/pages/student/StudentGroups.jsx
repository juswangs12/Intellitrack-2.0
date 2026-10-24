import {useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import api from '../../services/ApiService';

export function EnrollmentStatus({enrollments}) {
  if (!enrollments) return null;
  const active=enrollments.filter(e=>!e.archived);
  if (!enrollments.length) return <section className="student-enrollment-status"><h2>Awaiting enrollment</h2><p>Your Google account is not linked to a classlist yet. Ask your coordinator to import or link the email you use to sign in.</p></section>;
  if (!active.length) return <section className="student-enrollment-status"><h2>No active semester</h2><p>Your previous records are saved in history. Your next subjects will appear after your coordinator imports the new classlist.</p><Link to="/student/groups">View semester history</Link></section>;
  const unassigned=active.filter(e=>!e.groups.length),empty=active.filter(e=>!e.requirements);
  return <section className="student-enrollment-status"><h2>{active.length} active {active.length===1?'enrollment':'enrollments'}</h2>
    {unassigned.length>0&&<p><strong>No group assigned:</strong> {unassigned.map(e=>e.subject+' · '+e.section).join(', ')}. Ask your coordinator to assign your group before submitting.</p>}
    {empty.length>0&&<p><strong>No requirements yet:</strong> {empty.map(e=>e.subject+' ('+e.term+')').join(', ')}. Deadlines will appear when your coordinator assigns them.</p>}
    {!unassigned.length&&!empty.length&&<p>Your classlist and group are linked. You can track your assigned requirements below.</p>}
    <Link to="/student/groups">View my groups and enrollment</Link></section>;
}

export default function StudentGroups(){
  const [enrollments,setEnrollments]=useState(null),[error,setError]=useState(''),[loading,setLoading]=useState(false),[history,setHistory]=useState(false),[term,setTerm]=useState('');
  const load=async()=>{setLoading(true);setError('');try{setEnrollments(await api.requestJson('/tracking/enrollments'));}catch(e){setError(e.message);}finally{setLoading(false);}};
  useEffect(()=>{load();},[]);
  const scoped=(enrollments||[]).filter(e=>e.archived===history);
  const terms=[...new Set(scoped.map(e=>e.term))].sort().reverse();
  const visible=scoped.filter(e=>!term||e.term===term);
  return <div className="tracking-page student-groups"><header className="tracking-header"><div><h1>My groups & enrollment</h1><p>Your subjects, classmates, and adviser in one place.</p></div><button className="btn btn-secondary" disabled={loading} onClick={load}>Refresh</button></header>
    {error&&<p role="alert" className="tracking-error">{error}</p>}
    <div className="student-group-toolbar"><div className="student-view-switch" aria-label="Enrollment view"><button aria-pressed={!history} onClick={()=>{setHistory(false);setTerm('');}}>Active enrollments</button><button aria-pressed={history} onClick={()=>{setHistory(true);setTerm('');}}>Semester history</button></div><label className="student-term-filter">Academic term<select value={term} onChange={e=>setTerm(e.target.value)}><option value="">All {history?'archived':'active'} terms</option>{terms.map(t=><option key={t} value={t}>{t}</option>)}</select></label></div>
    {loading?<p role="status">Loading your enrollment...</p>:enrollments&&<>
      {!history&&!enrollments.length&&<EnrollmentStatus enrollments={enrollments}/>}
      {!visible.length&&enrollments.length>0&&<div className="tracking-empty"><h2>{history?'No archived semesters':'No active enrollment'}</h2><p>{history?'Finished semesters will appear here.':'Your previous records are available in Semester history.'}</p></div>}
      <div className="student-group-grid">{visible.map(e=><section className="tracking-group student-group-card" key={e.id}><header><div><span className="student-subject">{e.subject} · {e.term}</span><h2>{e.subjectName}</h2><p>Section {e.section}</p></div><span className="tracking-status">{history?'Archived · read-only':'Enrolled'}</span></header>
        {!e.groups.length?<p className="student-inline-note">No group assigned. {history?'This enrollment is archived.':'Contact your coordinator to be assigned to a group.'}</p>:e.groups.map(g=><article className="student-roster" key={g.id}><h3>{g.name}</h3><p>Adviser: <strong>{g.adviser}</strong></p><h4>Group members ({g.members.length})</h4><ul>{g.members.map((m,i)=><li key={i}><span>{m.name}{m.self&&<small className="student-you">You</small>}</span><span>Section {m.section}</span></li>)}</ul></article>)}
        <footer>{!e.requirements?<span>No requirements assigned.</span>:<Link className="btn btn-secondary" to={`/student/submissions?term=${encodeURIComponent(e.term)}&subject=${encodeURIComponent(e.subjectId)}`}>{history?'View past submissions':'View requirements'}</Link>}</footer>
      </section>)}</div>
    </>}
  </div>;
}
