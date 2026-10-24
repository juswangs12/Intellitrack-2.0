import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import {MemoryRouter} from 'react-router-dom';
import GroupProgression from './GroupProgression';
import api from '../services/ApiService';
jest.mock('../services/ApiService',()=>({requestJson:jest.fn()}));
test('one decision updates the whole group with one request',async()=>{
 global.IS_REACT_ACT_ENVIRONMENT=true;api.requestJson.mockResolvedValue({updated:2});
 const container=document.createElement('div'),root=createRoot(container),onChanged=jest.fn();
 const groups=[{id:5,code:'Alpha',academicTerm:'2026-2027 / 1',students:[{id:1,progression:'PENDING'},{id:2,progression:'ELIGIBLE'}]}];
 try {
  await act(async()=>root.render(<MemoryRouter future={{v7_startTransition:true,v7_relativeSplatPath:true}}><GroupProgression groups={groups} onChanged={onChanged}/></MemoryRouter>));
  await act(async()=>Simulate.change(container.querySelector('select'),{target:{value:'5'}}));
  expect(container.textContent).toContain('1 Pending / INC');expect(container.textContent).toContain('1 Eligible to proceed');
  const button=[...container.querySelectorAll('button')].find(b=>b.textContent==='Apply to whole group');expect(button.disabled).toBe(true);
  await act(async()=>Simulate.change(container.querySelector('input'),{target:{value:'Approved by professor'}}));
  await act(async()=>Simulate.click(button));
  expect(api.requestJson).toHaveBeenCalledWith('/academic/groups/5/progression',{method:'PUT',body:JSON.stringify({status:'ELIGIBLE',reason:'Approved by professor'})});
  expect(onChanged).toHaveBeenCalled();expect(container.textContent).toContain('Updated all 2 members');
 } finally {act(()=>root.unmount());}
});
