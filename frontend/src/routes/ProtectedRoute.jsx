import { getToken } from "../api/apiCore";
import { Navigate } from "react-router";

export default function ProtectedRoute({ children }) {
  const token = getToken();
  return token ? children : <Navigate to="/login" replace />;
}
