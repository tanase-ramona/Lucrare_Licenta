import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

export function AdminRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, state } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (!state.user?.roles.includes("ADMIN")) return <Navigate to="/" replace />;
  return <>{children}</>;
}
