import React, {act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import {MemoryRouter} from 'react-router-dom';
import TrackingDashboard from './TrackingDashboard';
import api from '../services/ApiService';
import {useAuth} from '../context/AuthContext';
jest.mock('../services/ApiService',()=>({requestJson:jest.fn()}));
jest.mock('../context/AuthContext',()=>({useAuth:jest.fn()}));
const row={enrollmentId:1,studentId:1,studentNumber:'A001',studentName:'Alice',section:'A',subjectId:2,subject:'CAP1',academicTerm:'2026-2027 / 1',groupId:3,group:'Alpha',adviserId:4,adviser:'Adviser',deliverableId:5,deliverable:'Proposal',dueAt:'2026-10-01T23:59:00',status:'OVERDUE',linked:true,canSubmit:true};
let root,container;
beforeEach(()=>{
  global.IS_REACT_ACT_ENVIRONMENT=true;
  container=document.createElement('div');document.body.appendChild(container);root=createRoot(container);
  useAuth.mockReturnValue({user:{role:'coordinator'}});
  api.requestJson.mockReset();api.requestJson.mockImplementation(async url=>url.startsWith('/google/status')?{reports:true}:url.startsWith('/google/reports')?{url:'https://docs.google.com/spreadsheets/d/example'}:url.startsWith('/tracking/summary')?{source:'Computed summary',text:'One overdue assignment'}:{rows:[row],summary:{students:1,expected:1,OVERDUE:1}});
});
afterEach(()=>{act(()=>root.unmount());container.remove();});
async function render(entry='/'){await act(async()=>{root.render(<MemoryRouter initialEntries={[entry]} future={{v7_startTransition:true,v7_relativeSplatPath:true}}><TrackingDashboard/></MemoryRouter>);});await act(async()=>{await new Promise(r=>setTimeout(r,200));});}
async function click(text){const button=[...container.querySelectorAll('button')].find(b=>(b.textContent===text || b.getAttribute("aria-label")===text));expect(button).toBeTruthy();await act(async()=>{Simulate.click(button);});}
test('selected filters are shared by displayed rows, AI and export',async()=>{
  await render();
  const status=[...container.querySelectorAll('label')].find(l=>l.textContent.startsWith('Status')).querySelector('select');
  await act(async()=>{Simulate.change(status,{target:{value:'OVERDUE'}});});
  await act(async()=>{await new Promise(r=>setTimeout(r,200));});
  await click('Generate AI summary');await click('Export to Google Sheets');
  for(const path of ['/tracking?','/tracking/summary?','/google/reports/export?']){
    const calls=api.requestJson.mock.calls.map(c=>c[0]).filter(url=>url.startsWith(path));
    const filters=new URLSearchParams(calls[calls.length-1].split('?')[1]);
    expect(filters.get('status')).toBe('OVERDUE');expect(filters.get('term')).toBe('2026-2027 / 1');
  }
  expect(container.textContent).toContain('Computed summary');
});
test('subject selection stays available and clears incompatible group filters',async()=>{
 api.requestJson.mockResolvedValue({rows:[row,{...row,enrollmentId:2,subjectId:8,subject:'IT411',groupId:9,group:'Beta'}],summary:{}});
 await render('/admin/analytics?term=2026-2027+%2F+1&subject=2&group=3');
 await click('IT411');await act(async()=>{await new Promise(r=>setTimeout(r,200));});
 const path=api.requestJson.mock.calls.map(([url])=>url).filter(url=>url.startsWith('/tracking?')).pop();
 const filters=new URLSearchParams(path.split('?')[1]);expect(filters.get('subject')).toBe('8');expect(filters.has('group')).toBe(false);
 expect(container.querySelector('.analytics-scope-bar')).toBeTruthy();
});
test('group drill-down opens individual records with the same academic scope',async()=>{
 useAuth.mockReturnValue({user:{role:'adviser'}});
 await render();await click('Groups');await click('View individual records');
 await act(async()=>{await new Promise(r=>setTimeout(r,200));});
 const path=api.requestJson.mock.calls.map(([url])=>url).filter(url=>url.startsWith('/tracking?')).pop();
 const filters=new URLSearchParams(path.split('?')[1]);expect(filters.get('subject')).toBe('2');expect(filters.get('group')).toBe('3');expect(filters.get('deliverable')).toBe('5');
 expect(container.querySelector('.tracking-table')).toBeTruthy();
});
test('adviser defaults to member breakdown and has no coordinator export action',async()=>{
  useAuth.mockReturnValue({user:{role:'adviser'}});await render();
  expect(container.textContent).toContain('Alpha · Proposal');expect(container.textContent).toContain('Alice');
  expect(container.textContent).not.toContain('Export to Google Sheets');
  expect(container.textContent).not.toContain('Submit link');
});

test('student sees requirement cards and submission action without reporting controls',async()=>{
  useAuth.mockReturnValue({user:{role:'student'}});await render();
  expect(container.querySelector('.student-requirement').textContent).toContain('Proposal');
  expect(container.querySelector('a[href="/student/submissions/5?group=3"]')).toBeTruthy();
  expect(container.textContent).not.toContain('Generate AI summary');
  expect(container.textContent).not.toContain('Export to Google Sheets');
  expect(container.querySelectorAll('fieldset select')).toHaveLength(3);
});

test('student can select historical terms and sees read-only submission history',async()=>{
  useAuth.mockReturnValue({user:{role:'student'}});
  api.requestJson.mockResolvedValue({rows:[{...row,canSubmit:false,submissionId:7,submissionBlockReason:'This semester is archived.'}],summary:{},terms:['2025-2026 / 1',row.academicTerm]});
  await render();
  expect(container.textContent).toContain('2025-2026 / 1');
  expect(container.textContent).toContain('This semester is archived.');
  expect(container.textContent).not.toContain('Submit Google Docs link');
  expect(container.querySelector('a[href*="term=2026-2027"]')).toBeTruthy();
});
test('empty coordinator dashboard guides setup and disables empty AI and exports',async()=>{
  api.requestJson.mockResolvedValue({rows:[],summary:{}});await render();
  expect(container.textContent).toContain('Getting started with a new semester');
  expect(container.querySelector('a[href="/coordinator/classlist"]')).toBeTruthy();
  for(const label of ['Generate AI summary','Export to Google Sheets'])expect([...container.querySelectorAll('button')].find(b=>b.textContent===label).disabled).toBe(true);
  const last=api.requestJson.mock.calls.map(c=>c[0]).filter(url=>url.startsWith('/tracking?')).pop();
  expect(new URLSearchParams(last.split('?')[1]).get('term')).toBeNull();
});
test('returning from Google and refreshing preserve selected academic scope',async()=>{
  await render('/coordinator/home?term=2025-2026+%2F+2&subject=2');
  await click('Refresh');await act(async()=>{await new Promise(r=>setTimeout(r,200));});
  const last=api.requestJson.mock.calls.map(c=>c[0]).filter(url=>url.startsWith('/tracking?')).pop();
  expect(new URLSearchParams(last.split('?')[1]).get('term')).toBe('2025-2026 / 2');
  expect(new URLSearchParams(last.split('?')[1]).get('subject')).toBe('2');
});
test('export refuses mixed subjects before connecting Google',async()=>{
  api.requestJson.mockResolvedValue({rows:[row,{...row,enrollmentId:2,subjectId:3,subject:'CAP2'}],summary:{}});
  await render();await click('Export to Google Sheets');
  expect(container.textContent).toContain('Choose one subject and academic term before exporting');
  expect(api.requestJson.mock.calls.some(([url])=>url.startsWith('/google/'))).toBe(false);
});

test('adviser empty state does not send advisers to staff setup controls',async()=>{
 useAuth.mockReturnValue({user:{role:'adviser'}});
 api.requestJson.mockResolvedValue({rows:[],summary:{}});
 await render();
 expect(container.textContent).toContain('Your coordinator manages group assignments and deadlines.');
 expect(container.textContent).not.toContain('Assign terms and requirements in Academic setup');
});

 test('coordinator overview combines deliverables without repeating groups or students',async()=>{
  api.requestJson.mockResolvedValue({rows:[row,{...row,deliverableId:6,deliverable:'SDD',status:'SUBMITTED_ON_TIME',submissionId:12},{...row,enrollmentId:2,studentName:'Bob',studentNumber:'B002'},{...row,enrollmentId:2,studentName:'Bob',studentNumber:'B002',deliverableId:6,deliverable:'SDD'}],summary:{}});
  await render();
  expect(container.querySelectorAll('.group-open')).toHaveLength(1);
  expect(container.querySelector('.group-overview').textContent).toContain('1/2 submitted');
  expect(container.querySelector('.group-overview').textContent).toContain('0/2 submitted');
  await click('Alpha');
  const details=container.querySelector('.group-detail-panel');
  expect(details.querySelectorAll('tbody > tr')).toHaveLength(2);
  expect(details.textContent).toContain('Proposal');expect(details.textContent).toContain('SDD');
  expect(container.querySelector('[role="tooltip"]')).toBeNull();
 });
 test('overview keeps subjects and terms separate even with the same group name',async()=>{
  api.requestJson.mockResolvedValue({rows:[row,{...row,subjectId:8,subject:'IT411'},{...row,academicTerm:'2025-2026 / 2'}],summary:{}});
  await render();expect(container.querySelectorAll('.group-open')).toHaveLength(3);
 });
 test('attention toggle hides groups without overdue work',async()=>{
  api.requestJson.mockResolvedValue({rows:[row,{...row,enrollmentId:2,groupId:8,group:'Beta',status:'AWAITING_SUBMISSION'}],summary:{}});
  await render();expect(container.querySelectorAll('.group-open')).toHaveLength(2);
  await act(async()=>Simulate.change(container.querySelector('.group-overview input[type="checkbox"]'),{target:{checked:true}}));
  expect(container.querySelectorAll('.group-open')).toHaveLength(1);
  expect(container.querySelector('.group-open').getAttribute('aria-label')).toBe('Alpha');
 });

test('one group dropdown expands across the full row without repeated requirement lists',async()=>{
 api.requestJson.mockResolvedValue({rows:[row,{...row,deliverableId:6,deliverable:'SDD'}],summary:{}});
 await render();
 expect(container.querySelectorAll('.group-detail-panel')).toHaveLength(0);
 await click('Alpha');
 expect(container.querySelector('.group-open').getAttribute('aria-expanded')).toBe('true');
 expect(container.querySelectorAll('.group-detail-panel')).toHaveLength(1);
 expect(container.querySelector('.group-expanded-row > td').colSpan).toBe(5);
 expect(container.querySelectorAll('.group-detail-table tbody tr')).toHaveLength(1);
 expect(container.querySelectorAll('.group-detail-table thead th')).toHaveLength(3);
 expect(container.querySelectorAll('.group-overview-table > table > tbody > tr:first-child button')).toHaveLength(1);
 expect(container.querySelector('.group-detail-table').textContent).toContain('Alice');
 expect(container.querySelector('.group-detail-table details')).toBeNull();
 await click('Alpha');expect(container.querySelector('.group-detail-panel')).toBeNull();
 expect(container.querySelector('.group-open').getAttribute('aria-expanded')).toBe('false');
});
