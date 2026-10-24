import React, {act} from 'react';
import {createRoot} from 'react-dom/client';
import {MemoryRouter,Routes,Route} from 'react-router-dom';
import SubmissionLink from './SubmissionLink';
import api from '../services/ApiService';

jest.mock('../services/ApiService',()=>({requestJson:jest.fn()}));

test('history distinguishes current compliance from the originally recorded status after an extension',async()=>{
  global.IS_REACT_ACT_ENVIRONMENT=true;
  const container=document.createElement('div');document.body.appendChild(container);
  const root=createRoot(container);
  api.requestJson.mockImplementation(async url=>url.endsWith('/history')
    ?[{version:1,submittedAt:'2026-10-01T10:00:00',status:'SUBMITTED_ON_TIME',recordedStatus:'SUBMITTED_LATE'}]
    :{rows:[{deliverableId:5,groupId:3,submissionId:7,deliverable:'Proposal',subject:'CAP1',group:'Alpha',dueAt:'2026-10-02T23:59:00',status:'SUBMITTED_ON_TIME'}]});
  try{
    await act(async()=>{root.render(<MemoryRouter initialEntries={['/student/submissions/5?group=3']} future={{v7_startTransition:true,v7_relativeSplatPath:true}}><Routes><Route path="/student/submissions/:deliverableId" element={<SubmissionLink/>}/></Routes></MemoryRouter>);});
    expect(container.textContent).toContain('Statuses below use the current deadline, including extensions.');
    expect(container.querySelector('ol').textContent).toContain('On time (originally recorded: Late)');
  }finally{act(()=>root.unmount());container.remove();}
});
