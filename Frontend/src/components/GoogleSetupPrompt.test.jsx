import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import GoogleSetupPrompt from './GoogleSetupPrompt';
import api from '../services/ApiService';
import {connectGoogle} from '../pages/TrackingDashboard';
jest.mock('../services/ApiService',()=>({requestJson:jest.fn()}));
jest.mock('../pages/TrackingDashboard',()=>({connectGoogle:jest.fn(()=>Promise.resolve())}));
let root,container;
beforeEach(()=>{localStorage.clear();global.IS_REACT_ACT_ENVIRONMENT=true;HTMLDialogElement.prototype.showModal=function(){this.open=true;};HTMLDialogElement.prototype.close=function(){this.open=false;};container=document.createElement('div');document.body.appendChild(container);root=createRoot(container);api.requestJson.mockResolvedValue({configured:true,calendar:false,imports:false,reports:false});});
afterEach(()=>{act(()=>root.unmount());container.remove();jest.clearAllMocks();});
test('offers role tools once and lets a student defer without blocking login',async()=>{
 await act(async()=>root.render(<GoogleSetupPrompt user={{id:1,role:'student'}}/>));expect(container.textContent).toContain('deadline calendar');
 await act(async()=>Array.from(container.querySelectorAll('button')).find(b=>b.textContent==='Not now').click());
 expect(container.querySelector('dialog')).toBeNull();expect(localStorage.getItem('google-setup-v1:1:student')).toBe('offered');expect(connectGoogle).not.toHaveBeenCalled();
});
test('coordinator uses one role-scoped consent and connected users see no prompt',async()=>{
 await act(async()=>root.render(<GoogleSetupPrompt user={{id:2,role:'coordinator'}}/>));
 await act(async()=>Array.from(container.querySelectorAll('button')).find(b=>b.textContent==='Continue with Google').click());expect(connectGoogle).toHaveBeenCalledWith('coordinator-tools');
 act(()=>root.unmount());root=createRoot(container);api.requestJson.mockResolvedValue({configured:true,calendar:true,imports:true,reports:true});
 await act(async()=>root.render(<GoogleSetupPrompt user={{id:3,role:'coordinator'}}/>));expect(container.querySelector('dialog')).toBeNull();
});
