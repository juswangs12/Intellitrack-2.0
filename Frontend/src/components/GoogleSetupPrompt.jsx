import {useEffect,useState} from 'react';
import api from '../services/ApiService';
import {connectGoogle} from '../pages/TrackingDashboard';
import WorkspaceDialog from './WorkspaceDialog';
const purposes={student:['calendar'],coordinator:['calendar','imports','reports'],administrator:['imports','reports']};
export default function GoogleSetupPrompt({user}){
  const [open,setOpen]=useState(false),[busy,setBusy]=useState(false),[error,setError]=useState('');
  const key=`google-setup-v1:${user.id}:${user.role}`;
  useEffect(()=>{
    let active=true;
    if(!purposes[user.role]||localStorage.getItem(key))return;
    api.requestJson('/google/status').then(status=>{if(active&&status.configured&&purposes[user.role].some(p=>!status[p]))setOpen(true);}).catch(()=>{});
    return()=>{active=false;};
  },[key,user.role]);
  const dismiss=()=>{localStorage.setItem(key,'offered');setOpen(false);};
  if(!open)return null;
  return <WorkspaceDialog title="Connect your Google tools" onClose={dismiss}>
    <p>{user.role==='student'?'Allow IntelliTrack to keep your deadline calendar up to date.':'Connect Google Drive for classlists and report exports'+(user.role==='coordinator'?', plus automatic deadline calendar updates.':'.')}</p>
    <p>Google will ask you to approve access once. You can also connect later from the feature you want to use.</p>
    {error&&<p role="alert">{error}</p>}
    <div className="tracking-actions"><button className="btn btn-primary" disabled={busy} onClick={async()=>{setBusy(true);setError('');try{await connectGoogle(user.role+'-tools');localStorage.setItem(key,'offered');}catch(e){setError(e.message);setBusy(false);}}}>Continue with Google</button><button className="btn btn-secondary" disabled={busy} onClick={dismiss}>Not now</button></div>
  </WorkspaceDialog>;
}
