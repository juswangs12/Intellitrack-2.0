import {useState} from 'react';
import {GraduationCap} from 'lucide-react';
import '../styles/BrandLogo.css';

export default function BrandLogo({login=false}){
  const [unavailable,setUnavailable]=useState(false);
  return <div className={`brand-logo${login?' brand-logo-login':''}${unavailable?' brand-logo-fallback':''}`}>
    {unavailable?<GraduationCap aria-hidden="true"/>:<img src={`${process.env.PUBLIC_URL}/intellitrack-logo.png`} alt="IntelliTrack logo" onError={()=>setUnavailable(true)}/>}
  </div>;
}
