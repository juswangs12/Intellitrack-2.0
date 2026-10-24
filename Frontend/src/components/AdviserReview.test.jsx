import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import AdviserReview from './AdviserReview';
import api from '../services/ApiService';
jest.mock('../services/ApiService',()=>({requestJson:jest.fn()}));
let root,container;
beforeEach(()=>{global.IS_REACT_ACT_ENVIRONMENT=true;container=document.createElement('div');document.body.appendChild(container);root=createRoot(container);});
afterEach(()=>{act(()=>root.unmount());container.remove();jest.clearAllMocks();});
test('shows follow-ups and fetches the selected submission history',async()=>{
 const rows=[{enrollmentId:1,deliverableId:2,studentName:'Alice',group:'Alpha',deliverable:'Proposal',status:'OVERDUE'}, {enrollmentId:3,deliverableId:2,studentName:'Bob',group:'Beta',deliverable:'Proposal',status:'SUBMITTED_ON_TIME',submissionId:7}];
 api.requestJson.mockResolvedValue([{version:1,status:'SUBMITTED_ON_TIME',googleDocUrl:'https://docs.google.com/document/d/test/edit'}]);
 await act(async()=>root.render(<AdviserReview rows={rows}/>));
 expect(container.textContent).toContain('Needs attention (1)');
 await act(async()=>Simulate.change(container.querySelector('select'),{target:{value:'3:2'}}));
 expect(api.requestJson).toHaveBeenCalledWith('/tracking/7/history');
 expect(container.textContent).toContain('Version 1');
 expect(container.querySelector('a').href).toContain('/document/d/test/edit');
});
