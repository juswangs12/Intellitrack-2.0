import {useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {Users, Settings2, ArrowUpRight, History, GraduationCap, BookOpen, ShieldCheck, ClipboardList} from 'lucide-react';
import api from '../../services/ApiService';
import AdminUsage from './AdminUsage';
import '../../styles/Tracking.css';
const roles=[['student','Students',GraduationCap],['adviser','Advisers',BookOpen],['coordinator','Coordinators',ClipboardList],['administrator','Administrators',ShieldCheck]];
export default function AdminHome(){
  const [users,setUsers]=useState([]),[logs,setLogs]=useState([]),[error,setError]=useState(''),[loading,setLoading]=useState(true);
  useEffect(()=>{let active=true;Promise.all([api.requestJson('/users'),api.requestJson('/audit?limit=10')]).then(([u,l])=>{if(active){setUsers(u);setLogs(l);}}).catch(e=>{if(active)setError(e.message);}).finally(()=>{if(active)setLoading(false);});return()=>{active=false;};},[]);
  return <div className="tracking-page admin-overview">
    <header className="tracking-header"><div><h1>Administration</h1><p>Account access, system settings, and a record of recent changes.</p></div><span className="workspace-context"><ShieldCheck size={16}/> Access & governance</span></header>
    {error&&<p role="alert" className="tracking-error">{error}</p>}
    <AdminUsage/>
    <div className="admin-account-grid">{roles.map(([role,label,Icon])=><div className="admin-account-card" key={role}><Icon size={22} aria-hidden="true"/><span>{label}</span><strong>{(loading||error)?'—':users.filter(u=>u.role===role).length}</strong></div>)}</div>
    <div className="admin-shortcuts"><Link to="/admin/users"><Users size={24}/><div><h2>Manage accounts</h2><p>Create users and maintain role access.</p></div><ArrowUpRight size={20}/></Link><Link to="/admin/system"><Settings2 size={24}/><div><h2>System configuration</h2><p>Inspect the environment and connected services.</p></div><ArrowUpRight size={20}/></Link></div>
    <section className="admin-activity"><div className="workspace-section-heading"><div><History size={20}/><h2>Recent activity</h2></div><span>Latest 10 records</span></div>
    {loading?<p role="status" className="tracking-empty">Loading activity…</p>:error?<p className="tracking-empty">Activity is unavailable. Refresh the page to try again.</p>:!logs.length?<div className="tracking-empty"><h3>No recorded activity yet</h3><p>Account and academic changes will appear here as they happen.</p></div>:<div className="tracking-table"><table><thead><tr><th>Action</th><th>Performed by</th><th>Details</th></tr></thead><tbody>{logs.map(log=><tr key={log.id}><td><span className="audit-action">{log.action?.replaceAll('_',' ')}</span></td><td>{log.performedBy}</td><td>{log.details}</td></tr>)}</tbody></table></div>}</section>
  </div>;
}
