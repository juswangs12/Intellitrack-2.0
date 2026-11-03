import {useCallback,useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import api from '../../services/ApiService';
import DashboardProgress from '../../components/DashboardProgress';
import {dateTime,query} from '../TrackingDashboard';
import '../../styles/Tracking.css';
export default function AdviserOverview(){
 const [data,setData]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState('');
 const load=useCallback(async()=>{setLoading(true);setError('');try{setData(await api.requestJson('/tracking'));}catch(e){setError(e.message);}finally{setLoading(false);}},[]);
 useEffect(()=>{load();},[load]);
 const rows=data?.rows||[];
 const groups=new Map();rows.forEach(r=>{const key=JSON.stringify([r.groupId,r.subjectId,r.academicTerm]);if(!groups.has(key))groups.set(key,{first:r,rows:[]});groups.get(key).rows.push(r);});
 const overdue=rows.filter(r=>r.status==='OVERDUE');
 const deadlines=[...new Map(rows.filter(r=>r.dueAt&&new Date(/[zZ]|[+-]\d{2}:\d{2}$/.test(r.dueAt)?r.dueAt:r.dueAt+'+08:00').getTime()>=Date.now()).map(r=>[r.deliverableId,r])).values()].sort((a,b)=>a.dueAt.localeCompare(b.dueAt));
 return <div className="tracking-page"><header className="tracking-header"><div><p className="tracking-eyebrow">ADVISER</p><h1>Adviser dashboard</h1><p>Your assigned groups, submission progress and next follow-ups.</p></div><button className="btn btn-secondary" onClick={load} disabled={loading}>Refresh overview</button></header>
 {error&&<p role="alert" className="tracking-error">{error}</p>}
 {loading?<p role="status">Loading adviser overview...</p>:!error&&data&&<>
 <DashboardProgress rows={rows} title={overdue.length?'Follow up on overdue work':'Your groups at a glance'} description="Only your assigned academic work is included. Open a group to review its individual submissions." actions={[{to:'/adviser/groups',label:'View groups'},{to:'/adviser/review',label:'Review submissions'}]}/>
 <div className="overview-cards">{[['Groups with requirements',groups.size],['Students',new Set(rows.map(r=>r.studentId||`enrollment:${r.enrollmentId}`)).size],['Overdue submissions',overdue.length],['Upcoming deadlines',deadlines.length]].map(([label,count])=><div className="tracking-card" key={label}><span>{label}</span><strong>{count}</strong></div>)}</div>
 <div className="adviser-home-grid"><section className="tracking-group"><h2>Group progress</h2>{groups.size?[...groups.values()].sort((a,b)=>b.rows.filter(r=>r.status==='OVERDUE').length-a.rows.filter(r=>r.status==='OVERDUE').length).slice(0,6).map(({first,rows:members})=><article className="adviser-home-item" key={JSON.stringify([first.groupId,first.subjectId,first.academicTerm])}><div><strong>{first.group||'Unassigned'}</strong><p>{first.subject} | {first.academicTerm}</p><small>{members.filter(r=>r.submissionId).length} / {members.length} submitted | {members.filter(r=>r.status==='OVERDUE').length} overdue missing</small></div><Link to={`/adviser/groups?${query({group:first.groupId,subject:first.subjectId,term:first.academicTerm})}`}>Open group</Link></article>):<p>No assigned requirements yet. Your coordinator can assign groups and requirements.</p>}<Link to="/adviser/groups">View all groups and submissions</Link></section>
 <section className="tracking-group"><h2>Upcoming deadlines</h2>{deadlines.length?deadlines.slice(0,5).map(r=><article className="adviser-home-item" key={r.deliverableId}><div><strong>{r.deliverable}</strong><p>{r.subject} | {dateTime(r.dueAt)}</p></div><Link to={`/adviser/review?${query({subject:r.subjectId,term:r.academicTerm,deliverable:r.deliverableId})}`}>Review</Link></article>):<p>No upcoming deadlines for your assigned work.</p>}<h3>Overdue follow-ups</h3><p>{overdue.length} required submissions are still missing after their deadline.</p>{overdue.length>0&&<Link to="/adviser/review?status=OVERDUE">View overdue submissions</Link>}</section></div>
 </> }</div>;
}
