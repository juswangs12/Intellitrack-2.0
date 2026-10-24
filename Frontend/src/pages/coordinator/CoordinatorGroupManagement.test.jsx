import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import {MemoryRouter} from 'react-router-dom';
import CoordinatorGroupManagement from './CoordinatorGroupManagement';
import api from '../../services/ApiService';
jest.mock('../../services/ApiService',()=>({getGroups:jest.fn(),getAllUsers:jest.fn(),getAllStudentEnrollments:jest.fn(),requestJson:jest.fn()}));
test('active groups can be searched without exposing retired outcome controls',async()=>{
 global.IS_REACT_ACT_ENVIRONMENT=true;
 api.getGroups.mockResolvedValue([{id:1,code:'Alpha',title:'Alpha',academicTerm:'2027-2028 / 1',students:[]},{id:2,code:'Old',title:'Archived group',semesterArchived:true,students:[]}]);
 api.getAllUsers.mockResolvedValue([]);api.getAllStudentEnrollments.mockResolvedValue([]);api.requestJson.mockResolvedValue([]);
 const container=document.createElement('div'),root=createRoot(container);
 try {
  await act(async()=>root.render(<MemoryRouter future={{v7_startTransition:true,v7_relativeSplatPath:true}}><CoordinatorGroupManagement/></MemoryRouter>));
  expect(container.textContent).toContain('Alpha');
  expect(container.textContent).not.toContain('Archived group');
  expect(container.textContent).not.toContain('Group progression');
  await act(async()=>Simulate.change(container.querySelector('input[type="search"]'),{target:{value:'missing'}}));
  expect(container.textContent).toContain('No groups match your search.');
 } finally {act(()=>root.unmount());}
});
