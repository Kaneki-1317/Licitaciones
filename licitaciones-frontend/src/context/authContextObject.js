import { createContext } from "react";

/**
 * El objeto de contexto en si, separado de AuthContext.jsx (que solo debe
 * exportar el componente <AuthProvider />) y de hooks/useAuth.js (que lo
 * consume): asi ningun archivo mezcla un export que no sea un componente
 * junto a uno que si lo es, requisito de React Fast Refresh (ver ESLint
 * react-refresh/only-export-components, ya activo en este proyecto).
 */
export const AuthContext = createContext(null);
