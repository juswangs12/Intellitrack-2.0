import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/ApiService';
import '../styles/Tracking.css';
import CoordinatorSetupSteps from '../components/CoordinatorSetupSteps';
import AdviserReview from '../components/AdviserReview';
import GroupSubmissionOverview from '../components/GroupSubmissionOverview';
import IndividualSubmissionOverview from '../components/IndividualSubmissionOverview';

export const STATUS = { SUBMITTED_ON_TIME:'On time', SUBMITTED_LATE:'Late', OVERDUE:'Overdue', DUE_TODAY:'Due today', AWAITING_SUBMISSION:'Awaiting submission' };
export const dateTime = value => value ? new Date(/[zZ]|[+-]\d{2}:\d{2}$/.test(value)?value:value+'+08:00').toLocaleString('en-PH', {timeZone:'Asia/Manila',dateStyle:'medium',timeStyle:'short'}) : '—';
export const query = filters => new URLSearchParams(Object.entries(filters).filter(([,v])=>v!==''&&v!=null)).toString();
export const connectGoogle = async purpose => {
  const result=await api.requestJson(`/google/connect/${purpose}`,{method:'POST'});
  sessionStorage.setItem('googleConnection',JSON.stringify({state:result.state,returnTo:window.location.pathname+window.location.search}));
  window.location.assign(result.url);
};

