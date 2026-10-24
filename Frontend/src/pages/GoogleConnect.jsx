import {useEffect,useRef,useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../context/AuthContext';
import api from '../services/ApiService';
export default function GoogleConnect(){
  const {loading,user}=useAuth();const navigate=useNavigate();const started=useRef(false);const [message,setMessage]=useState('Completing Google connection…');
  useEffect(()=>{if(loading||started.current)return;started.current=true;
    const response=new URLSearchParams(window.location.hash.substring(1));window.history.replaceState(null,'','/google-connect');
    let saved;try{saved=JSON.parse(sessionStorage.getItem('googleConnection')||'null');}catch{}
    sessionStorage.removeItem('googleConnection');
    if(!user||response.get('error')||!saved||saved.state!==response.get('state')){setMessage('Google connection was cancelled or expired. Return to IntelliTrack and connect again.');return;}
    api.requestJson('/google/complete',{method:'POST',body:JSON.stringify({code:response.get('code'),state:saved.state})}).then(()=>navigate(saved.returnTo?.startsWith('/')&&!saved.returnTo.startsWith('//')?saved.returnTo:'/',{replace:true})).catch(e=>setMessage(e.message));
  },[loading,user,navigate]);
  return <main style={{padding:40}}><h1>Google connection</h1><p role="status">{message}</p><a href="/">Return to IntelliTrack</a></main>;
}
