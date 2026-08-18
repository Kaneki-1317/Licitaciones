import { useContext } from "react";
import { AuthContext } from "../context/authContextObject";

/**
 * Hook para consumir la sesion (ver context/AuthContext.jsx).
 * @returns {{user: {username: string, role: string} | null, token: string | null, isAuthenticated: boolean, login: (username: string, password: string) => Promise<object>, logout: () => void}}
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth debe usarse dentro de <AuthProvider>");
  }
  return context;
}
