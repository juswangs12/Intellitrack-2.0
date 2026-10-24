import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import UserManagement from './UserManagement';
import api from '../../services/ApiService';
jest.mock('../../services/ApiService',()=>({requestJson:jest.fn()}));
let root,container;
beforeEach(()=>{global.IS_REACT_ACT_ENVIRONMENT=true;container=document.createElement('div');document.body.appendChild(container);root=createRoot(container);api.requestJson.mockResolvedValue([{id:1,firstName:'Alex',lastName:'Rivera',email:'alex@test.invalid',role:'adviser'}]);});
afterEach(()=>{act(()=>root.unmount());container.remove();jest.clearAllMocks();});
test('full name search matches staff accounts',async()=>{
 await act(async()=>root.render(<UserManagement/>));
 const search=container.querySelector('input');
 await act(async()=>Simulate.change(search,{target:{value:' Alex Rivera '}}));
 expect(container.querySelector('tbody').textContent).toContain('Alex Rivera');
});
test('account editor exposes a labelled dialog and closes on Escape',async()=>{
 await act(async()=>root.render(<UserManagement/>));
 const edit=container.querySelector('button[aria-label="Edit Alex Rivera"]');edit.focus();
 await act(async()=>Simulate.click(edit));
 const dialog=container.querySelector('[role="dialog"]');expect(dialog).not.toBeNull();
 expect(dialog.getAttribute('aria-labelledby')).toBe('account-dialog-title');
 expect(container.querySelector('label[for="account-firstName"]')).not.toBeNull();
 await act(async()=>dialog.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true})));
 expect(container.querySelector('[role="dialog"]')).toBeNull();expect(document.activeElement).toBe(edit);
});