export default function TrackingDashboard(){
  const {user}=useAuth(); const {groupId}=useParams();
  const [searchParams,setSearchParams]=useSearchParams();
  const initialized=useRef(searchParams.has('term'));
  const subjectInitialized=useRef(searchParams.has('subject'));
  const initialHistoryTerm=useRef(searchParams.get('term'));
  const student=user?.role==='student';const coordinator=['coordinator','administrator'].includes(user?.role);
  const [availableTerms,setAvailableTerms]=useState([]);
  const [catalog,setCatalog]=useState([]),[data,setData]=useState({rows:[],summary:{}});
  const [filters,setFilters]=useState(()=>Object.fromEntries(Object.entries({term:'',subject:'',section:'',group:groupId||'',adviser:'',deliverable:'',status:'',search:''}).map(([key,fallback])=>[key,searchParams.get(key)??fallback])));
  const [ready,setReady]=useState(false),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const [busy,setBusy]=useState(''),[notice,setNotice]=useState(''),[insight,setInsight]=useState(null),[report,setReport]=useState('');
  const [page,setPage]=useState(0),[view,setView]=useState(student?'students':'groups'),[revision,setRevision]=useState(0);
  useEffect(()=>{let active=true;api.requestJson(initialHistoryTerm.current?`/tracking?${query({term:initialHistoryTerm.current})}`:'/tracking').then(result=>{
    if(!active)return;setCatalog(result.rows);setAvailableTerms(result.terms||[]);const terms=[...new Set(result.rows.map(r=>r.academicTerm).filter(Boolean))].sort().reverse();
    if(!initialized.current){setFilters(f=>({...f,term:terms[0]||''}));initialized.current=true;}
    if(!student&&!subjectInitialized.current){setFilters(f=>({...f,subject:String(result.rows.find(r=>!f.term||r.academicTerm===f.term)?.subjectId||'')}));subjectInitialized.current=true;}setReady(true);
  }).catch(e=>{if(active){setError(e.message);setLoading(false);}});return()=>{active=false;};},[revision,student]);
  const parameters=query(filters);
  const urlParameters=parameters+(filters.term===''?(parameters?'&term=':'term='):'');
  useEffect(()=>{if(ready&&searchParams.toString()!==urlParameters)setSearchParams(urlParameters,{replace:true});},[urlParameters,ready,searchParams,setSearchParams]);
  useEffect(()=>{if(!ready)return;let active=true;setLoading(true);setError('');setInsight(null);setReport('');setPage(0);
    const timer=setTimeout(()=>api.requestJson(`/tracking?${parameters}`).then(result=>{if(active)setData(result);}).catch(e=>{if(active)setError(e.message);}).finally(()=>{if(active)setLoading(false);}),150);
    return()=>{active=false;clearTimeout(timer);};
  },[parameters,ready,revision]);
  const options=useCallback((key,label=key)=>[...new Map(catalog.filter(r=>r[key]!=null&&(key==='academicTerm'||!filters.term||r.academicTerm===filters.term)&&(key==='academicTerm'||key==='subjectId'||!filters.subject||String(r.subjectId)===filters.subject)).map(r=>[String(r[key]),String(r[label])])).entries()].sort((a,b)=>a[1].localeCompare(b[1])),[catalog,filters.term,filters.subject]);
  const grouped=useMemo(()=>Object.values(data.rows.reduce((all,r)=>{
    const key=[r.groupId,r.subjectId,r.academicTerm,r.deliverableId].join(':');
    if(!all[key])all[key]={...r,members:[],counts:{}};all[key].members.push(r);all[key].counts[r.status]=(all[key].counts[r.status]||0)+1;return all;
  },{})),[data.rows]);
  const action=async(name,fn)=>{setBusy(name);setNotice('');setError('');try{await fn();}catch(e){setError(e.message);}finally{setBusy('');}};
  const exportReport=()=>action('export',async()=>{setReport('');if(new Set(data.rows.map(r=>`${r.subjectId}:${r.academicTerm}`)).size>1)throw new Error('Choose one subject and academic term before exporting. Each subject and semester has its own report file.');const status=await api.requestJson('/google/status');if(!status.reports){await connectGoogle('reports');return;}const result=await api.requestJson(`/google/reports/export?${parameters}`,{method:'POST'});setReport(result.url);setNotice(result.message||'Report updated.');});
  const generate=()=>action('ai',async()=>setInsight(await api.requestJson(`/tracking/summary?${parameters}`,{method:'POST'})));
  const list=view==='groups'?grouped:data.rows; const pageSize=25;const visible=list.slice(page*pageSize,(page+1)*pageSize);
  const setFilter=(key,value)=>{setPage(0);setFilters(f=>({...f,...(['subject','term'].includes(key)?{section:'',group:'',deliverable:'',adviser:''}:{}),...(key==='term'?{subject:''}:{}),[key]:value}));};
  return <div className="tracking-page">
    <header className="tracking-header"><div><p className="tracking-eyebrow">INTELLITRACK · SUBMISSION TRACKING</p><h1>{student?'My submissions':coordinator?'Submission analytics':'Group submission overview'}</h1><p>{student?'Your requirements, deadlines and submission history across subjects.':coordinator?'See who submitted, who was late, and who needs follow-up.':'Follow up on your assigned groups and open individual submitted documents.'}</p></div><button className="btn btn-secondary" onClick={()=>setRevision(x=>x+1)} disabled={loading||!!busy}>Refresh</button></header>
    {ready&&!catalog.length&&user?.role==='coordinator'&&<CoordinatorSetupSteps/>}
    {!student&&<nav className="analytics-scope-bar" aria-label="Analytics subject and view"><strong>Subject</strong>{options('subjectId','subject').map(([id,label])=><button className={`btn ${filters.subject===id?'btn-primary':'btn-secondary'}`} aria-pressed={filters.subject===id} key={id} onClick={()=>setFilter('subject',id)}>{label}</button>)}<span className="analytics-view-switch"><button className="btn btn-secondary" aria-pressed={view==='groups'} onClick={()=>{setView('groups');setPage(0);}}>Groups</button><button className="btn btn-secondary" aria-pressed={view==='students'} onClick={()=>{setView('students');setPage(0);}}>Individuals</button></span></nav>}
    <fieldset className="tracking-filters analytics-filters" disabled={!!busy}>
      <label>Academic term<select value={filters.term} onChange={e=>setFilter('term',e.target.value)}><option value="">All terms</option>{(student?[...new Set([...availableTerms,...options('academicTerm').map(([v])=>v)])].map(v=>[v,v]):options('academicTerm')).map(([v,l])=><option key={v} value={v}>{l}</option>)}<option value="legacy">Legacy / unassigned term</option></select></label>
      {[['subject','subjectId','subject','Subject'],['section','section','section','Section'],['group','groupId','group','Group'],['deliverable','deliverableId','deliverable','Requirement'],...(!student?[['adviser','adviserId','adviser','Adviser']]:[])].filter(([key])=>!student||key==='subject').map(([key,id,label,title])=><label key={key}>{title}<select value={filters[key]} onChange={e=>setFilter(key,e.target.value)}><option value="">All</option>{options(id,label).map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label>)}
      <label>Status<select value={filters.status} onChange={e=>setFilter('status',e.target.value)}><option value="">All</option>{Object.entries(STATUS).map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label>
      {!student&&<label>Find student<input value={filters.search} onChange={e=>setFilter('search',e.target.value)} placeholder="Name or student ID"/></label>}
    </fieldset>
    {error&&<p role="alert" className="tracking-error">{error}</p>}{notice&&<p role="status" className="tracking-notice">{notice}</p>}
    {user?.role==='adviser'&&!loading&&!error&&<AdviserReview key={parameters+':'+revision} rows={data.rows}/>}
    <p className="tracking-help">{student?'Deadlines are shown in Philippine time (Asia/Manila).':'All counts, AI summaries and exports use the selected filters. Times are Asia/Manila. Each status counts one student × requirement.'}</p>
    <div className="tracking-cards">{Object.entries(STATUS).map(([key,label])=><div key={key} className={`tracking-card ${key}`}><span>{label}</span><strong>{loading?'—':data.summary[key]||0}</strong></div>)}</div>
    <p hidden={student}>{data.summary.students||0} distinct students · {data.summary.expected||0} assignments · {data.summary.lateReplacements||0} on-time submissions with a late replacement</p>
    {coordinator&&<details className="report-help"><summary>How report exports work</summary><p className="tracking-help">Exports reuse one report file per subject and semester. Each section gets its own tab; exporting the same filters updates that snapshot. Groups and Summary retain separately labelled results for each exported scope.</p></details>}
    <div className="tracking-actions">
      {!student&&<button className="btn btn-secondary" onClick={()=>{setView(v=>v==='students'?'groups':'students');setPage(0);}}>{view==='students'?'Group breakdown':'Student details'}</button>}
      {!student&&<button className="btn btn-primary" disabled={!!busy||loading||!data.rows.length} onClick={generate}>{busy==='ai'?'Summarizing…':'Generate AI summary'}</button>}
      {coordinator&&<button className="btn btn-secondary" disabled={!!busy||loading||!data.rows.length} onClick={exportReport}>{busy==='export'?'Exporting…':'Export to Google Sheets'}</button>}
      {coordinator&&<button className="btn btn-secondary" disabled={!!busy} onClick={()=>action('connect',()=>connectGoogle(student?'calendar':'reports'))}>Connect / reconnect Google</button>}
      {report&&<a href={report} target="_blank" rel="noreferrer">Open report ↗</a>}
    </div>
    {insight&&<section className="tracking-insight"><h2>{insight.source}</h2><p style={{whiteSpace:'pre-wrap'}}>{insight.text}</p><small>{insight.notice}</small></section>}
    {loading?<p role="status">Loading tracking data… The backend may take a moment to wake up.</p>:!list.length?<div className="tracking-empty">No requirements match this scope. {student?'Your classlist may not be linked yet, or your coordinator has not assigned requirements. Ask your coordinator to check the Google email used for your enrollment.':coordinator?'Assign terms and requirements in Academic setup, or adjust the filters.':'No assigned requirements match these filters. Your coordinator manages group assignments and deadlines.'}</div>:student?<div className="student-requirements">{visible.map(r=><article className="student-requirement" key={r.enrollmentId+':'+r.deliverableId}>
      <div className="student-requirement-heading"><span className="student-subject">{r.subject}</span><span className={`tracking-status ${r.status}`}>{STATUS[r.status]}</span></div>
      <h2>{r.deliverable}</h2><p className="student-due">Due {dateTime(r.dueAt)}</p>
      <dl><div><dt>Group</dt><dd>{r.group||'Not assigned'}</dd></div><div><dt>Adviser</dt><dd>{r.adviser||'Not assigned'}</dd></div><div><dt>Academic term</dt><dd>{r.academicTerm||'Not assigned'}</dd></div><div><dt>First submitted</dt><dd>{dateTime(r.firstSubmittedAt)}</dd></div></dl>
      {r.latestStatus==='SUBMITTED_LATE'&&r.status==='SUBMITTED_ON_TIME'&&<p className="tracking-help">Your replacement was late; your first submission remains on time.</p>}
      <div className="student-requirement-actions">{r.canSubmit?<Link className="btn btn-primary" to={`/student/submissions/${r.deliverableId}?group=${r.groupId}`}>{r.submissionId?'View submission / replace':'Submit Google Docs link'}</Link>:<p>{r.submissionBlockReason||'Ask your coordinator to assign you to a group before submitting.'}</p>}{!r.canSubmit&&r.submissionId&&<Link to={`/student/submissions/${r.deliverableId}?group=${r.groupId}&term=${encodeURIComponent(r.academicTerm||'')}`}>View submission history</Link>}{r.googleDocUrl&&<a href={r.googleDocUrl} target="_blank" rel="noreferrer">Open submitted document</a>}</div>
    </article>)}</div>:view==='groups'&&coordinator?<GroupSubmissionOverview key={parameters} rows={data.rows} statusLabels={STATUS} formatDate={dateTime}/>:view==='groups'?<div className="tracking-groups">{visible.map(g=><section key={`${g.groupId}:${g.deliverableId}`} className="tracking-group"><h2>{g.group} · {g.deliverable}</h2>{g.groupId&&<button className="btn btn-secondary" onClick={()=>{setFilters(f=>({...f,subject:String(g.subjectId),term:g.academicTerm||'legacy',group:String(g.groupId),deliverable:String(g.deliverableId)}));setView('students');setPage(0);}}>View individual records</button>}<p>{g.subject} · {g.academicTerm||'Legacy'} · Adviser: {g.adviser} · Due {dateTime(g.dueAt)}</p><div className="group-status-summary">{Object.entries(g.counts).map(([status,n])=><span key={status} className={`tracking-status ${status}`}>{n} {STATUS[status]}</span>)}</div><ul>{g.members.map(r=><li className="group-member-summary" key={r.enrollmentId}><div><strong>{r.studentName}</strong><small>Section {r.section}</small></div><span className={`tracking-status ${r.status}`}>{STATUS[r.status]}</span>{r.latestStatus==='SUBMITTED_LATE'&&r.status==='SUBMITTED_ON_TIME'&&<small>Latest replacement late</small>}{r.googleDocUrl&&<a className="group-document-link" href={r.googleDocUrl} target="_blank" rel="noreferrer">Open document</a>}</li>)}</ul></section>)}</div>:<IndividualSubmissionOverview key={parameters} rows={data.rows} statusLabels={STATUS} formatDate={dateTime}/>}
    {(student||(!coordinator&&view==='groups'))&&list.length>pageSize&&<div className="tracking-actions"><button disabled={page===0} onClick={()=>setPage(p=>p-1)}>Previous</button><span>Page {page+1} of {Math.ceil(list.length/pageSize)}</span><button disabled={(page+1)*pageSize>=list.length} onClick={()=>setPage(p=>p+1)}>Next</button></div>}
  </div>;
}
