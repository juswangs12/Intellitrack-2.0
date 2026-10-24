import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import {MemoryRouter} from 'react-router-dom';
import StudentGroups,{EnrollmentStatus} from './StudentGroups';
import api from '../../services/ApiService';
jest.mock('../../services/ApiService',()=>({requestJson:jest.fn()}));
let container,root;
beforeEach(()=>{global.IS_REACT_ACT_ENVIRONMENT=true;container=document.createElement('div');root=createRoot(container);});
afterEach(()=>act(()=>root.unmount()));
async function render(node){await act(async()=>root.render(<MemoryRouter future={{v7_startTransition:true,v7_relativeSplatPath:true}}>{node}</MemoryRouter>));}
test('distinguishes unlinked, archived, and incomplete enrollment',async()=>{
 await render(<EnrollmentStatus enrollments={[]}/>);expect(container.textContent).toContain('Awaiting enrollment');
 await render(<EnrollmentStatus enrollments={[{archived:true}]}/>);expect(container.textContent).toContain('No active semester');
 await render(<EnrollmentStatus enrollments={[{subject:'CAP1',section:'A',groups:[],requirements:0}]}/>);
 expect(container.textContent).toContain('No group assigned:');expect(container.textContent).toContain('No requirements yet:');
});
test('shows roster without requirements and keeps archived enrollment in history',async()=>{
 const enrollment={id:1,subjectId:10,subject:'CAP1',subjectName:'Capstone 1',section:'A',term:'2026-2027 / 1',archived:false,requirements:0,groups:[{id:2,name:'Alpha',adviser:'Professor Test',members:[{name:'Alice',section:'A',self:true}]}]};
 api.requestJson.mockResolvedValue([enrollment,{...enrollment,id:3,term:'2025-2026 / 1',archived:true,requirements:1}]);
 await render(<StudentGroups/>);
 expect(container.textContent).toContain('Professor Test');expect(container.textContent).toContain('Alice');expect(container.textContent).not.toContain('2025-2026');
 await act(async()=>Simulate.click([...container.querySelectorAll('button')].find(b=>b.textContent==='Semester history')));
 expect(container.textContent).toContain('Archived · read-only');
 expect(container.querySelector('a').getAttribute('href')).toContain('subject=10');
 expect(container.textContent).not.toContain('2026-2027');
});
