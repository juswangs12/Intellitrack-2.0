import React, {act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import {MemoryRouter, useLocation} from 'react-router-dom';
import Login from './Login';
import {useAuth} from '../context/AuthContext';
jest.mock('../context/AuthContext',()=>({useAuth:jest.fn()}));
let root, container, login;
function Location(){return <output>{useLocation().pathname}</output>;}
beforeEach(()=>{
  global.IS_REACT_ACT_ENVIRONMENT=true;
  container=document.createElement('div');document.body.appendChild(container);root=createRoot(container);
  login=jest.fn();useAuth.mockReturnValue({login,loading:false,isAuthenticated:()=>false,user:null});
});
afterEach(()=>{act(()=>root.unmount());container.remove();});
test.each([['student','student'],['adviser','adviser'],['coordinator','coordinator'],['administrator','admin']])(
  '%s signs in through the shared screen and reaches its workspace',async(role,path)=>{
    login.mockResolvedValue({success:true,role});
    await act(async()=>root.render(<MemoryRouter future={{v7_startTransition:true,v7_relativeSplatPath:true}}><Login/><Location/></MemoryRouter>));
    expect(container.querySelector('[role="tablist"]')).toBeNull();
    expect(container.textContent).toContain('New, unregistered Google accounts start as students.');
    await act(async()=>Simulate.submit(container.querySelector('form')));
    expect(container.querySelector('output').textContent).toBe('/'+path+'/home');
  }
);
