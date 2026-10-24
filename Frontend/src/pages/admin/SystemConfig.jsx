import {useEffect,useState} from 'react';
import api from '../../services/ApiService';
import DeadlineAutomation from './DeadlineAutomation';
import '../../styles/Tracking.css';
export default function SystemConfig(){
  const [data,setData]=useState(null),[error,setError]=useState('');
  useEffect(()=>{let active=true;api.requestJson('/system-config').then(result=>{if(active)setData(result);}).catch(e=>{if(active)setError(e.message);});return()=>{active=false;};},[]);
  return <div className="tracking-page system-settings"><header className="tracking-header"><div><h1>System configuration</h1><p>Inspect the settings currently used by IntelliTrack.</p></div><span className="workspace-context">Read-only configuration</span></header>
    <DeadlineAutomation/>
    {error&&<p role="alert" className="tracking-error">{error}</p>}
    {!data&&!error?<p role="status">Loading configuration…</p>:data&&<><dl className="config-context"><div><dt>Environment</dt><dd>{data.system.environment}</dd></div><div><dt>Database</dt><dd>{data.system.databaseProduct}</dd></div></dl>
    <div className="workspace-section-heading"><h2>Runtime settings</h2></div><div className="tracking-table"><table><thead><tr><th>Setting</th><th>Effective value</th></tr></thead><tbody>{Object.entries(data.settings).map(([name,value])=><tr key={name}><td>{name}</td><td>{value}</td></tr>)}</tbody></table></div>
    <div className="tracking-group"><h2>Updating configuration</h2><p>Change deployment environment variables and restart the backend to update these settings.</p><p className="tracking-help">“Configured” means credentials are present; confirm external services during workflow testing. Secret values are never displayed.</p></div></>}
  </div>;
}
