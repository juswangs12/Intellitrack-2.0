import {Link} from 'react-router-dom';
export default function CoordinatorSetupSteps(){
  return <details className="setup-guide" aria-label="Coordinator setup guide">
    <summary>Getting started with a new semester</summary><p>Import a classlist, then assign advisers and deadlines.</p>
    <ol>
      <li><Link to="/coordinator/classlist">1. Import classlist</Link><span>Select the school year and semester. The import creates subjects, sections, enrollments and named groups.</span></li>
      <li><Link to="/coordinator/groups">2. Assign advisers</Link><span>Review imported groups and assign their advisers.</span></li>
      <li><Link to="/coordinator/setup?tab=requirements">3. Set deadlines</Link><span>Add the requirements students need to submit.</span></li>
    </ol>
  </details>;
}
