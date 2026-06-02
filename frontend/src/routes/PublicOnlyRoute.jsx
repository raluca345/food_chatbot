import { getToken } from "../api/apiCore";
import { Navigate } from "react-router";

export default function PublicOnlyRoute({ children }) {
  const token = getToken();
  return token ? <Navigate to="/home" replace /> : children;
}
