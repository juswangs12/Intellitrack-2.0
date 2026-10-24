import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import {MemoryRouter} from 'react-router-dom';
import SemesterHistory from './SemesterHistory';
import api from '../services/ApiService';
jest.mock('../services/ApiService',()=>({requestJson:jest.fn(),getGroups:jest.fn()}));
test('finish requires confirmation, preserves history, and can reopen',async()=>{
 global.IS_REACT_ACT_ENVIRONMENT=true;let archived=false;const term='2026-2027 / 1';
 api.getGroups.mockResolvedValue([]);
 api.requestJson.mockImplementation(async(url,options)=>{
  if(url==='/academic/semesters'){archived=JSON.parse(options.body).archived;return {};}
  if(url==='/academic')return {archivedTerms:archived?[term]:[],subjects:[],sections:[{id:1,term}],groups:[],deliverables:[]};
  return [{code:'CAP1',sections:[{id:1,academicTerm:term,section:'A',enrollments:[{id:1,fullName:'Test Student',progression:'PENDING'}]}]}];
 });
 const container=document.createElement('div'),root=createRoot(container);
 try {
  await act(async()=>root.render(<MemoryRouter future={{v7_startTransition:true,v7_relativeSplatPath:true}}><SemesterHistory onChanged={jest.fn()}/></MemoryRouter>));
  await act(async()=>Simulate.change(container.querySelector('select'),{target:{value:term}}));
  expect(container.querySelector('button').disabled).toBe(true);
  await act(async()=>Simulate.change(container.querySelector('input'),{target:{checked:true}}));
  await act(async()=>Simulate.click(container.querySelector('button')));
  expect(api.requestJson).toHaveBeenCalledWith('/academic/semesters',{method:'PUT',body:JSON.stringify({term,archived:true})});
  expect(container.textContent).toContain('Test Student');expect(container.textContent).toContain('Pending / INC');
  expect(container.textContent).toContain('Reopen semester');expect(container.textContent).toContain('read-only');
  await act(async()=>Simulate.click(container.querySelector('button')));
  expect(container.textContent).toContain('Semester reopened');
 } finally {act(()=>root.unmount());}
});
