import api from './ApiService';
beforeEach(()=>{localStorage.clear();localStorage.setItem('token','old');global.fetch=jest.fn();});
afterEach(()=>api.setAuthContext(null));
test('returns the successful retry response',async()=>{
 const success={status:200,ok:true};fetch.mockResolvedValueOnce({status:401}).mockResolvedValueOnce(success);
 api.setAuthContext({token:'old',refreshAccessToken:async()=>{localStorage.setItem('token','new');return true;},logout:jest.fn()});
 expect(await api.apiCall('/tracking')).toBe(success);
 expect(fetch.mock.calls[1][1].headers.Authorization).toBe('Bearer new');
});
test('a second unauthorized response clears the rejected session',async()=>{
 const logout=jest.fn();fetch.mockResolvedValue({status:401});
 api.setAuthContext({token:'old',refreshAccessToken:async()=>{localStorage.setItem('token','new');return true;},logout});
 await api.apiCall('/tracking');expect(logout).toHaveBeenCalledTimes(1);
});
test('a failed old refresh does not log out a new session',async()=>{
 const logout=jest.fn();fetch.mockResolvedValue({status:401});
 api.setAuthContext({token:'old',refreshAccessToken:async()=>{localStorage.setItem('token','different-login');return false;},logout});
 await expect(api.apiCall('/tracking')).rejects.toThrow();expect(logout).not.toHaveBeenCalled();
});
