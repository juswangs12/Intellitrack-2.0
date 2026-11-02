import {useEffect,useState} from 'react';
import {Mail,RefreshCw,CheckCircle2,PauseCircle} from 'lucide-react';
import api from '../../services/ApiService';
import {connectGoogle} from '../TrackingDashboard';
import '../../styles/DeadlineAutomation.css';

export default function DeadlineAutomation(){
  const [data,setData]=useState(null),[error,setError]=useState('');
  const [connecting,setConnecting]=useState(false),[refreshing,setRefreshing]=useState(false);
  const refresh=async()=>{setError('');setRefreshing(true);try{setData(await api.requestJson('/admin/automation'));}catch(e){setError(e.message);}finally{setRefreshing(false);}};
  const connect=async()=>{setError('');setConnecting(true);try{await connectGoogle('mail');}catch(e){setError(e.message);}finally{setConnecting(false);}};
  useEffect(()=>{refresh();},[]);
  return <section className="automation-panel" aria-labelledby="automation-title">
    <header className="automation-heading"><div className="automation-title"><span className="automation-icon"><Mail size={22} aria-hidden="true"/></span><div><h2 id="automation-title">Deadline notifications</h2><p>Sender connection and automatic delivery status.</p></div></div>
      <button type="button" className="btn btn-secondary" disabled={refreshing} onClick={refresh}><RefreshCw size={15} aria-hidden="true"/>{refreshing?'Refreshing...':'Refresh status'}</button>
    </header>
    {error&&<p role="alert" className="tracking-error">{error}</p>}
    {data?<>
      {data.configurationProblems?.length>0&&<div className="tracking-error" role="alert"><strong>Backend configuration needs attention</strong><ul>{data.configurationProblems.map(problem=><li key={problem}>{problem}</li>)}</ul></div>}
      <div className="automation-content"><div className="automation-sender"><span className="automation-label">Gmail sender</span><strong>{data.sender||'No sender configured'}</strong>
        <span className={`automation-badge ${data.connected?'is-connected':''}`}>{data.connected?<CheckCircle2 size={14} aria-hidden="true"/>:<Mail size={14} aria-hidden="true"/>}{data.connected?'Connected':'Connection required'}</span>
        <p>{data.connected?'This account is authorized to send deadline emails. Reconnect only if access expires or is revoked.':'Connect the designated Google account to authorize outgoing deadline emails.'}</p>
        <button type="button" className={`btn ${data.connected?'btn-secondary':'btn-primary'}`} disabled={!data.sender||connecting||data.configurationProblems?.length>0} onClick={connect}>{connecting?'Opening Google...':data.connected?'Reconnect Gmail':'Connect Gmail sender'}</button>
      </div><div className="automation-delivery"><span className="automation-label">Automatic delivery</span><h3>{data.enabled?<CheckCircle2 size={18} aria-hidden="true"/>:<PauseCircle size={18} aria-hidden="true"/>}{data.enabled?'Enabled':'Not enabled yet'}</h3>
        <p>{data.enabled?'The worker sends enrollment welcomes, adviser account and assignment notices, student deadline reminders, and daily staff digests.':'Gmail is connected separately. Automatic email delivery is off until DEADLINE_AUTOMATION_ENABLED is set to true on the production backend.'}</p>
        <p>Newly imported students receive their subject, section, term and sign-in link, even before their account is linked. Re-importing an existing enrollment does not resend its welcome.</p>
        <p>Coordinators and advisers receive at most one daily digest after 8:00 AM Philippine time. Analytics refreshes and sign-ins do not send emails.</p>
        <dl className="automation-counts" aria-label="Email delivery totals"><div><dt>Queued</dt><dd>{data.pending}</dd></div><div><dt>Sent</dt><dd>{data.sent}</dd></div><div><dt>Needs review</dt><dd>{data.review}</dd></div></dl>
      </div></div>
      {data.review>0&&<p role="status" className="tracking-error">Check the sender's Sent folder for uncertain deliveries. These emails are not retried automatically.</p>}
      <details className="automation-details"><summary>Deployment and calendar setup</summary><p>Students and coordinators connect their own Calendar once. The worker then updates their assigned deadlines.</p><p>On Render Free, configure the protected external scheduler before relying on reminders. Keep automation disabled locally when sharing the production database.</p><p>Sender and worker settings are managed through backend environment variables.</p></details>
    </>:<p role="status">Loading notification settings...</p>}
  </section>;
}
