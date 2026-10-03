import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Header } from './components/Header';
import { Footer } from './components/Footer';

import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { UnauthorizedPage } from './pages/UnauthorizedPage';
import { ClaimSubmissionPage } from './pages/ClaimSubmissionPage';
import { MyClaimsPage } from './pages/MyClaimsPage';
import { ReviewerQueuePage } from './pages/ReviewerQueuePage';
import { ClaimDetailPage } from './pages/ClaimDetailPage';
import { PolicyRulesPage } from './pages/PolicyRulesPage';
import { NotFoundPage } from './pages/NotFoundPage';

export function App() {
  return (
    <AuthProvider>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <div className="app-container">
          <Header />
          <main className="main-content">
            <Routes>
              {/* Public Auth Routes */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/unauthorized" element={<UnauthorizedPage />} />

              {/* Protected Routes */}
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <DashboardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/claims/new"
                element={
                  <ProtectedRoute>
                    <ClaimSubmissionPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/claims/my"
                element={
                  <ProtectedRoute>
                    <MyClaimsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/claims/:id"
                element={
                  <ProtectedRoute>
                    <ClaimDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/policies"
                element={
                  <ProtectedRoute>
                    <PolicyRulesPage />
                  </ProtectedRoute>
                }
              />

              {/* Reviewer / Admin Queue Route */}
              <Route
                path="/claims/reviewer/queue"
                element={
                  <ProtectedRoute allowedRoles={['REVIEWER', 'ADMIN']}>
                    <ReviewerQueuePage />
                  </ProtectedRoute>
                }
              />

              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </main>
          <Footer />
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
