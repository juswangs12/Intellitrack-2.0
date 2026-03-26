import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import StudentDashboard from './pages/StudentDashboard';
import './styles/App.css';

// Placeholder components - will be implemented as we build features
const AdviserDashboard = () => <div>Adviser Dashboard</div>;
const CoordinatorDashboard = () => <div>Coordinator Dashboard</div>;
const AdminDashboard = () => <div>Admin Dashboard</div>;

function App() {
  return (
    <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <div className="app">
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/student" element={<StudentDashboard />} />
          <Route path="/adviser" element={<AdviserDashboard />} />
          <Route path="/coordinator" element={<CoordinatorDashboard />} />
          <Route path="/admin" element={<AdminDashboard />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;