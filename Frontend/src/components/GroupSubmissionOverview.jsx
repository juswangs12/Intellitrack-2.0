import { useState } from 'react';
import api from '../services/ApiService';

const submitted = row => ['SUBMITTED_ON_TIME', 'SUBMITTED_LATE'].includes(row.status);

// Keep identities scoped by subject and term, including unassigned enrollments.
export function groupSubmissionRows(rows) {
  const scopes = new Map();
  for (const row of rows) {
    const scopeKey = JSON.stringify([row.subjectId, row.academicTerm]);
    if (!scopes.has(scopeKey)) scopes.set(scopeKey, { key: scopeKey, subject: row.subject, term: row.academicTerm, requirements: new Map(), groups: new Map() });
    const scope = scopes.get(scopeKey);
    scope.requirements.set(row.deliverableId, row);
    const groupKey = row.groupId ?? `unassigned:${row.section}`;
    if (!scope.groups.has(groupKey)) scope.groups.set(groupKey, { key: groupKey, name: row.group || 'Unassigned', adviser: row.adviser, members: new Map() });
    const group = scope.groups.get(groupKey);
    if (!group.members.has(row.enrollmentId)) group.members.set(row.enrollmentId, { ...row, requirements: new Map() });
    group.members.get(row.enrollmentId).requirements.set(row.deliverableId, row);
  }
  return [...scopes.values()];
}

export default function GroupSubmissionOverview({ rows, statusLabels, formatDate }) {
  const [attention, setAttention] = useState(false);
  const [search, setSearch] = useState('');
  const scopes = groupSubmissionRows(rows);
  const term = search.trim().toLowerCase();
  let count = 0;
  return <section className="group-overview">
    <div className="tracking-actions">
      <label>Find group or student <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Group, name or student ID" /></label>
      <label><input type="checkbox" checked={attention} onChange={e => setAttention(e.target.checked)} /> Needs attention: overdue missing submissions</label>
    </div>
    <p className="tracking-help">Each count is submitted students / assigned students within the selected filters. Open a group to compare students, or a count to inspect a requirement. Overview search and Needs attention only narrow this table; exports use the filters above.</p>
    {scopes.map(scope => {
      const requirements = [...scope.requirements.values()];
      const groups = [...scope.groups.values()].filter(group => {
        const members = [...group.members.values()];
        return (!term || [group.name, ...members.flatMap(m => [m.studentName, m.studentNumber])].join(' ').toLowerCase().includes(term)) &&
          (!attention || members.some(m => [...m.requirements.values()].some(r => r.status === 'OVERDUE')));
      });
      count += groups.length;
      if (!groups.length) return null;
      return <section key={scope.key}>
        <h2>{scope.subject} · {scope.term || 'Legacy'}</h2>
        <div className="tracking-table"><table>
          <thead><tr><th>Group / student details</th><th>Adviser</th><th>Students</th>{requirements.map(r => <th key={r.deliverableId}>{r.deliverable}<small>Due {formatDate(r.dueAt)} PHT</small></th>)}</tr></thead>
          <tbody>{groups.map(group => {
            const members = [...group.members.values()];
            return <tr key={group.key}>
              <td><details className="group-student-details"><summary>{group.name}</summary>
                <div className="tracking-table"><table><caption>Individual submissions — {group.name}</caption>
                  <thead><tr><th>Student</th>{requirements.map(r => <th key={r.deliverableId}>{r.deliverable}</th>)}</tr></thead>
                  <tbody>{members.map(member => <tr key={member.enrollmentId}><td><strong>{member.studentName}</strong><small>{member.studentNumber} · Section {member.section}{!member.linked ? ' · account not linked' : ''}</small></td>{requirements.map(r => <td key={r.deliverableId}><Submission row={member.requirements.get(r.deliverableId)} statusLabels={statusLabels} formatDate={formatDate} /></td>)}</tr>)}</tbody>
                </table></div>
              </details></td><td>{group.adviser || 'Unassigned'}</td><td>{members.length}</td>
              {requirements.map(requirement => {
                const assigned = members.map(m => m.requirements.get(requirement.deliverableId)).filter(Boolean);
                if (!assigned.length) return <td key={requirement.deliverableId}>Not assigned</td>;
                const overdue = assigned.filter(r => r.status === 'OVERDUE').length;
                return <td key={requirement.deliverableId}><details><summary>{assigned.filter(submitted).length}/{assigned.length} submitted</summary>
                  {assigned.map(r => <div className="group-requirement-member" key={r.enrollmentId}><strong>{r.studentName}</strong><Submission row={r} statusLabels={statusLabels} formatDate={formatDate} /></div>)}
                </details>{overdue > 0 && <small className="tracking-error">{overdue} overdue</small>}</td>;
              })}
            </tr>;
          })}</tbody>
        </table></div>
      </section>;
    })}
    {!count && <p className="tracking-empty">No groups match this overview search or attention filter.</p>}
  </section>;
}

function Submission({ row, statusLabels, formatDate }) {
  const [history, setHistory] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const loadHistory = async () => {
    setBusy(true); setError('');
    try { setHistory(await api.requestJson(`/tracking/${row.submissionId}/history`)); }
    catch (e) { setError(e.message); }
    finally { setBusy(false); }
  };
  if (!row) return 'Not assigned';
  return <details className="group-submission-detail"><summary><span className={`tracking-status ${row.status}`}>{statusLabels[row.status] || row.status}</span></summary>
    <small>First submitted: {formatDate(row.firstSubmittedAt)}</small>
    <small>Latest replacement: {formatDate(row.latestSubmittedAt)}</small>
    {row.latestStatus === 'SUBMITTED_LATE' && row.status === 'SUBMITTED_ON_TIME' && <small>Late replacement; first submission remains on time.</small>}
    {row.googleDocUrl && <a href={row.googleDocUrl} target="_blank" rel="noreferrer">Open document</a>}
    {row.submissionId && <button className="btn btn-secondary" onClick={loadHistory} disabled={busy}>{busy ? 'Loading history…' : 'View history'}</button>}
    {error && <p role="alert">{error}</p>}
    {history && (history.length ? <ol>{history.map(h => <li key={h.version}>Version {h.version}<small>{formatDate(h.submittedAt)} · {statusLabels[h.status] || h.status}</small>{h.googleDocUrl && <a href={h.googleDocUrl} target="_blank" rel="noreferrer">Open saved document</a>}</li>)}</ol> : <p>No saved versions found.</p>)}
  </details>;
}
