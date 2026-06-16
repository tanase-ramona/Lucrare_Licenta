import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import AuthPage from "./pages/AuthPage";
import InterviewSetupPage from "./pages/InterviewSetupPage";
import InterviewPage from "./pages/InterviewPage";
import InterviewResultPage from "./pages/InterviewResultPage";
import InterviewReviewPage from "./pages/InterviewReviewPage";
import RecommendationsPage from "./pages/RecommendationsPage";
import AdminDashboard from "./pages/AdminDashboard";
import AdminQuestionsPage from "./pages/AdminQuestionsPage";
import AdminUsersPage from "./pages/AdminUsersPage";
import AdminQuestionsManagePage from "./pages/AdminQuestionsManagePage";
import ProfilePage from "./pages/ProfilePage";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { AdminRoute } from "./auth/AdminRoute";
import { Routes, Route, Navigate } from "react-router-dom";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/auth"  element={<AuthPage />} />

      <Route path="/" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
      <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
      <Route path="/recommendations" element={<ProtectedRoute><RecommendationsPage /></ProtectedRoute>} />

      <Route path="/interview/setup" element={<ProtectedRoute><InterviewSetupPage /></ProtectedRoute>} />
      <Route path="/interview/:id" element={<ProtectedRoute><InterviewPage /></ProtectedRoute>} />
      <Route path="/interview/:id/result" element={<ProtectedRoute><InterviewResultPage /></ProtectedRoute>} />
      <Route path="/interview/:id/review" element={<ProtectedRoute><InterviewReviewPage /></ProtectedRoute>} />

      <Route path="/admin"                    element={<AdminRoute><AdminDashboard /></AdminRoute>} />
      <Route path="/admin/users"              element={<AdminRoute><AdminUsersPage /></AdminRoute>} />
      <Route path="/admin/questions"          element={<AdminRoute><AdminQuestionsPage /></AdminRoute>} />
      <Route path="/admin/questions/manage"   element={<AdminRoute><AdminQuestionsManagePage /></AdminRoute>} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
