import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import {MemoryRouter} from 'react-router-dom';
import CoordinatorOverview,{overviewScope,manilaToday} from './CoordinatorOverview';
import api from '../../services/ApiService';
jest.mock('../../services/ApiService',()=>({requestJson:jest.fn()}));
const term='2026-2027 / 1';
const fixture={catalog:{subjects:[{id:1,code:'CAP1',name:'Capstone 1'},{id:2,code:'CAP2',name:'Capstone 2'}],sections:[{id:10,subjectId:1,term}],groups:[{id:20,subjectId:1,term}],deliverables:[{id:30,subjectId:1,term,name:'Proposal',active:true,dueAt:manilaToday()+'T23:59:00'},{id:31,subjectId:2,term,name:'Other subject',active:true,dueAt:manilaToday()+'T23:59:00'},{id:32,subjectId:1,term,name:'Archived',active:false,dueAt:manilaToday()+'T23:59:00'}]},rows:[{enrollmentId:40,studentId:50,studentName:'Alice',subjectId:1,subject:'CAP1',academicTerm:term,groupId:20,group:'Alpha',section:'A',deliverableId:30,deliverable:'Proposal',status:'OVERDUE',dueAt:'2020-01-01T12:00:00'}],subjects:[{id:1,sections:[{id:10,enrollments:[{id:40,userId:50}]}]}],awaiting:[{id:90}]};
let container,root;
beforeEach(()=>{global.IS_REACT_ACT_ENVIRONMENT=true;container=document.createElement('div');document.body.appendChild(container);root=createRoot(container);api.requestJson.mockImplementation(async path=>path==='/academic'?fixture.catalog:path==='/tracking'?{rows:fixture.rows}:path==='/subjects/with-sections'?fixture.subjects:fixture.awaiting);});
afterEach(()=>{act(()=>root.unmount());container.remove();});
async function render(calendar=false){await act(async()=>root.render(<MemoryRouter initialEntries={['/?subject=1']} future={{v7_startTransition:true,v7_relativeSplatPath:true}}><CoordinatorOverview calendar={calendar}/></MemoryRouter>));}
test('scope excludes other subjects and archived requirements',()=>{
  const scope=overviewScope(fixture,term,'1');expect(scope.requirements.map(d=>d.id)).toEqual([30]);expect(scope.enrollments).toHaveLength(1);expect(scope.overdue).toHaveLength(1);
  expect(overviewScope(fixture,'2027-2028 / 1','1').rows).toHaveLength(0);
});
test('coordinator dashboard shows follow-ups and scoped analytics links',async()=>{
  await render();expect(container.textContent).toContain('Coordinator dashboard');expect(container.textContent).toContain('Alice');expect(container.textContent).toContain('1 Google students awaiting enrollment');
  const link=[...container.querySelectorAll('a')].find(a=>a.textContent==='View all 1 overdue assignments');expect(link.getAttribute('href')).toContain('subject=1');expect(link.getAttribute('href')).toContain('status=OVERDUE');
  expect(new URL(link.href).searchParams.get('term')).toBe('');
});
test('legacy bookmarks match only unassigned academic terms',()=>{
  const data={...fixture,catalog:{...fixture.catalog,deliverables:[...fixture.catalog.deliverables,{id:99,subjectId:1,term:null,active:true}]}};
  expect(overviewScope(data,'legacy','1').requirements.map(d=>d.id)).toEqual([99]);
  expect(overviewScope(data,'legacy','1').enrollments).toHaveLength(0);
});
test('calendar shows requirements without enrollment and supports month navigation',async()=>{
  api.requestJson.mockImplementation(async path=>path==='/academic'?fixture.catalog:path==='/tracking'?{rows:[]}:[]);
  await render(true);expect(container.textContent).toContain('Proposal');expect(container.textContent).toContain('No enrolled students yet');expect(container.textContent).not.toContain('Archived');
  expect(container.querySelector(`button[aria-label="${manilaToday()}, 1 deadlines"]`)).toBeTruthy();
  await act(async()=>Simulate.click([...container.querySelectorAll('button')].find(b=>b.textContent==='Next month')));
  expect(container.textContent).toContain('0 deadlines this month');
});
test('coordinator calendar retains simultaneous requirements within and across subjects',async()=>{
 const catalog={...fixture.catalog,deliverables:[...fixture.catalog.deliverables,{id:33,subjectId:1,term,name:'Design',active:true,dueAt:manilaToday()+'T23:59:00'}]};
 api.requestJson.mockImplementation(async path=>path==='/academic'?catalog:path==='/tracking'?{rows:[]}:[]);
 await render(true);
 let day=container.querySelector(`button[aria-label="${manilaToday()}, 2 deadlines"]`);
 expect(day.textContent).toContain('Proposal');expect(day.textContent).toContain('Design');
 await act(async()=>Simulate.change(container.querySelectorAll('.tracking-filters select')[1],{target:{value:''}}));
 day=container.querySelector(`button[aria-label="${manilaToday()}, 3 deadlines"]`);
 expect(day.textContent).toContain('Other subject');expect(day.querySelectorAll('span')).toHaveLength(3);
});
test('calendar sync sends the complete catalog request even when the view is filtered',async()=>{
  const original=api.requestJson.getMockImplementation();
  api.requestJson.mockImplementation(async(path,options)=>path==='/google/status'?{calendar:true}:path==='/google/calendar/coordinator/sync'?{synced:2,calendarUrl:'https://calendar.google.com/calendar/u/0/r'}:original(path,options));
  await render(true);
  await act(async()=>Simulate.click([...container.querySelectorAll('button')].find(b=>b.textContent==='Sync all deadlines')));
  expect(api.requestJson).toHaveBeenCalledWith('/google/calendar/coordinator/sync',{method:'POST'});
  expect(container.textContent).toContain('2 deadlines synced');
  expect(container.querySelector('a[href="https://calendar.google.com/calendar/u/0/r"]')).toBeTruthy();
});
test('calendar shows sync errors and permits retry',async()=>{
  const original=api.requestJson.getMockImplementation();
  api.requestJson.mockImplementation(async(path,options)=>{if(path==='/google/status')return {calendar:true};if(path==='/google/calendar/coordinator/sync')throw new Error('Reconnect Google.');return original(path,options);});
  await render(true);
  const button=[...container.querySelectorAll('button')].find(b=>b.textContent==='Sync all deadlines');
  await act(async()=>Simulate.click(button));
  expect(container.querySelector('[role="alert"]').textContent).toContain('Reconnect Google.');expect(button.disabled).toBe(false);
});
