import {useEffect,useState} from 'react';
import api from '../services/ApiService';
import {connectGoogle} from '../pages/TrackingDashboard';
export default function CalendarConnection({endpoint}){
 const [status,setStatus]=useState(null),[error,setError]=useState(''),[busy,setBusy]=useState(false),[notice,setNotice]=useState('');
 useEffect(()=>{let active=true;api.requestJson('/google/status').then(s=>{if(active)setStatus(s);}).catch(e=>{if(active)setError(e.message);});return()=>{active=false;};},[]);
 const run=async(connect)=>{setBusy(true);setError('');try{if(connect){await connectGoogle('calendar');return;}const r=await api.requestJson(endpoint,{method:'POST'});setNotice(`${r.synced} deadlines updated.`);}catch(e){setError(e.message);}finally{setBusy(false);}};
 return <section className="tracking-group calendar-connection"><h2>Google Calendar</h2>{error&&<p role="alert">{error}</p>}<p>{!status?'Checking calendar connection...':!status.calendar?'Connect your Google Calendar to receive deadline updates.':status.calendarAutomation?'Connected. Deadlines update automatically in your IntelliTrack calendar. Updates can take a few minutes.':'Connected. Automatic updates are disabled; refresh deadlines manually.'}</p><div className="tracking-actions"><button className="btn btn-secondary" disabled={busy} onClick={()=>run(true)}>{status?.calendar?'Reconnect Google Calendar':'Connect Google Calendar'}</button>{status?.calendar&&!status.calendarAutomation&&endpoint&&<button className="btn btn-primary" disabled={busy} onClick={()=>run(false)}>Update calendar</button>}{status?.calendar&&<a className="btn btn-secondary" href="https://calendar.google.com/" target="_blank" rel="noreferrer">Open Google Calendar</a>}</div>{notice&&<p role="status">{notice}</p>}</section>;
}
