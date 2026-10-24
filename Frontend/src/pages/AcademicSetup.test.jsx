import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import {MemoryRouter} from 'react-router-dom';
import AcademicSetup from './AcademicSetup';
import api from '../services/ApiService';
jest.mock('../services/ApiService',()=>({requestJson:jest.fn()}));
jest.mock('../context/AuthContext',()=>({useAuth:()=>({user:{role:'coordinator'}})}));
test('fresh setup creates a subject and selects it for section creation',async()=>{
  global.IS_REACT_ACT_ENVIRONMENT=true;let subjects=[];
  api.requestJson.mockImplementation(async(url,options)=>{
    if(url==='/academic/subjects'){subjects=[{id:9,...JSON.parse(options.body)}];return {id:9};}
    return {subjects,sections:[],groups:[],deliverables:[]};
  });
  const container=document.createElement('div');document.body.appendChild(container);const root=createRoot(container);
  try{
    await act(async()=>root.render(<MemoryRouter future={{v7_startTransition:true,v7_relativeSplatPath:true}}><AcademicSetup/></MemoryRouter>));
    const forms=container.querySelectorAll('form');
    const inputs=forms[0].querySelectorAll('input');
    await act(async()=>{Simulate.change(inputs[0],{target:{value:'CAP1'}});});
    await act(async()=>{Simulate.change(inputs[1],{target:{value:'Capstone 1'}});});
    await act(async()=>{Simulate.submit(forms[0]);});
    expect(api.requestJson).toHaveBeenCalledWith('/academic/subjects',expect.objectContaining({method:'POST',body:JSON.stringify({code:'CAP1',name:'Capstone 1'})}));
    const subject=[...container.querySelectorAll('label')].find(l=>l.textContent.startsWith('SubjectChoose'));
    expect(subject.querySelector('select').value).toBe('9');
    expect([...container.querySelectorAll('button')].find(b=>b.textContent==='Create section').disabled).toBe(false);
    expect(container.querySelector('details.manual-setup')).toBeTruthy();
    expect(container.textContent).toContain('Subjects and sections are created when you import a classlist.');
  }finally{act(()=>root.unmount());container.remove();}
});
