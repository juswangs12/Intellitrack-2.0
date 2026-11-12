import CalendarConnection from '../../components/CalendarConnection';
import {useCallback,useEffect,useMemo,useState} from 'react';
import {Link,useSearchParams} from 'react-router-dom';
import api from '../../services/ApiService';
import {dateTime,query} from '../TrackingDashboard';
import DashboardProgress from '../../components/DashboardProgress';
import CoordinatorSetupSteps from '../../components/CoordinatorSetupSteps';
import '../../styles/Tracking.css';

export const manilaToday=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Manila',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
const dueTime=value=>new Date(/[zZ]|[+-]\d{2}:\d{2}$/.test(value)?value:value+'+08:00').getTime();
export function overviewScope(data,term,subject){
  const matches=(t,s)=>!(data.catalog.archivedTerms||[]).includes(t)&&(!term||(term==='legacy'?!t:t===term))&&(!subject||String(s)===subject);
  const requirements=data.catalog.deliverables.filter(d=>d.active&&matches(d.term,d.subjectId));
  const groups=data.catalog.groups.filter(g=>matches(g.term,g.subjectId));
  const sections=data.catalog.sections.filter(s=>matches(s.term,s.subjectId));
  const sectionIds=new Set(sections.map(s=>s.id));
  const enrollments=data.subjects.flatMap(s=>(s.sections||[]).filter(c=>sectionIds.has(c.id)).flatMap(c=>c.enrollments||[]));
  const rows=data.rows.filter(r=>matches(r.academicTerm,r.subjectId));
  const overdue=rows.filter(r=>r.status==='OVERDUE');
  return {requirements,groups,sections,enrollments,rows,overdue};
}
function Deadline({item,catalog,rows,term,subject}){
  const assigned=rows.filter(r=>r.deliverableId===item.id);
  const missing=assigned.filter(r=>!r.submissionId).length;
  return <article className="deadline-item"><div><strong>{item.name}</strong><span className="scope-note">{catalog.subjects.find(s=>s.id===item.subjectId)?.code||'Unassigned subject'} · {item.term||'Term not assigned'}</span><span className="scope-note">{dateTime(item.dueAt)}</span></div><div><span className="scope-note">{assigned.length?`${missing} of ${assigned.length} submissions pending`:'No enrolled students yet'}</span><Link to={`/coordinator/analytics?${query({term:term||item.term||'legacy',subject:subject||item.subjectId,deliverable:item.id})}`}>View submissions</Link></div></article>;
}
export default function CoordinatorOverview({calendar=false}){
  const [params,setParams]=useSearchParams();const term=params.get('term')||'',subject=params.get('subject')||'';
  const [data,setData]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const [month,setMonth]=useState(()=>manilaToday().slice(0,7)),[selectedDay,setSelectedDay]=useState(manilaToday);
  const [createOpen,setCreateOpen]=useState(false),[createBusy,setCreateBusy]=useState(false),[createError,setCreateError]=useState(''),[createNotice,setCreateNotice]=useState('');
  const [createForm,setCreateForm]=useState({name:'',term:'',subject:'',dueAt:''});
  const [expandedDeadline,setExpandedDeadline]=useState(null);
  const load=useCallback(async()=>{setLoading(true);setError('');try{
    const [catalog,tracking,subjects,awaiting]=await Promise.all([api.requestJson('/academic'),api.requestJson('/tracking'),api.requestJson('/subjects/with-sections'),api.requestJson('/student-enrollments/awaiting')]);
    setData({catalog,rows:tracking.rows,subjects,awaiting});
  }catch(e){setError(e.message);}finally{setLoading(false);}},[]);
  useEffect(()=>{load();},[load]);
  const scope=useMemo(()=>data?overviewScope(data,term,subject):null,[data,term,subject]);
  const filters={term,subject};
  const setFilter=(key,value)=>setParams(query({...filters,[key]:value}));
  const monthEvents=scope?.requirements.filter(d=>d.dueAt?.slice(0,7)===month)||[];
  const terms=data?[...new Set([...data.catalog.sections.map(s=>s.term),...data.catalog.deliverables.map(d=>d.term),...data.catalog.groups.map(g=>g.term)].filter(t=>t&&!(data.catalog.archivedTerms||[]).includes(t)))].sort().reverse():[];
  const upcoming=scope?.requirements.filter(d=>d.dueAt&&dueTime(d.dueAt)>=Date.now()).sort((a,b)=>dueTime(a.dueAt)-dueTime(b.dueAt))||[];
  const [year,monthNumber]=month.split('-').map(Number);
  const days=new Date(Date.UTC(year,monthNumber,0)).getUTCDate(),start=new Date(Date.UTC(year,monthNumber-1,1)).getUTCDay();
  const shiftMonth=delta=>{const date=new Date(Date.UTC(year,monthNumber-1+delta,1));const value=date.toISOString().slice(0,7);setMonth(value);setSelectedDay(value+'-01');};
  const today=manilaToday();
  const openCreateForDay=()=>{setCreateError('');setCreateNotice('');setCreateForm({name:'',term:term&&term!=='legacy'&&terms.includes(term)?term:'',subject:subject||'',dueAt:`${selectedDay}T23:59`});setCreateOpen(true);};
  const editDeadline=item=>{setCreateError('');setCreateNotice('');setCreateForm({...item,subject:String(item.subjectId),dueAt:item.dueAt?.slice(0,16)||'',reason:'',active:item.active!==false});setCreateOpen(true);};
  const saveRequirement=async e=>{e.preventDefault();setCreateBusy(true);setCreateError('');setCreateNotice('');try{
    await api.requestJson('/academic/requirements',{method:'POST',body:JSON.stringify({id:createForm.id||null,name:createForm.name,term:createForm.term,subjectId:Number(createForm.subject),dueAt:createForm.dueAt,reason:createForm.reason||'',active:createForm.active!==false})});
    setCreateOpen(false);await load();setCreateNotice(createForm.id?'Requirement updated.':'Requirement created. It now appears on this calendar and in student workspaces.');
  }catch(e){setCreateError(e.message);}finally{setCreateBusy(false);}};
  const deleteDeadline=async item=>{const hasSubmitted=scope.rows.some(r=>r.deliverableId===item.id&&r.submissionId);if(!window.confirm(hasSubmitted?`Requirement "${item.name}" has submission history. Delete is blocked to preserve it. Open edit to archive instead?`:`Delete "${item.name}"? This cannot be undone.`))return;if(hasSubmitted){setCreateForm({...item,subject:String(item.subjectId),dueAt:item.dueAt?.slice(0,16)||'',reason:'',active:false});setCreateOpen(true);return;}try{await api.requestJson(`/academic/requirements/${item.id}`,{method:'DELETE'});setCreateNotice('Requirement deleted.');await load();}catch(e){setCreateNotice(`Could not delete requirement: ${e.message}`);}};
  const archiveDeadline=async item=>{if(!window.confirm(`Archive "${item.name}"? Its submission history will remain available.`))return;try{await api.requestJson('/academic/requirements',{method:'POST',body:JSON.stringify({id:item.id,name:item.name,term:item.term,subjectId:item.subjectId,dueAt:item.dueAt,active:false,reason:'Archived from deadline calendar'})});setCreateNotice('Requirement archived; its history is preserved.');await load();}catch(e){setCreateNotice(`Could not archive requirement: ${e.message}`);}};
  return <div className="tracking-page">
    <header className="tracking-header"><div><p className="tracking-eyebrow">COORDINATOR</p><h1>{calendar?'Deadline calendar':'Coordinator dashboard'}</h1><p>{calendar?'All active requirement deadlines, shown in Asia/Manila time.':'Upcoming deadlines, submission follow-ups, and academic setup at a glance.'}</p></div><button className="btn btn-secondary" disabled={loading} onClick={load}>Refresh overview</button></header>
    {error&&<p role="alert" className="tracking-error">{error} Use Refresh overview to retry.</p>}
    {loading?<p role="status">Loading coordinator overview...</p>:!error&&data&&<>
      <div className="tracking-filters"><label>Academic term<select value={term} onChange={e=>setFilter('term',e.target.value)}><option value="">All terms</option>{term==='legacy'&&<option value="legacy">Unassigned term</option>}{terms.map(t=><option key={t}>{t}</option>)}</select></label><label>Subject<select value={subject} onChange={e=>setFilter('subject',e.target.value)}><option value="">All subjects</option>{data.catalog.subjects.map(s=><option key={s.id} value={s.id}>{s.code} — {s.name}</option>)}</select></label></div>
      {!data.catalog.subjects.length&&<CoordinatorSetupSteps/>}
      {calendar?<>
        <CalendarConnection endpoint="/google/calendar/coordinator/sync"/>
        <div className="calendar-toolbar"><button className="btn btn-secondary" onClick={()=>shiftMonth(-1)}>Previous month</button><label>Month<input type="month" value={month} onChange={e=>{if(/^\d{4}-\d{2}$/.test(e.target.value)){setMonth(e.target.value);setSelectedDay(e.target.value+'-01');}}}/></label><button className="btn btn-secondary" onClick={()=>{setMonth(today.slice(0,7));setSelectedDay(today);}}>Today</button><button className="btn btn-secondary" onClick={()=>shiftMonth(1)}>Next month</button></div>
        <p>{monthEvents.length} deadlines this month. Select a day to see its requirements.</p>
        <div className="deadline-calendar" aria-label="Monthly deadline calendar">
          {['Sun','Mon','Tue','Wed','Thu','Fri','Sat'].map(d=><div key={d} className="calendar-weekday">{d}</div>)}
          {Array.from({length:start},(_,i)=><div key={`blank-${i}`} className="calendar-blank"/>)}
          {Array.from({length:days},(_,i)=>{const day=`${month}-${String(i+1).padStart(2,'0')}`;const events=monthEvents.filter(d=>d.dueAt.slice(0,10)===day);return <button key={day} className={`calendar-day ${day===today?'is-today':''}`} aria-pressed={day===selectedDay} aria-label={`${day}, ${events.length} deadlines`} onClick={()=>setSelectedDay(day)}><strong>{i+1}</strong>{events.map(d=><span key={d.id}>{data.catalog.subjects.find(s=>s.id===d.subjectId)?.code}: {d.name}</span>)}</button>;})}
        </div>
        <section className="tracking-group calendar-deadlines"><div className="calendar-section-heading"><div><h2>Deadlines on {selectedDay}</h2><p>Review progress, open details, or edit a requirement.</p></div><button className="btn btn-primary" onClick={openCreateForDay}>Create deadline</button></div>{monthEvents.filter(d=>d.dueAt.slice(0,10)===selectedDay).length?<div className="deadline-card-grid">{monthEvents.filter(d=>d.dueAt.slice(0,10)===selectedDay).map(d=>{const assigned=scope.rows.filter(r=>r.deliverableId===d.id),submitted=assigned.filter(r=>r.submissionId).length,missing=assigned.length-submitted,expanded=expandedDeadline===d.id,subj=data.catalog.subjects.find(s=>s.id===d.subjectId);return <article className="calendar-deadline-card" key={d.id}><div className="calendar-deadline-main"><div><span className="tracking-eyebrow">{subj?.code||'Subject'} | {d.term||'Term not assigned'}</span><h3>{d.name}</h3><p>Due {dateTime(d.dueAt)} | {assigned.length?`${submitted} submitted, ${missing} pending of ${assigned.length}`:'No students assigned'}</p></div><div className="calendar-deadline-actions"><button className="btn btn-secondary" aria-expanded={expanded} onClick={()=>setExpandedDeadline(expanded?null:d.id)}>{expanded?'Hide details':'Details'}</button><button className="btn btn-secondary" onClick={()=>editDeadline(d)}>Edit</button><button className="btn btn-secondary" onClick={()=>archiveDeadline(d)}>Archive</button><button className="btn btn-danger" onClick={()=>deleteDeadline(d)}>Delete</button></div></div>{expanded&&<div className="calendar-deadline-details"><p><strong>Subject:</strong> {subj?.name||'Unassigned'} ({subj?.code||'—'})</p><p><strong>Academic term:</strong> {d.term||'Unassigned'}</p><p><strong>Assignment scope:</strong> All enrolled students in sections for this subject and term.</p><p><strong>Submission progress:</strong> {submitted} submitted, {missing} pending, {assigned.length} assigned</p><Link to={`/coordinator/analytics?${query({term:term||d.term||'legacy',subject:subject||d.subjectId,deliverable:d.id})}`}>View submissions</Link></div>}</article>})}</div>:<p>No deadlines on this day in the selected scope.</p>}</section>
        <section className="tracking-group calendar-all-deadlines"><h2>All active deadlines in this scope</h2><p>Includes past and future deadlines. Use “View date” to jump to its month and day.</p>{scope.requirements.length?<div className="tracking-table"><table><thead><tr><th>Requirement</th><th>Subject</th><th>Academic term</th><th>Deadline (Asia/Manila)</th><th>Progress</th><th>Actions</th></tr></thead><tbody>{scope.requirements.slice().sort((a,b)=>dueTime(a.dueAt)-dueTime(b.dueAt)).map(d=>{const subj=data.catalog.subjects.find(s=>s.id===d.subjectId),day=d.dueAt?.slice(0,10)||'',assigned=scope.rows.filter(r=>r.deliverableId===d.id),missing=assigned.filter(r=>!r.submissionId).length;return <tr key={d.id}><td>{d.name}</td><td>{subj?.code||'—'}</td><td>{d.term||'Unassigned'}</td><td>{dateTime(d.dueAt)}</td><td>{assigned.length?`${missing} of ${assigned.length} submissions pending`:'No enrolled students yet'}</td><td><div className="calendar-deadline-actions"><button className="btn btn-secondary" onClick={()=>{if(day){setMonth(day.slice(0,7));setSelectedDay(day);}}}>View date</button><button className="btn btn-secondary" onClick={()=>editDeadline(d)}>Edit</button></div></td></tr>;})}</tbody></table></div>:<p>No active requirements in this scope.</p>}</section>
        {createNotice&&<p role="status" className="tracking-notice">{createNotice}</p>}
        {createOpen&&<form className="tracking-form calendar-create-form" onSubmit={saveRequirement}>
          <h2>{createForm.id?'Edit requirement':'Create a requirement from the calendar'}</h2><p>It applies to every section in the selected subject and term. Deadline times use Asia/Manila.</p>
          {createError&&<p role="alert" className="tracking-error">{createError}</p>}
          <label>Requirement name<input required maxLength={255} value={createForm.name} onChange={e=>setCreateForm({...createForm,name:e.target.value})}/></label>
          <label>Academic term<select required value={createForm.term} onChange={e=>setCreateForm({...createForm,term:e.target.value})}><option value="">Choose a term</option>{terms.map(t=><option key={t} value={t}>{t}</option>)}</select></label>
          <label>Subject<select required value={createForm.subject} onChange={e=>setCreateForm({...createForm,subject:e.target.value})}><option value="">Choose a subject</option>{data.catalog.subjects.map(s=><option key={s.id} value={s.id}>{s.code} — {s.name}</option>)}</select></label>
          <label>Deadline (Asia/Manila)<input required type="datetime-local" value={createForm.dueAt} onChange={e=>setCreateForm({...createForm,dueAt:e.target.value})}/></label>
          {createForm.id&&<><label>Reason for deadline change<input value={createForm.reason||''} onChange={e=>setCreateForm({...createForm,reason:e.target.value})} placeholder="Required when changing the deadline"/></label><label className="calendar-active-toggle"><input type="checkbox" checked={createForm.active!==false} onChange={e=>setCreateForm({...createForm,active:e.target.checked})}/> Active requirement</label></>}
          <div className="tracking-actions"><button className="btn btn-primary" disabled={createBusy||!terms.length||!data.catalog.subjects.length}>{createBusy?'Saving…':createForm.id?'Save changes':'Create requirement'}</button><button className="btn btn-secondary" type="button" disabled={createBusy} onClick={()=>setCreateOpen(false)}>Cancel</button></div>
        </form>}
      </>:<>
        <DashboardProgress rows={scope.rows} title={scope.overdue.length?"Prioritize missing overdue work":"Monitor this academic scope"} description="Use the subject and term filters above to focus your overview. Follow up on missing work or prepare the next requirement." actions={[{to:`/coordinator/analytics?${query({...filters,status:'OVERDUE'})}`,label:"Overdue work"},{to:"/coordinator/calendar",label:"Manage deadlines"},{to:"/coordinator/classlist",label:"Import classlist"}]}/>
        <div className="overview-cards">{[['Enrolled students',new Set(scope.enrollments.map(e=>e.userId?`u:${e.userId}`:`e:${e.id}`)).size],['Groups',scope.groups.length],['Active requirements',scope.requirements.length],['Overdue submissions',scope.overdue.length]].map(([label,count])=><Link className="tracking-card" to={label==='Enrolled students'?'/coordinator/classlist-view':label==='Groups'?'/coordinator/groups':label==='Active requirements'?'/coordinator/calendar':`/coordinator/analytics?${query({...filters,status:'OVERDUE'})}`} key={label}><span>{label}</span><strong>{count}</strong></Link>)}</div>
        <p className="tracking-help">Overdue counts student × requirement assignments that have not been submitted. Late submissions are shown in detailed analytics.</p>
        <div className="setup-columns"><section className="tracking-group"><h2>Upcoming deadlines</h2>{upcoming.length?upcoming.slice(0,6).map(d=><Deadline key={d.id} item={d} catalog={data.catalog} rows={scope.rows} {...filters}/>):<p>No upcoming deadlines in this scope. <Link to="/coordinator/calendar">Assign a requirement</Link>.</p>}<Link to={`/coordinator/calendar?${query(filters)}`}>View the full calendar</Link></section>
        <section className="tracking-group"><h2>Enrollment follow-ups</h2><ul className="attention-list">{data.awaiting.length>0&&<li><Link to="/coordinator/classlist-view">{data.awaiting.length} Google students awaiting enrollment</Link><small>Across all subjects and terms</small></li>}{scope.enrollments.some(e=>!e.userId)&&<li><Link to="/coordinator/classlist-view">{scope.enrollments.filter(e=>!e.userId).length} enrollment records without linked accounts</Link></li>}{scope.rows.some(r=>!r.groupId)&&<li><Link to="/coordinator/groups">{new Set(scope.rows.filter(r=>!r.groupId).map(r=>r.enrollmentId)).size} enrolled students without groups</Link></li>}</ul>{!data.awaiting.length&&!scope.enrollments.some(e=>!e.userId)&&!scope.rows.some(r=>!r.groupId)&&<p>No enrollment issues need attention in this scope.</p>}<Link to="/coordinator/classlist-view">View students and sections</Link></section></div>
        <section className="tracking-group"><h2>Overdue follow-ups</h2>{scope.overdue.length?<><p>{new Set(scope.overdue.map(r=>r.groupId).filter(Boolean)).size} groups have missing overdue submissions.</p><div className="tracking-table"><table><thead><tr><th>Student</th><th>Group / subject</th><th>Requirement</th><th>Deadline</th></tr></thead><tbody>{scope.overdue.slice().sort((a,b)=>dueTime(a.dueAt)-dueTime(b.dueAt)).slice(0,10).map(r=><tr key={`${r.enrollmentId}:${r.deliverableId}`}><td>{r.studentName}<small>{r.section}</small></td><td>{r.group}<small>{r.subject}</small></td><td>{r.deliverable}</td><td>{dateTime(r.dueAt)}</td></tr>)}</tbody></table></div><Link to={`/coordinator/analytics?${query({...filters,status:'OVERDUE'})}${term===''?'&term=':''}`}>View all {scope.overdue.length} overdue assignments</Link></>:<p>No missing overdue submissions in the selected scope.</p>}</section>
      </>}
    </>}
  </div>;
}
