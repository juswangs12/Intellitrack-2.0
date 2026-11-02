import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {AuthProvider,useAuth} from './AuthContext';
let root,container,auth;
function Probe(){auth=useAuth();return null;}
beforeEach(()=>{global.IS_REACT_ACT_ENVIRONMENT=true;localStorage.clear();container=document.createElement('div');document.body.appendChild(container);root=createRoot(container);global.fetch=jest.fn();});
afterEach(()=>{act(()=>root.unmount());container.remove();jest.restoreAllMocks();});
test('login and refresh use separate JSON payloads and refresh updates account role',async()=>{
  await act(async()=>{root.render(<AuthProvider><Probe/></AuthProvider>);});
  fetch.mockResolvedValueOnce({ok:true,json:async()=>({token:'access1',refreshToken:'refresh1',role:'student',user:{id:1,role:'student'}})});
  await act(async()=>{await auth.login('student@gmail.com','password123');});
  expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({email:'student@gmail.com',password:'password123',audience:'student'});
  fetch.mockResolvedValueOnce({ok:true,json:async()=>({token:'access2',refreshToken:'refresh1',role:'adviser',user:{id:1,role:'adviser'}})});
  await act(async()=>{expect(await auth.refreshAccessToken()).toBe(true);});
  expect(fetch.mock.calls[1][0]).toMatch(/\/api\/auth\/refresh-token$/);
  expect(JSON.parse(fetch.mock.calls[1][1].body)).toEqual({refreshToken:'refresh1'});
  expect(auth.user.role).toBe('adviser');expect(localStorage.getItem('token')).toBe('access2');
});

test('restoring a session uses the server role instead of saved browser profile',async()=>{
 localStorage.setItem('token','saved');localStorage.setItem('user',JSON.stringify({id:1,role:'administrator'}));
 fetch.mockResolvedValueOnce({ok:true,json:async()=>({id:1,role:'student'})});
 await act(async()=>root.render(<AuthProvider><Probe/></AuthProvider>));
 expect(auth.user.role).toBe('student');expect(auth.loading).toBe(false);
});
test('concurrent refreshes share one request and cannot restore a logged out session',async()=>{
 await act(async()=>root.render(<AuthProvider><Probe/></AuthProvider>));
 fetch.mockResolvedValueOnce({ok:true,json:async()=>({token:'old',refreshToken:'refresh',user:{id:1,role:'student'}})});
 await act(async()=>auth.login('s@test.com','password123'));
 let resolve;fetch.mockImplementationOnce(()=>new Promise(r=>resolve=r));
 let first,second;act(()=>{first=auth.refreshAccessToken();second=auth.refreshAccessToken();});
 expect(first).toBe(second);expect(fetch).toHaveBeenCalledTimes(2);
 act(()=>auth.logout());
 await act(async()=>{resolve({ok:true,json:async()=>({token:'new',user:{id:1,role:'student'}})});expect(await first).toBe(false);});
 expect(auth.isAuthenticated()).toBe(false);expect(localStorage.getItem('token')).toBeNull();
});
