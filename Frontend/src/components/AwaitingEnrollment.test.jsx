import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import {MemoryRouter} from 'react-router-dom';
import AwaitingEnrollment from './AwaitingEnrollment';
import api from '../services/ApiService';
jest.mock('../services/ApiService',()=>({requestJson:jest.fn(),linkUserToEnrollment:jest.fn(),addStudentManually:jest.fn()}));
test.each(['section:2','link:3'])('coordinator resolves a Google student using %s',async choice=>{
  global.IS_REACT_ACT_ENVIRONMENT=true;
  api.requestJson.mockReset();api.linkUserToEnrollment.mockReset();api.addStudentManually.mockReset();
  api.requestJson.mockResolvedValueOnce([{id:1,name:'Alice Test',email:'alice@example.test'}]).mockResolvedValue([]);
  const onChanged=jest.fn();const container=document.createElement('div');document.body.appendChild(container);const root=createRoot(container);
  try{
    await act(async()=>root.render(<MemoryRouter future={{v7_startTransition:true,v7_relativeSplatPath:true}}><AwaitingEnrollment onChanged={onChanged} subjects={[{code:'CAP1',sections:[{id:2,section:'A · 2026-2027 / 1',enrollments:[{id:3,fullName:'Alice Test',email:'alice@example.test',userId:null}]}]}]}/></MemoryRouter>));
    expect(container.textContent).toContain('alice@example.test');
    await act(async()=>Simulate.change(container.querySelector('select'),{target:{value:choice}}));
    await act(async()=>Simulate.click([...container.querySelectorAll('button')].find(b=>b.textContent==='Confirm enrollment')));
    if(choice==='section:2')expect(api.addStudentManually).toHaveBeenCalledWith({userId:1,classSectionId:2});
    else expect(api.linkUserToEnrollment).toHaveBeenCalledWith(3,1);
    expect(onChanged).toHaveBeenCalled();expect(container.textContent).toContain('No Google students are awaiting enrollment.');
  }finally{act(()=>root.unmount());container.remove();}
});
