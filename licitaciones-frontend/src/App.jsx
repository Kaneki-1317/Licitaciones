import { Route, Routes } from "react-router-dom";
import Layout from "./components/Layout/Layout";
import ProtectedRoute from "./components/ProtectedRoute/ProtectedRoute";
import AdminRoute from "./components/AdminRoute/AdminRoute";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import CrearProceso from "./pages/CrearProceso";
import AnalizarDocumentos from "./pages/AnalizarDocumentos";
import Resultado from "./pages/Resultado";
import DetalleProceso from "./pages/DetalleProceso";
import Usuarios from "./pages/Usuarios";
import NotFound from "./pages/NotFound";

/**
 * /login es la UNICA ruta publica (ver seccion 10 del pedido): todo lo
 * demas exige sesion, forzada por <ProtectedRoute /> (redirige a /login si
 * no hay token). /usuarios ademas exige rol ADMIN, forzado por
 * <AdminRoute /> (ver seccion 9). El resto de la app y su Layout no
 * cambian.
 */
function App() {
  return (
    <Routes>
      <Route path="login" element={<Login />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="procesos/nuevo" element={<CrearProceso />} />
          <Route path="procesos/:id" element={<DetalleProceso />} />
          <Route path="procesos/:id/analizar" element={<AnalizarDocumentos />} />
          <Route path="procesos/:id/resultado" element={<Resultado />} />
          <Route element={<AdminRoute />}>
            <Route path="usuarios" element={<Usuarios />} />
          </Route>
          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>
    </Routes>
  );
}

export default App;
