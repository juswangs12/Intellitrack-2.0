import {Link} from 'react-router-dom';
import '../styles/DashboardProgress.css';
export default function DashboardProgress({rows=[],title,description,actions=[]}){
 const submitted=rows.filter(r=>r.submissionId||r.firstSubmittedAt).length;
 const overdue=rows.filter(r=>r.status==='OVERDUE').length;
 const percent=rows.length?Math.round(submitted/rows.length*100):0;
 return <section className="dashboard-progress" aria-label="Submission progress">
  <div><span className="dashboard-kicker">AT A GLANCE</span><h2>{title}</h2><p>{description}</p>
   <nav aria-label="Dashboard actions">{actions.map(a=><Link key={a.to} to={a.to}>{a.label}<span aria-hidden="true"> &rarr;</span></Link>)}</nav>
  </div>
  <div className="dashboard-progress-summary"><strong>{submitted} <span>/ {rows.length}</span></strong><p>required submissions received</p><progress value={submitted} max={rows.length||1} aria-label="Required submissions received"/>
  <div><span>{percent}% submitted</span><span>{overdue} overdue missing</span></div><small>Each student counts separately for each requirement. Replacements do not increase this total.</small></div>
 </section>;
}
