import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import AdminUsage from './AdminUsage';
import api from '../../services/ApiService';
jest.mock('../../services/ApiService',()=>({requestJson:jest.fn()}));
let root,container;
beforeEach(()=>{global.IS_REACT_ACT_ENVIRONMENT=true;container=document.createElement('div');document.body.appendChild(container);root=createRoot(container);});
afterEach(()=>{act(()=>root.unmount());container.remove();jest.clearAllMocks();});
test('unknown integration success and empty sign-in history are labelled honestly',async()=>{
 api.requestJson.mockResolvedValue({totalUsers:4,activeToday:1,activeWeek:2,registrations:[{date:'2026-10-23',count:2}],academic:{terms:1,subjects:2,groups:2,submissions:3},database:'Reachable',integrations:[{purpose:'calendar',connected:1,lastSuccess:null}],recentSignIns:[]});
 await act(async()=>root.render(<AdminUsage/>));
 expect(container.textContent).toContain('Not recorded yet');expect(container.textContent).toContain('No sign-ins recorded');
 expect(container.textContent).toContain('Active in 7 days');
});
test('failed metrics do not display zeros as successful results',async()=>{
 api.requestJson.mockRejectedValue(new Error('Unavailable'));
 await act(async()=>root.render(<AdminUsage/>));
 expect(container.querySelector('[role="alert"]').textContent).toBe('Unavailable');expect(container.querySelector('.admin-account-grid')).toBeNull();
});
