import {useCallback,useEffect,useMemo,useState} from 'react';
import {Link,useSearchParams} from 'react-router-dom';
import api from '../../services/ApiService';
import {dateTime,query,connectGoogle} from '../TrackingDashboard';
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
  const [googleBusy,setGoogleBusy]=useState(false),[googleNotice,setGoogleNotice]=useState(''),[googleError,setGoogleError]=useState(''),[calendarUrl,setCalendarUrl]=useState('');
  const googleAction=async(sync)=>{setGoogleBusy(true);setGoogleNotice('');setGoogleError('');try{
    if(!sync){await connectGoogle('calendar');return;}
    const status=await api.requestJson('/google/status');
    if(!status.calendar){await connectGoogle('calendar');return;}
    const result=await api.requestJson('/google/calendar/coordinator/sync',{method:'POST'});
    setGoogleNotice(`${result.synced} deadlines synced to IntelliTrack Deadlines in Google Calendar.`);setCalendarUrl(result.calendarUrl);
  }catch(e){setGoogleError(e.message);}finally{setGoogleBusy(false);}};
  const [month,setMonth]=useState(()=>manilaToday().slice(0,7)),[selectedDay,setSelectedDay]=useState(manilaToday);
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
  return <div className="tracking-page">
    <header className="tracking-header"><div><p className="tracking-eyebrow">COORDINATOR</p><h1>{calendar?'Deadline calendar':'Coordinator dashboard'}</h1><p>{calendar?'All active requirement deadlines, shown in Asia/Manila time.':'Upcoming deadlines, submission follow-ups, and academic setup at a glance.'}</p></div><button className="btn btn-secondary" disabled={loading} onClick={load}>Refresh overview</button></header>
    {error&&<p role="alert" className="tracking-error">{error} Use Refresh overview to retry.</p>}
    {loading?<p role="status">Loading coordinator overview...</p>:!error&&data&&<>
      <div className="tracking-filters"><label>Academic term<select value={term} onChange={e=>setFilter('term',e.target.value)}><option value="">All terms</option>{term==='legacy'&&<option value="legacy">Unassigned term</option>}{terms.map(t=><option key={t}>{t}</option>)}</select></label><label>Subject<select value={subject} onChange={e=>setFilter('subject',e.target.value)}><option value="">All subjects</option>{data.catalog.subjects.map(s=><option key={s.id} value={s.id}>{s.code} — {s.name}</option>)}</select></label></div>
      {!data.catalog.subjects.length&&<CoordinatorSetupSteps/>}
      {calendar?<>
        <section className="tracking-group calendar-connection"><h2>Google Calendar</h2><p>Connect once for automatic updates from all active semesters. Manual sync is available for an immediate refresh.</p>
          <div className="tracking-actions"><button className="btn btn-secondary" disabled={googleBusy} onClick={()=>googleAction(false)}>Connect / reconnect Google Calendar</button><button className="btn btn-primary" disabled={googleBusy} onClick={()=>googleAction(true)}>{googleBusy?'Connecting / syncing…':'Sync all deadlines'}</button>{calendarUrl&&<a href={calendarUrl} target="_blank" rel="noopener noreferrer">Open Google Calendar</a>}</div>
          <details className="report-help"><summary>Connection and reminder details</summary><p className="tracking-help">After granting Google permission, the production worker syncs your calendar automatically. Background updates depend on the scheduled worker reaching the backend. Events use Asia/Manila time, with reminders one day and one hour before the deadline. Google Calendar edits do not change IntelliTrack deadlines. Page filters do not limit the sync.</p></details>
          {googleNotice&&<p role="status">{googleNotice}</p>}{googleError&&<p role="alert" className="tracking-error">{googleError}</p>}
        </section>
        <div className="calendar-toolbar"><button className="btn btn-secondary" onClick={()=>shiftMonth(-1)}>Previous month</button><label>Month<input type="month" value={month} onChange={e=>{if(/^\d{4}-\d{2}$/.test(e.target.value)){setMonth(e.target.value);setSelectedDay(e.target.value+'-01');}}}/></label><button className="btn btn-secondary" onClick={()=>{setMonth(today.slice(0,7));setSelectedDay(today);}}>Today</button><button className="btn btn-secondary" onClick={()=>shiftMonth(1)}>Next month</button></div>
        <p>{monthEvents.length} deadlines this month. Select a day to see its requirements.</p>
        <div className="deadline-calendar" aria-label="Monthly deadline calendar">
          {['Sun','Mon','Tue','Wed','Thu','Fri','Sat'].map(d=><div key={d} className="calendar-weekday">{d}</div>)}
          {Array.from({length:start},(_,i)=><div key={`blank-${i}`} className="calendar-blank"/>)}
          {Array.from({length:days},(_,i)=>{const day=`${month}-${String(i+1).padStart(2,'0')}`;const events=monthEvents.filter(d=>d.dueAt.slice(0,10)===day);return <button key={day} className={`calendar-day ${day===today?'is-today':''}`} aria-pressed={day===selectedDay} aria-label={`${day}, ${events.length} deadlines`} onClick={()=>setSelectedDay(day)}><strong>{i+1}</strong>{events.map(d=><span key={d.id}>{data.catalog.subjects.find(s=>s.id===d.subjectId)?.code}: {d.name}</span>)}</button>;})}
        </div>
        <section className="tracking-group"><h2>Deadlines on {selectedDay}</h2>{monthEvents.filter(d=>d.dueAt.slice(0,10)===selectedDay).length?monthEvents.filter(d=>d.dueAt.slice(0,10)===selectedDay).map(d=><Deadline key={d.id} item={d} catalog={data.catalog} rows={scope.rows} {...filters}/>):<p>No deadlines on this day in the selected scope.</p>}</section>
        <section className="tracking-group"><h2>Month agenda</h2>{monthEvents.length?monthEvents.slice().sort((a,b)=>dueTime(a.dueAt)-dueTime(b.dueAt)).map(d=><Deadline key={d.id} item={d} catalog={data.catalog} rows={scope.rows} {...filters}/>):<p>No deadlines this month. Choose another month or add a requirement in Academic setup.</p>}</section>
      </>:<>
        <DashboardProgress rows={scope.rows} title={scope.overdue.length?"Prioritize missing overdue work":"Monitor this academic scope"} description="Use the subject and term filters above to focus your overview. Follow up on missing work or prepare the next requirement." actions={[{to:`/coordinator/analytics?${query({...filters,status:'OVERDUE'})}`,label:"Overdue work"},{to:"/coordinator/setup?tab=requirements",label:"Manage deadlines"},{to:"/coordinator/classlist",label:"Import classlist"}]}/>
        <div className="overview-cards">{[['Enrolled students',new Set(scope.enrollments.map(e=>e.userId?`u:${e.userId}`:`e:${e.id}`)).size],['Groups',scope.groups.length],['Active requirements',scope.requirements.length],['Overdue submissions',scope.overdue.length]].map(([label,count])=><div className="tracking-card" key={label}><span>{label}</span><strong>{count}</strong></div>)}</div>
        <p className="tracking-help">Overdue counts student × requirement assignments that have not been submitted. Late submissions are shown in detailed analytics.</p>
        <div className="setup-columns"><section className="tracking-group"><h2>Upcoming deadlines</h2>{upcoming.length?upcoming.slice(0,6).map(d=><Deadline key={d.id} item={d} catalog={data.catalog} rows={scope.rows} {...filters}/>):<p>No upcoming deadlines in this scope. <Link to="/coordinator/setup?tab=requirements">Assign a requirement</Link>.</p>}<Link to={`/coordinator/calendar?${query(filters)}`}>View the full calendar</Link></section>
        <section className="tracking-group"><h2>Needs attention</h2><ul className="attention-list"><li><Link to="/coordinator/classlist-view">{data.awaiting.length} Google students awaiting enrollment</Link><small>Across all subjects and terms</small></li><li><Link to="/coordinator/classlist-view">{scope.enrollments.filter(e=>!e.userId).length} enrollment records without linked accounts</Link></li><li><Link to="/coordinator/groups">{scope.rows.length?new Set(scope.rows.filter(r=>!r.groupId).map(r=>r.enrollmentId)).size:'—'} enrolled students without groups</Link><small>Based on students with active requirements in this scope</small></li><li><Link to="/coordinator/setup?tab=subjects">{scope.sections.length} sections in this scope</Link>{!scope.sections.length&&<small>Import a classlist to create sections.</small>}</li></ul></section></div>
        <section className="tracking-group"><h2>Overdue follow-ups</h2>{scope.overdue.length?<><p>{new Set(scope.overdue.map(r=>r.groupId).filter(Boolean)).size} groups have missing overdue submissions.</p><div className="tracking-table"><table><thead><tr><th>Student</th><th>Group / subject</th><th>Requirement</th><th>Deadline</th></tr></thead><tbody>{scope.overdue.slice().sort((a,b)=>dueTime(a.dueAt)-dueTime(b.dueAt)).slice(0,10).map(r=><tr key={`${r.enrollmentId}:${r.deliverableId}`}><td>{r.studentName}<small>{r.section}</small></td><td>{r.group}<small>{r.subject}</small></td><td>{r.deliverable}</td><td>{dateTime(r.dueAt)}</td></tr>)}</tbody></table></div><Link to={`/coordinator/analytics?${query({...filters,status:'OVERDUE'})}${term===''?'&term=':''}`}>View all {scope.overdue.length} overdue assignments</Link></>:<p>No missing overdue submissions in the selected scope.</p>}</section>
      </>}
    </>}
  </div>;
}
