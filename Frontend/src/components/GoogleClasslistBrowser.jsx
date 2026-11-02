import React, {useEffect, useState} from 'react';
import api from '../services/ApiService';
import {connectGoogle} from '../pages/TrackingDashboard';

export default function GoogleClasslistBrowser({onSelect, disabled}) {
  const [files,setFiles]=useState([]),[search,setSearch]=useState(''),[page,setPage]=useState('');
  const [loading,setLoading]=useState(false),[error,setError]=useState('');
  const [loadedSearch,setLoadedSearch]=useState('');
  const load=async(next='',query=search)=>{
    setLoading(true);setError('');
    try { const result=await api.requestJson(`/google/imports/files?${new URLSearchParams({search:query,page:next})}`);
      setFiles(previous=>next?[...previous,...result.files]:result.files);setPage(result.nextPageToken||'');setLoadedSearch(query);
    } catch(e){setError(e.message||'Could not load Google Drive. Connect your account and try again.');}
    finally {setLoading(false);}
  };
  useEffect(()=>{load('','');},[]); // eslint-disable-line react-hooks/exhaustive-deps
  return <section aria-label="Google Drive classlists" className="tracking-form">
    <h3>Choose a classlist from Google Drive</h3>
    <p>Google Sheets you own or can access. Convert Excel files in Drive to Google Sheets before selecting them.</p>
    <button type="button" className="btn btn-secondary" disabled={disabled||loading} onClick={async()=>{try{await connectGoogle('imports');}catch(e){setError(e.message);}}}>Connect / reconnect Google Drive</button>
    <form onSubmit={e=>{e.preventDefault();load();}} className="tracking-actions">
      <label>Find a spreadsheet<input value={search} onChange={e=>setSearch(e.target.value)} maxLength={200} disabled={loading||disabled}/></label>
      <button className="btn btn-secondary" disabled={loading||disabled}>Search Drive</button>
    </form>
    {error&&<p role="alert">{error} If permissions changed, reconnect Google Drive.</p>}
    <div aria-live="polite">{loading?'Loading spreadsheets…':!error&&files.length===0?'No spreadsheets found.':''}</div>
    <ul className="cloud-classlist-files">{files.map(file=><li key={file.id}><button type="button" className="btn btn-secondary" disabled={disabled||loading} onClick={()=>onSelect({name:file.name,url:`https://docs.google.com/spreadsheets/d/${file.id}/edit`})}>{file.name}</button></li>)}</ul>
    {page&&<button type="button" className="btn btn-secondary" disabled={loading||disabled} onClick={()=>load(page,loadedSearch)}>Load more</button>}
  </section>;
}
