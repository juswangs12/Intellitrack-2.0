import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

import StudentDashboard from './pages/StudentDashboard';
import StudentGroups from './pages/student/StudentGroups';
import StudentOverview from './pages/student/StudentOverview';
import StudentProfile from './pages/student/StudentProfile';

import AdviserDashboard from './pages/AdviserDashboard';
import AdviserOverview from './pages/adviser/AdviserOverview';
import AdviserProfile from './pages/adviser/AdviserProfile';

import CoordinatorDashboard from './pages/CoordinatorDashboard';
import CoordinatorOverview from './pages/coordinator/CoordinatorOverview';
import CoordinatorProfile from './pages/coordinator/CoordinatorProfile';
import CoordinatorGroupManagement from './pages/coordinator/CoordinatorGroupManagement';
import CoordinatorClasslistImport from './pages/coordinator/CoordinatorClasslistImport';
import CoordinatorClasslistView from './pages/coordinator/CoordinatorClasslistView';

import AdminDashboard from './pages/AdminDashboard';
import AdminHome from './pages/admin/AdminHome';
import UserManagement from './pages/admin/UserManagement';
import SystemConfig from './pages/admin/SystemConfig';

import './styles/App.css';
import './styles/Workspace.css';
import TrackingDashboard from './pages/TrackingDashboard';
import SubmissionLink from './pages/SubmissionLink';
import GoogleConnect from './pages/GoogleConnect';
import AcademicSetup from './pages/AcademicSetup';

const UnauthorizedPage = () => (
  <div style={{ padding: '40px', textAlign: 'center' }}>
    <h1>Access Denied</h1>
    <p>You do not have permission to access this page.</p>
    <a href="/">Return to Login</a>
  </div>
);

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/login" element={<Login />} />
          <Route path="/student/login" element={<Login />} />
          <Route path="/staff/login" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/unauthorized" element={<UnauthorizedPage />} />
          <Route path="/google-connect" element={<GoogleConnect />} />

          {/* Student routes */}
          <Route
            path="/student"
            element={
              <PrivateRoute requiredRole="student">
                <StudentDashboard />
              </PrivateRoute>
            }
          >
            <Route index element={<Navigate to="home" replace />} />
            <Route path="home" element={<StudentOverview />} />
            <Route path="submissions" element={<TrackingDashboard />} />
            <Route path="calendar" element={<StudentOverview calendar />} />
            <Route path="groups" element={<StudentGroups />} />
            <Route path="submissions/:deliverableId" element={<SubmissionLink />} />
            <Route path="profile" element={<StudentProfile />} />
          </Route>

          {/* Adviser routes */}
          <Route
            path="/adviser"
            element={
              <PrivateRoute requiredRole="adviser">
                <AdviserDashboard />
              </PrivateRoute>
            }
          >
            <Route index element={<Navigate to="home" replace />} />
            <Route path="home" element={<AdviserOverview />} />
            <Route path="groups" element={<TrackingDashboard />} />
            <Route path="groups/:groupId" element={<TrackingDashboard />} />
            <Route path="review" element={<TrackingDashboard />} />
            <Route path="profile" element={<AdviserProfile />} />
          </Route>

          {/* Coordinator routes */}
          <Route
            path="/coordinator"
            element={
              <PrivateRoute requiredRole="coordinator">
                <CoordinatorDashboard />
              </PrivateRoute>
            }
          >
            <Route index element={<Navigate to="home" replace />} />
            <Route path="home" element={<CoordinatorOverview />} />
            <Route path="requirements" element={<AcademicSetup requirementsOnly />} />
            <Route path="classlist" element={<CoordinatorClasslistImport />} />
            <Route path="classlist-view" element={<CoordinatorClasslistView />} />
            <Route path="document-review" element={<TrackingDashboard />} />
            <Route path="submission-tracking" element={<TrackingDashboard />} />
            <Route path="groups" element={<CoordinatorGroupManagement />} />
            <Route path="milestones" element={<AcademicSetup />} />
            <Route path="setup" element={<AcademicSetup />} />
            <Route path="deliverables" element={<AcademicSetup />} />
            <Route path="deadlines" element={<AcademicSetup />} />
            <Route path="analytics" element={<TrackingDashboard />} />
            <Route path="calendar" element={<CoordinatorOverview calendar />} />
            <Route path="profile" element={<CoordinatorProfile />} />
          </Route>

          {/* Admin routes */}
          <Route
            path="/admin"
            element={
              <PrivateRoute requiredRole="administrator">
                <AdminDashboard />
              </PrivateRoute>
            }
          >
            <Route index element={<Navigate to="home" replace />} />
            <Route path="home" element={<AdminHome />} />
            <Route path="users" element={<UserManagement />} />
            <Route path="profile" element={<CoordinatorProfile />} />
            <Route path="system" element={<SystemConfig />} />
            <Route path="deadlines" element={<AcademicSetup />} />
            <Route path="analytics" element={<TrackingDashboard />} />
          </Route>
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;
