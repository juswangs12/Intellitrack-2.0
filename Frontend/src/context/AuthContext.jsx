import React, {createContext, useState, useContext, useEffect, useRef} from 'react';
import apiService from '../services/ApiService';
const AuthContext=createContext(null);
const base=()=>`${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/api`;
const roles=new Set(['student','adviser','coordinator','administrator']);
export const AuthProvider=({children})=>{
  const [session,setSession]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState(null);
  const current=useRef(null),generation=useRef(0),refreshInFlight=useRef(null);
  const save=next=>{
    current.current=next;setSession(next);
    if(next){localStorage.setItem('user',JSON.stringify(next.user));localStorage.setItem('token',next.token);localStorage.setItem('refreshToken',next.refreshToken||'');}
    else ['user','token','refreshToken'].forEach(key=>localStorage.removeItem(key));
  };
  const logout=()=>{generation.current++;refreshInFlight.current=null;save(null);setLoading(false);setError(null);sessionStorage.removeItem('googleConnection');};
  const valid=data=>data?.token&&data?.user?.id&&roles.has(data.user.role);
  const refreshAccessToken=()=>{
    if(refreshInFlight.current)return refreshInFlight.current;
    const snapshot=current.current,epoch=generation.current;
    if(!snapshot?.refreshToken)return Promise.resolve(false);
    const pending=(async()=>{
      try{
        const response=await fetch(`${base()}/auth/refresh-token`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({refreshToken:snapshot.refreshToken})});
        if(!response.ok)return false;
        const data=await response.json();
        if(epoch!==generation.current||!valid(data))return false;
        save({token:data.token,user:data.user,refreshToken:data.refreshToken||snapshot.refreshToken});return true;
      }catch{return false;}
    })();
    refreshInFlight.current=pending;
    pending.finally(()=>{if(refreshInFlight.current===pending)refreshInFlight.current=null;});
    return pending;
  };
  const login=async(email,password)=>{
    const epoch=++generation.current;refreshInFlight.current=null;setLoading(true);setError(null);
    try{
      const response=await fetch(`${base()}/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:email.trim(),password})});
      const data=await response.json().catch(()=>null);
      if(epoch!==generation.current)return {success:false,error:'Sign-in cancelled.'};
      if(!response.ok||!valid(data))throw new Error(data?.message||data?.error||'Invalid email or password.');
      save({token:data.token,user:data.user,refreshToken:data.refreshToken});return {success:true,role:data.user.role};
    }catch(e){if(epoch===generation.current){save(null);setError(e.message);}return {success:false,error:e.message};}
    finally{if(epoch===generation.current)setLoading(false);}
  };
  const handleOAuth2Callback=async({token,refreshToken})=>{
    const epoch=++generation.current;setLoading(true);setError(null);
    try{
      const response=await fetch(`${base()}/users/me`,{headers:{Authorization:`Bearer ${token}`}});
      if(!response.ok)throw new Error('Google session could not be verified. Please sign in again.');
      const user=await response.json();
      if(epoch!==generation.current)throw new Error('Sign-in cancelled.');
      if(!valid({token,user}))throw new Error('Unsupported account role.');
      save({token,refreshToken,user});return user;
    }catch(e){if(epoch===generation.current){save(null);setError(e.message);}throw e;}
    finally{if(epoch===generation.current)setLoading(false);}
  };
  useEffect(()=>{
    if(generation.current!==0)return;
    let active=true;const epoch=generation.current;
    const token=localStorage.getItem('token'),refreshToken=localStorage.getItem('refreshToken');
    if(!token){setLoading(false);return;}
    current.current={token,refreshToken};
    (async()=>{
      try{
        const response=await fetch(`${base()}/users/me`,{headers:{Authorization:`Bearer ${token}`}});
        if(!active||epoch!==generation.current)return;
        if(response.ok){const user=await response.json();if(active&&epoch===generation.current){if(!valid({token,user}))throw new Error('Invalid account');save({token,refreshToken,user});}}
        else if(response.status===401){if(!await refreshAccessToken()&&active&&epoch===generation.current)save(null);}
        else throw new Error('Unable to verify your session. Please sign in again.');
      }catch(e){if(active&&epoch===generation.current){save(null);setError('Unable to restore your session. Please sign in again.');}}
      finally{if(active&&epoch===generation.current)setLoading(false);}
    })();
    return()=>{active=false;};
  },[]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(()=>{
    const sync=e=>{if(e.key==='token'&&e.newValue===null)logout();};
    window.addEventListener('storage',sync);return()=>window.removeEventListener('storage',sync);
  },[]); // eslint-disable-line react-hooks/exhaustive-deps
  const value={user:session?.user||null,token:session?.token||null,refreshToken:session?.refreshToken||null,loading,error,login,logout,handleOAuth2Callback,refreshAccessToken,
    updateProfile:user=>{if(current.current?.user)save({...current.current,user});},
    getAuthHeader:()=>current.current?.token?{Authorization:`Bearer ${current.current.token}`}:{},
    isAuthenticated:()=>!!session?.token&&!!session?.user,hasRole:role=>session?.user?.role===role};
  useEffect(()=>{apiService.setAuthContext(value);});
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
export const useAuth=()=>{const context=useContext(AuthContext);if(!context)throw new Error('useAuth must be used within an AuthProvider');return context;};
