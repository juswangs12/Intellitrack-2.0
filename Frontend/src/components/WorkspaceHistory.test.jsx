import React,{act} from 'react';
import {createRoot} from 'react-dom/client';
import {MemoryRouter} from 'react-router-dom';
import WorkspaceHistory from './WorkspaceHistory';
import CalendarConnection from './CalendarConnection';
import api from '../services/ApiService';
jest.mock('../services/ApiService',()=>({requestJson:jest.fn()}));
let container,root;
beforeEach(()=>{global.IS_REACT_ACT_ENVIRONMENT=true;container=document.createElement('div');root=createRoot(container);api.requestJson.mockReset();});
afterEach(()=>act(()=>root.unmount()));
const render=async component=>act(async()=>root.render(<MemoryRouter future={{v7_startTransition:true,v7_relativeSplatPath:true}}>{component}</MemoryRouter>));
test('saved report link is restored on mount without exporting again',async()=>{
 api.requestJson.mockResolvedValue([{id:1,createdAt:'2026-11-03T12:00:00',scope:{subject:'IT332'},result:{url:'https://docs.google.com/spreadsheets/d/report'}}]);
 await render(<WorkspaceHistory kind="report"/>);
 expect(api.requestJson).toHaveBeenCalledWith('/google/reports/history');
 expect(container.querySelector('a').href).toBe('https://docs.google.com/spreadsheets/d/report');
 expect(container.textContent).toContain('Open latest export');
});
test('summary history restores prior text with its scope and date',async()=>{
 api.requestJson.mockResolvedValue([{id:2,createdAt:'2026-11-03T12:00:00',scope:{subject:'IT332'},result:{source:'Computed summary',text:'Two assignments overdue.',notice:'Tracking only'}}]);
 await render(<WorkspaceHistory/>);expect(container.textContent).toContain('Two assignments overdue.');expect(container.textContent).toContain('IT332');
});
test('automatic calendar hides manual sync without sending any sync request',async()=>{
 api.requestJson.mockResolvedValue({calendar:true,calendarAutomation:true});
 await render(<CalendarConnection endpoint="/google/calendar/coordinator/sync"/>);
 expect(container.textContent).toContain('Deadlines update automatically');expect(container.textContent).not.toContain('Update calendar');
 expect(api.requestJson).toHaveBeenCalledTimes(1);
});
