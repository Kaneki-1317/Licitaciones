import { Link } from "react-router-dom";
import Button from "../components/Button/Button";

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center gap-4 py-24 text-center">
      <p className="text-6xl font-bold text-slate-200">404</p>
      <p className="text-slate-600">La página que buscas no existe.</p>
      <Link to="/">
        <Button variant="primary">Volver al listado de procesos</Button>
      </Link>
    </div>
  );
}
