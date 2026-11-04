import { Fragment, useState } from 'react';
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
  const [selection, setSelection] = useState(null);
  const scopes = groupSubmissionRows(rows);
  const openGroup = (scope, group) => setSelection(current => current?.scope === scope.key && current?.group === group.key ? null : {scope: scope.key, group: group.key});
  let count = 0;
  return <section className="group-overview">
    <p className="tracking-help">Counts show submitted / assigned students. Expand a group to compare individual requirements.</p>
    {scopes.map(scope => {
      const requirements = [...scope.requirements.values()].sort((a,b) => (a.dueAt || "").localeCompare(b.dueAt || "") || a.deliverable.localeCompare(b.deliverable));
      const groups = [...scope.groups.values()];
      count += groups.length;
      if (!groups.length) return null;
      return <section key={scope.key}>
        <h2>{scope.subject} · {scope.term || 'Legacy'}</h2>
        <div className="tracking-table group-overview-table"><table style={{minWidth: 470 + requirements.length * 190}}>
          <colgroup><col style={{width:200}}/><col style={{width:170}}/><col style={{width:100}}/>{requirements.map(r => <col key={r.deliverableId} style={{width:190}}/>)}</colgroup>
          <thead><tr><th>Group code</th><th>Adviser</th><th>Students</th>{requirements.map(r => <th key={r.deliverableId}>{r.deliverable}<small>Due {formatDate(r.dueAt)} PHT</small></th>)}</tr></thead>
          <tbody>{groups.map(group => {
            const members = [...group.members.values()];
            const expanded = selection?.scope === scope.key && selection?.group === group.key;
            return <Fragment key={group.key}><tr>
              <td><button className="group-overview-link group-open" onClick={() => openGroup(scope, group)} aria-expanded={expanded} aria-label={group.name}><span aria-hidden="true" className="group-chevron">{expanded ? '\u25be' : '\u25b8'}</span>{group.name}</button></td><td>{group.adviser || 'Unassigned'}</td><td>{members.length}</td>
              {requirements.map(requirement => {
                const assigned = members.map(m => m.requirements.get(requirement.deliverableId)).filter(Boolean);
                if (!assigned.length) return <td key={requirement.deliverableId}>Not assigned</td>;
                const overdue = assigned.filter(r => r.status === 'OVERDUE').length;
                return <td key={requirement.deliverableId}><span>{assigned.filter(submitted).length}/{assigned.length} submitted</span>{overdue > 0 && <small className="group-overdue">{overdue} overdue</small>}</td>;
              })}
            </tr>{expanded && <tr className="group-expanded-row"><td colSpan={requirements.length + 3}><section className="group-detail-panel" aria-label="Selected group details"><h3>{group.name} - student submissions</h3>
      <div className="tracking-table group-detail-table"><table style={{minWidth:240 + requirements.length * 190}}>
        <colgroup><col style={{width:240}}/>{requirements.map(r => <col key={r.deliverableId} style={{width:190}}/>)}</colgroup>
        <thead><tr><th>Student</th>{requirements.map(r => <th key={r.deliverableId}>{r.deliverable}<small>Due {formatDate(r.dueAt)} PHT</small></th>)}</tr></thead>
        <tbody>{[...group.members.values()].map(member => <tr key={member.enrollmentId}><td><strong>{member.studentName}</strong><small>{member.studentNumber} | Section {member.section}{!member.linked ? ' | account not linked' : ''}</small></td>{requirements.map(r => <td key={r.deliverableId}><Submission row={member.requirements.get(r.deliverableId)} statusLabels={statusLabels} formatDate={formatDate} /></td>)}</tr>)}</tbody>
      </table></div>
</section></td></tr>}</Fragment>;
          })}</tbody>
        </table></div>
      </section>;
    })}
    {!count && <p className="tracking-empty">No groups match the selected filters.</p>}
  </section>;
}

export function Submission({ row, statusLabels, formatDate }) {
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
  if (!row.submissionId && !row.firstSubmittedAt) return <span className={`tracking-status ${row.status}`}>{statusLabels[row.status] || row.status}</span>;
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
