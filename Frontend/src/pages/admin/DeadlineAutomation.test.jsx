import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import DeadlineAutomation from './DeadlineAutomation';
import api from '../../services/ApiService';
import {connectGoogle} from '../TrackingDashboard';
jest.mock('../../services/ApiService',()=>({requestJson:jest.fn()}));
jest.mock('../TrackingDashboard',()=>({connectGoogle:jest.fn(()=>Promise.resolve())}));
let root,container;
beforeEach(()=>{global.IS_REACT_ACT_ENVIRONMENT=true;container=document.createElement('div');document.body.appendChild(container);root=createRoot(container);});
afterEach(()=>{act(()=>root.unmount());container.remove();jest.clearAllMocks();});
test('shows sender and review state and connects mail purpose',async()=>{
 connectGoogle.mockResolvedValue(undefined);
 api.requestJson.mockResolvedValue({enabled:true,connected:false,sender:'sender@example.com',pending:2,sent:3,review:1});
 await act(async()=>root.render(<DeadlineAutomation/>));
 expect(container.querySelector('.automation-counts').textContent).toContain('Queued2');
 expect(container.textContent).toContain('not retried automatically');
 await act(async()=>Array.from(container.querySelectorAll('button')).find(b=>b.textContent==='Connect Gmail sender').click());
 expect(connectGoogle).toHaveBeenCalledWith('mail');
});
test('connected Gmail shows reconnect separately from disabled delivery',async()=>{
 api.requestJson.mockResolvedValue({enabled:false,connected:true,sender:'sender@example.com',pending:0,sent:0,review:0});
 await act(async()=>root.render(<DeadlineAutomation/>));
 expect(container.textContent).toContain('Not enabled yet');
 expect(container.textContent).toContain('Reconnect Gmail');
 expect(container.textContent).not.toContain('Connect the designated Google account');
 expect(container.querySelector('details').open).toBe(false);
});
