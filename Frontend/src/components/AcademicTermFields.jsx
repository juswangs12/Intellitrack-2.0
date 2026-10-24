export const initialTerm = () => {
  const year = new Date().getFullYear();
  return `${year}-${year+1} / 1`;
};
export default function AcademicTermFields({value,onChange,disabled=false}) {
  const match=/^(\d{4})-\d{4} \/ ([12])$/.exec(value||'');
  const year=match?Number(match[1]):new Date().getFullYear();
  const semester=match?match[2]:'1';
  const current=new Date().getFullYear();
  const years=[...new Set([year,...Array.from({length:8},(_,i)=>current-3+i)])].sort();
  return <div className="term-fields">
    <label>School year<select value={year} disabled={disabled} onChange={e=>onChange(`${e.target.value}-${Number(e.target.value)+1} / ${semester}`)}>{years.map(y=><option key={y} value={y}>{y}-{y+1}</option>)}</select></label>
    <label>Semester<select value={semester} disabled={disabled} onChange={e=>onChange(`${year}-${year+1} / ${e.target.value}`)}><option value="1">First semester</option><option value="2">Second semester</option></select></label>
  </div>;
}
