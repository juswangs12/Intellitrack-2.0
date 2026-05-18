import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

import StudentDashboard from './pages/StudentDashboard';
import StudentHome from './pages/student/StudentHome';
import StudentProfile from './pages/student/StudentProfile';
import DocumentSubmission from './pages/student/DocumentSubmissions';

import AdviserDashboard from './pages/AdviserDashboard';
import AdviserHome from './pages/adviser/AdviserHome';
import AdviserGroups from './pages/adviser/AdviserGroups';
import AdviserGroupDetails from './pages/adviser/AdviserGroupDetails';
import AdviserProfile from './pages/adviser/AdviserProfile';
import AdviserDocumentReview from './pages/adviser/AdviserDocumentReview';

import CoordinatorDashboard from './pages/CoordinatorDashboard';
import CoordinatorHome from './pages/coordinator/CoordinatorHome';
import DocumentReview from './pages/coordinator/DocumentReview';
import CoordinatorCalendar from './pages/coordinator/CoordinatorCalendar';
import CoordinatorProfile from './pages/coordinator/CoordinatorProfile';
import CoordinatorGroupManagement from './pages/coordinator/CoordinatorGroupManagement';
import MilestoneManagement from './pages/coordinator/MilestoneManagement';
import CoordinatorAnalytics from './pages/coordinator/CoordinatorAnalytics';
import CoordinatorClasslistImport from './pages/coordinator/CoordinatorClasslistImport';
import CoordinatorClasslistView from './pages/coordinator/CoordinatorClasslistView';

import AdminDashboard from './pages/AdminDashboard';
import AdminHome from './pages/admin/AdminHome';
import UserManagement from './pages/admin/UserManagement';
import SystemConfig from './pages/admin/SystemConfig';
import Deadlines from './pages/admin/Deadlines';
import Analytics from './pages/admin/Analytics';

import './styles/App.css';

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
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/unauthorized" element={<UnauthorizedPage />} />

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
            <Route path="home" element={<StudentHome />} />
            <Route path="submissions/:deliverableId" element={<DocumentSubmission />} />
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
            <Route path="home" element={<AdviserHome />} />
            <Route path="groups" element={<AdviserGroups />} />
            <Route path="groups/:groupId" element={<AdviserGroupDetails />} />
            <Route path="review" element={<AdviserDocumentReview />} />
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
            <Route path="home" element={<CoordinatorHome />} />
            <Route path="classlist" element={<CoordinatorClasslistImport />} />
            <Route path="classlist-view" element={<CoordinatorClasslistView />} />
            <Route path="document-review" element={<DocumentReview />} />
            <Route path="groups" element={<CoordinatorGroupManagement />} />
            <Route path="milestones" element={<MilestoneManagement />} />
            <Route path="deliverables" element={<MilestoneManagement />} />
            <Route path="deadlines" element={<MilestoneManagement />} />
            <Route path="analytics" element={<CoordinatorAnalytics />} />
            <Route path="calendar" element={<CoordinatorCalendar />} />
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
            <Route path="system" element={<SystemConfig />} />
            <Route path="deadlines" element={<Deadlines />} />
            <Route path="analytics" element={<Analytics />} />
          </Route>
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;
