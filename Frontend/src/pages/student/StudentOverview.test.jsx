import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import {MemoryRouter} from 'react-router-dom';
import StudentOverview from './StudentOverview';
import api from '../../services/ApiService';
jest.mock('../../services/ApiService',()=>({requestJson:jest.fn()}));
jest.mock('../../context/AuthContext',()=>({useAuth:()=>({user:{firstName:'Alice',role:'student'}})}));
let container,root;
const term='2026-2027 / 1';
beforeEach(()=>{global.IS_REACT_ACT_ENVIRONMENT=true;container=document.createElement('div');root=createRoot(container);api.requestJson.mockReset();api.requestJson.mockImplementation(async path=>path==='/tracking'?{rows:[{enrollmentId:1,deliverableId:2,deliverable:'Proposal',groupId:3,group:'Alpha',subject:'CAP1',academicTerm:term,dueAt:'2026-10-30T23:59:00',status:'AWAITING_SUBMISSION',canSubmit:true}],terms:[term],summary:{}}:path==='/tracking/summary'?{text:'Your proposal is awaiting submission.',source:'Computed summary',notice:'Tracking only'}:path==='/google/status'?{calendar:true}:{synced:1});});
afterEach(()=>act(()=>root.unmount()));
async function render(calendar=false){await act(async()=>root.render(<MemoryRouter future={{v7_startTransition:true,v7_relativeSplatPath:true}}><StudentOverview calendar={calendar}/></MemoryRouter>));}
test('dashboard generates a student-scoped summary and links to their requirement',async()=>{
 await render();expect(container.textContent).toContain('Welcome, Alice');expect(container.querySelector('a[href*="/student/submissions/2"]')).toBeTruthy();
 await act(async()=>Simulate.click([...container.querySelectorAll('button')].find(b=>b.textContent==='Generate summary')));
 expect(api.requestJson).toHaveBeenCalledWith('/tracking/summary',{method:'POST'});expect(container.textContent).toContain('Your proposal is awaiting submission.');
});
test('calendar shows selected-day deadlines and uses the student sync endpoint',async()=>{
 await render(true);
 await act(async()=>Simulate.change(container.querySelector('input[type="month"]'),{target:{value:'2026-10'}}));
 await act(async()=>Simulate.click(container.querySelector('[aria-label="2026-10-30, 1 deadlines"]')));
 expect(container.textContent).toContain('Deadlines on 2026-10-30');
 await act(async()=>Simulate.click([...container.querySelectorAll('button')].find(b=>b.textContent==='Update calendar')));
 expect(api.requestJson).toHaveBeenCalledWith('/google/calendar/sync?term=2026-2027%20%2F%201',{method:'POST'});
 expect(container.textContent).toContain('1 deadlines updated');
});
test('calendar preserves every deliverable at the same deadline',async()=>{
 const rows=[1,2,3].map(id=>({enrollmentId:1,deliverableId:id,deliverable:'Document '+id,subject:'IT332',academicTerm:term,dueAt:'2026-11-05T23:59:00',status:'AWAITING_SUBMISSION'}));
 api.requestJson.mockImplementation(async path=>path==='/tracking'?{rows,terms:[term],summary:{}}:[]);
 await render(true);await act(async()=>Simulate.change(container.querySelector('input[type="month"]'),{target:{value:'2026-11'}}));
 const cell=container.querySelector('[aria-label="2026-11-05, 3 deadlines"]');
 for(const row of rows)expect(cell.textContent).toContain(row.deliverable);
 await act(async()=>Simulate.click(cell));
 expect(container.querySelectorAll('.student-calendar-agendas section')[0].querySelectorAll('article')).toHaveLength(3);
});
