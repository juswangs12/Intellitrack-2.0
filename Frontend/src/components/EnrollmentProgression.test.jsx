import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import EnrollmentProgression from './EnrollmentProgression';
import api from '../services/ApiService';
jest.mock('../services/ApiService',()=>({requestJson:jest.fn()}));
test('progression requires a reason and saves the chosen decision',async()=>{
 global.IS_REACT_ACT_ENVIRONMENT=true;
 const container=document.createElement('div');const root=createRoot(container);const onChanged=jest.fn();
 api.requestJson.mockResolvedValue({});
 try {
  await act(async()=>root.render(<EnrollmentProgression enrollment={{id:4,progression:'PENDING'}} sections={[]} onChanged={onChanged}/>));
  const save=[...container.querySelectorAll('button')].find(b=>b.textContent==='Save progression');expect(save.disabled).toBe(true);
  await act(async()=>{Simulate.change(container.querySelector('select'),{target:{value:'ELIGIBLE'}});Simulate.change(container.querySelector('input'),{target:{value:'Professor cleared requirements'}});});
  await act(async()=>Simulate.click(save));
  expect(api.requestJson).toHaveBeenCalledWith('/academic/enrollments/4/progression',{method:'PUT',body:JSON.stringify({status:'ELIGIBLE',reason:'Professor cleared requirements'})});
  expect(onChanged).toHaveBeenCalled();
 } finally {act(()=>root.unmount());}
});
test('repeat is a recorded outcome without enrollment creation and removal stays separate',async()=>{
 global.IS_REACT_ACT_ENVIRONMENT=true;
 const container=document.createElement('div');const root=createRoot(container);
 try {
  await act(async()=>root.render(<EnrollmentProgression enrollment={{id:4,progression:'REPEAT',academicTerm:'2026-2027 / 1',removedFromGroup:true}} sections={[{id:1,section:'Old',academicTerm:'2025-2026 / 2'},{id:2,section:'Current',academicTerm:'2026-2027 / 1'},{id:3,section:'Next',academicTerm:'2026-2027 / 2'}]} onChanged={jest.fn()}/>));
  expect(container.querySelectorAll('select')).toHaveLength(1);
  expect(container.textContent).not.toContain('Create repeat enrollment');expect(container.textContent).toContain('Progression is assessed separately');
 } finally {act(()=>root.unmount());}
});
