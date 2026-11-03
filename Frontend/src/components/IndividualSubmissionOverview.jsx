import { useState } from 'react';
import { groupSubmissionRows, Submission } from './GroupSubmissionOverview';

export default function IndividualSubmissionOverview({ rows, statusLabels, formatDate }) {
  return <div className="individual-overview">{groupSubmissionRows(rows).map(scope =>
    <SubjectStudents key={scope.key} scope={scope} statusLabels={statusLabels} formatDate={formatDate} />
  )}</div>;
}

function SubjectStudents({ scope, statusLabels, formatDate }) {
  const [page, setPage] = useState(0);
  const requirements = [...scope.requirements.values()].sort((a,b) => (a.dueAt || '').localeCompare(b.dueAt || '') || a.deliverable.localeCompare(b.deliverable));
  const students = [...scope.groups.values()].flatMap(group => [...group.members.values()])
    .sort((a,b) => a.studentName.localeCompare(b.studentName) || String(a.enrollmentId).localeCompare(String(b.enrollmentId)));
  const pageSize = 25;
  return <section>
    <h2>{scope.subject} · {scope.term || 'Legacy'}</h2>
    <p className="tracking-help">One row per student enrollment. Expand a submitted status for dates, document links and version history.</p>
    <div className="tracking-table individual-matrix"><table style={{minWidth:420 + requirements.length * 190}}>
      <colgroup><col style={{width:240}}/><col style={{width:180}}/>{requirements.map(r => <col key={r.deliverableId} style={{width:190}}/>)}</colgroup>
      <thead><tr><th>Student</th><th>Group</th>{requirements.map(r => <th key={r.deliverableId}>{r.deliverable}<small>Due {formatDate(r.dueAt)} PHT</small></th>)}</tr></thead>
      <tbody>{students.slice(page * pageSize, (page + 1) * pageSize).map(member => <tr key={member.enrollmentId}>
        <td><strong>{member.studentName}</strong><small>{member.studentNumber}</small><small>Section {member.section}{!member.linked ? ' · account not linked' : ''}</small></td>
        <td>{member.group || 'Unassigned'}</td>
        {requirements.map(r => <td key={r.deliverableId}><Submission row={member.requirements.get(r.deliverableId)} statusLabels={statusLabels} formatDate={formatDate}/></td>)}
      </tr>)}</tbody>
    </table></div>
    {students.length > pageSize && <nav className="tracking-actions" aria-label={`${scope.subject} student pages`}><button className="btn btn-secondary" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</button><span>Page {page + 1} of {Math.ceil(students.length / pageSize)}</span><button className="btn btn-secondary" disabled={(page + 1) * pageSize >= students.length} onClick={() => setPage(p => p + 1)}>Next</button></nav>}
  </section>;
}
