import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {MemoryRouter} from 'react-router-dom';
import AdviserOverview from './AdviserOverview';
import api from '../../services/ApiService';
jest.mock('../../services/ApiService',()=>({requestJson:jest.fn()}));
let container,root;
beforeEach(()=>{global.IS_REACT_ACT_ENVIRONMENT=true;container=document.createElement('div');root=createRoot(container);api.requestJson.mockReset();});
afterEach(()=>act(()=>root.unmount()));
async function render(){await act(async()=>root.render(<MemoryRouter><AdviserOverview/></MemoryRouter>));}
test('dashboard summarizes requirements without repeating groups',async()=>{
 api.requestJson.mockResolvedValue({rows:[{enrollmentId:1,studentId:2,groupId:3,group:'GROUP-17',subjectId:4,subject:'IT332',academicTerm:'2026-2027 / 1',deliverableId:5,deliverable:'Proposal',status:'OVERDUE',dueAt:'2020-01-01T12:00:00'},{enrollmentId:1,studentId:2,groupId:3,group:'GROUP-17',subjectId:4,subject:'IT332',academicTerm:'2026-2027 / 1',deliverableId:6,deliverable:'Design',status:'SUBMITTED_ON_TIME',submissionId:10}]});
 await render();expect(api.requestJson).toHaveBeenCalledWith('/tracking');expect(container.textContent).toContain('Adviser dashboard');
 expect(container.textContent.match(/GROUP-17/g)).toHaveLength(1);expect(container.textContent).toContain('1 / 2 submitted');
 expect(container.querySelector('a[href="/adviser/review?status=OVERDUE"]')).toBeTruthy();
});
test('empty assignment has an explicit state',async()=>{
 api.requestJson.mockResolvedValue({rows:[]});await render();expect(container.textContent).toContain('No assigned requirements yet');
});

test('failed requests show a retryable error instead of an empty success',async()=>{
 api.requestJson.mockRejectedValue(new Error('Unable to load your groups'));await render();
 expect(container.querySelector('[role="alert"]').textContent).toContain('Unable to load your groups');
 expect(container.textContent).not.toContain('No assigned requirements yet');
});
