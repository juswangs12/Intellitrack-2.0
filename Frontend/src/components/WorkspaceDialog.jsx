import {useEffect,useRef} from 'react';
export default function WorkspaceDialog({title,onClose,children}){
  const ref=useRef(null);
  useEffect(()=>{const dialog=ref.current;dialog.showModal();return()=>dialog.close();},[]);
  return <dialog ref={ref} aria-label={title} className="workspace-dialog" onCancel={e=>{e.preventDefault();onClose();}}>
    <div className="workspace-dialog-heading"><h2>{title}</h2><button type="button" className="btn btn-secondary" onClick={onClose} aria-label="Close dialog">Close</button></div>
    {children}
  </dialog>;
}
