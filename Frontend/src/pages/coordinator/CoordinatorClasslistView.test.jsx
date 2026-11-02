import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {MemoryRouter} from 'react-router-dom';
import Page from './CoordinatorClasslistView';
import api from '../../services/ApiService';
jest.mock('../../services/ApiService',()=>({requestJson:jest.fn(),getAllUsers:jest.fn(),moveStudent:jest.fn()}));
jest.mock('../../components/AwaitingEnrollment',()=>()=>null);
test('move includes other approved subjects and active terms even when source has one section',async()=>{
 global.IS_REACT_ACT_ENVIRONMENT=true;const container=document.createElement('div');document.body.appendChild(container);const root=createRoot(container);
 api.getAllUsers.mockResolvedValue([]);api.requestJson.mockResolvedValue([
 {id:1,code:'IT322',name:'Capstone 1',sections:[{id:1,section:'G1',academicTerm:'2026-2027 / 1',enrollments:[{id:1,fullName:'Test Student'}]}]},
 {id:2,code:'IT332',name:'Capstone 1',sections:[{id:2,section:'G01',academicTerm:'2026-2027 / 1'},{id:3,section:'G1',academicTerm:'2026-2027 / 2'},{id:4,section:'G2',academicTerm:'2025-2026 / 2',semesterArchived:true}]},
 {id:3,code:'IT411',name:'Capstone 2',sections:[{id:5,section:'G1',academicTerm:'2026-2027 / 1'}]}]);
 try{
 await act(async()=>root.render(<MemoryRouter><Page/></MemoryRouter>));
 const headings=container.querySelectorAll('h2');
 await act(async()=>Array.from(headings).find(h=>h.textContent.includes('IT322')).parentElement.click());
 await act(async()=>Array.from(container.querySelectorAll('h3')).find(h=>h.textContent.includes('G1')).parentElement.click());
 await act(async()=>Array.from(container.querySelectorAll('button')).find(b=>b.textContent.trim()==='Move').click());
 const select=container.querySelector('select.form-select');expect(select.textContent).toContain('IT332');expect(select.textContent).toContain('IT411');expect(select.textContent).toContain('2026-2027 / 2');expect(select.textContent).not.toContain('2025-2026');expect(select.textContent).not.toContain('IT322');
 }finally{act(()=>root.unmount());container.remove();}
});
