import SummaryPanel from '../../components/SummaryPanel';
import CalendarConnection from '../../components/CalendarConnection';
import {useCallback,useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {useAuth} from '../../context/AuthContext';
import DashboardProgress from '../../components/DashboardProgress';
import {EnrollmentStatus} from './StudentGroups';
import api from '../../services/ApiService';
import {STATUS,dateTime} from '../TrackingDashboard';

const todayInManila=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Manila',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
const submissionPath=r=>`/student/submissions/${r.deliverableId}?group=${r.groupId}&term=${encodeURIComponent(r.academicTerm||'')}`;
function Requirement({row}){
  return <article className="student-agenda-item"><div><strong>{row.deliverable}</strong><p>{row.subject} · {row.group||'No group'} · {dateTime(row.dueAt)}</p><span className={`tracking-status ${row.status}`}>{STATUS[row.status]}</span></div>{(row.canSubmit||row.submissionId)&&<Link className="btn btn-secondary" to={submissionPath(row)}>{row.submissionId?'View submission':row.canSubmit?'Submit link':'View history'}</Link>}</article>;
}
export default function StudentOverview({calendar=false}){
  const {user}=useAuth();
  const [enrollments,setEnrollments]=useState(null);
  const [data,setData]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const busy='',notice='';
  const [month,setMonth]=useState(()=>todayInManila().slice(0,7)),[day,setDay]=useState(todayInManila);
  const load=useCallback(async()=>{setLoading(true);setError('');try{const [tracking,enrollment]=await Promise.all([api.requestJson('/tracking'),api.requestJson('/tracking/enrollments')]);setData(tracking);setEnrollments(Array.isArray(enrollment)?enrollment:null);}catch(e){setError(e.message);}finally{setLoading(false);}},[]);
  useEffect(()=>{load();},[load]);
  const rows=data?.rows||[];
  const terms=data?.terms||[...new Set(rows.map(r=>r.academicTerm).filter(Boolean))];
  const ordered=[...rows].sort((a,b)=>(a.dueAt||'9999').localeCompare(b.dueAt||'9999'));
  const outstanding=ordered.filter(r=>!r.submissionId);
  const events=ordered.filter(r=>r.dueAt?.slice(0,7)===month);
  const [year,number]=month.split('-').map(Number);
  const count=new Date(Date.UTC(year,number,0)).getUTCDate(),start=new Date(Date.UTC(year,number-1,1)).getUTCDay();
  const changeMonth=value=>{if(/^\d{4}-\d{2}$/.test(value)){setMonth(value);setDay(value+'-01');}};
  const shift=delta=>changeMonth(new Date(Date.UTC(year,number-1+delta,1)).toISOString().slice(0,7));
  return <div className="tracking-page student-overview">
    <header className="tracking-header"><div><h1>{calendar?'My deadline calendar':`Welcome, ${user?.firstName||user?.name?.split(' ')[0]||'student'}`}</h1><p>{calendar?'Your assigned deadlines in Philippine time.':'Keep track of your requirements, submissions, and next steps.'}</p></div><button className="btn btn-secondary" disabled={loading||!!busy} onClick={load}>Refresh</button></header>
    {error&&<p role="alert" className="tracking-error">{error}</p>}{notice&&<p role="status" className="tracking-notice">{notice}</p>}
    {loading?<p role="status">Loading your requirements...</p>:data&&<>
    {calendar?<>
      <CalendarConnection endpoint={terms.length?`/google/calendar/sync?term=${encodeURIComponent(terms[0])}`:null}/>
      <section className="student-calendar-surface"><div className="student-calendar-toolbar"><h2>{new Intl.DateTimeFormat('en-PH',{month:'long',year:'numeric',timeZone:'UTC'}).format(new Date(Date.UTC(year,number-1,1)))}</h2><button className="btn btn-secondary" onClick={()=>shift(-1)}>Previous</button><label>Month<input type="month" value={month} onChange={e=>changeMonth(e.target.value)}/></label><button className="btn btn-secondary" onClick={()=>{setMonth(todayInManila().slice(0,7));setDay(todayInManila());}}>Today</button><button className="btn btn-secondary" onClick={()=>shift(1)}>Next</button></div>
      <div className="student-calendar" aria-label="Your monthly deadlines">{['Sun','Mon','Tue','Wed','Thu','Fri','Sat'].map(d=><div className="student-weekday" key={d}>{d}</div>)}{Array.from({length:start},(_,i)=><div className="student-calendar-blank" key={'blank'+i}/>)}{Array.from({length:count},(_,i)=>{const date=`${month}-${String(i+1).padStart(2,'0')}`,items=events.filter(r=>r.dueAt.slice(0,10)===date);return <button className={date===todayInManila()?'is-today':''} aria-label={`${date}, ${items.length} deadlines`} aria-pressed={day===date} key={date} onClick={()=>setDay(date)}><strong>{i+1}</strong>{items.map(r=><span key={r.enrollmentId+':'+r.deliverableId}>{r.subject}: {r.deliverable}</span>)}</button>;})}</div>
      </section><div className="student-calendar-agendas"><section className="tracking-group"><h2>Deadlines on {day}</h2>{events.filter(r=>r.dueAt.slice(0,10)===day).length?events.filter(r=>r.dueAt.slice(0,10)===day).map(r=><Requirement row={r} key={r.enrollmentId+':'+r.deliverableId}/>):<p>No deadlines on this day.</p>}</section>
      <section className="tracking-group"><h2>This month</h2>{events.length?events.map(r=><Requirement row={r} key={r.enrollmentId+':'+r.deliverableId}/>):<p>No deadlines this month. Choose another month to see more.</p>}</section></div>
    </>:<>
      <DashboardProgress rows={rows} title={outstanding.length?"Your next steps":"You are up to date"} description="Submit your assigned work, check deadlines and keep your current document links together." actions={[{to:"/student/submissions",label:"My submissions"},{to:"/student/calendar",label:"My calendar"},{to:"/student/groups",label:"My groups"}]}/>
      <div className="student-summary-cards">{[['Requirements',rows.length],['Submitted',rows.filter(r=>r.submissionId).length],['Due today',data.summary?.DUE_TODAY||0],['Overdue',data.summary?.OVERDUE||0]].map(([label,value])=><Link to={`/student/submissions?${label==='Overdue'?'status=OVERDUE':label==='Due today'?'status=DUE_TODAY':label==='Submitted'?'submitted=true':''}`} className={`tracking-card student-stat-${label.toLowerCase().replace(' ','-')}`} key={label}><span>{label}</span><strong>{value}</strong></Link>)}</div>
      <EnrollmentStatus enrollments={enrollments}/>
      <div className="student-overview-columns"><section className="tracking-group"><h2>What needs your attention</h2>{outstanding.length?outstanding.slice(0,5).map(r=><Requirement row={r} key={r.enrollmentId+':'+r.deliverableId}/>):<p>{rows.length?'You have submitted all currently assigned requirements.':'New requirements will appear here when assigned.'}</p>}{outstanding.length>5&&<Link to="/student/submissions">View all {outstanding.length} outstanding requirements</Link>}</section>
      <SummaryPanel disabled={!rows.length}/></div>

    </>}
    </>}
  </div>;
}
