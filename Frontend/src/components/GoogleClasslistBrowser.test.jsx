import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {Simulate} from 'react-dom/test-utils';
import Browser from './GoogleClasslistBrowser';
import api from '../services/ApiService';
jest.mock('../services/ApiService',()=>({requestJson:jest.fn()}));
jest.mock('../pages/TrackingDashboard',()=>({connectGoogle:jest.fn()}));
test('pagination keeps the query that produced its token when search text is edited',async()=>{
  global.IS_REACT_ACT_ENVIRONMENT=true;
  const container=document.createElement('div');const root=createRoot(container);
  api.requestJson.mockResolvedValueOnce({files:[{id:'a',name:'First'}],nextPageToken:'page-two'}).mockResolvedValueOnce({files:[{id:'b',name:'Second'}],nextPageToken:''});
  try {
    await act(async()=>root.render(<Browser onSelect={()=>{}}/>));
    await act(async()=>Simulate.change(container.querySelector('input'),{target:{value:'Unsubmitted search'}}));
    await act(async()=>Array.from(container.querySelectorAll('button')).find(b=>b.textContent==='Load more').click());
    const request=api.requestJson.mock.calls[1][0];
    expect(new URLSearchParams(request.split('?')[1]).get('search')).toBe('');
    expect(new URLSearchParams(request.split('?')[1]).get('page')).toBe('page-two');
    expect(container.textContent).toContain('First');expect(container.textContent).toContain('Second');
  } finally {act(()=>root.unmount());jest.clearAllMocks();}
});
