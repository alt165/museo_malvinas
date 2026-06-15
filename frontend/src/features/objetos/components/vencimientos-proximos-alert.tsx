"use client";

import { X } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useExhibicionesProximasInicioQuery } from "@/features/exhibiciones/queries";
import { useConfigAlertasComodatosPrestamosQuery, useObjetosVencimientosProximosQuery } from "@/features/objetos/queries";
import type { CaracterRecepcionObjeto } from "@/features/objetos/types";
import { hasRole, useAuth } from "@/lib/auth";

const storageKey = "museo.vencimientos-proximos.cerrada";

const caracterLabels: Partial<Record<CaracterRecepcionObjeto, string>> = {
  PRESTAMO: "Préstamo",
  COMODATO: "Comodato"
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat("es-AR", { dateStyle: "medium" }).format(new Date(`${value}T00:00:00`));
}

function diasRestantesLabel(dias: number) {
  if (dias === 0) {
    return "vence hoy";
  }

  if (dias === 1) {
    return "vence en 1 día";
  }

  return `vence en ${dias} días`;
}


function diasInicioLabel(dias: number) {
  if (dias === 0) {
    return "inicia hoy";
  }

  if (dias === 1) {
    return "inicia en 1 día";
  }

  return `inicia en ${dias} días`;
}

function initialClosed() {
  if (typeof window === "undefined") {
    return false;
  }

  return window.sessionStorage.getItem(storageKey) === "1";
}

export function VencimientosProximosAlert() {
  const { authenticated, roles } = useAuth();
  const esAdmin = authenticated && hasRole(roles, "ADMIN");
  const [cerrada, setCerrada] = useState(initialClosed);
  const configQuery = useConfigAlertasComodatosPrestamosQuery(esAdmin && !cerrada);
  const diasVencimiento = configQuery.data?.diasAnticipacion ?? 14;
  const vencimientosQuery = useObjetosVencimientosProximosQuery(diasVencimiento, esAdmin && !cerrada && configQuery.isSuccess);
  const exhibicionesQuery = useExhibicionesProximasInicioQuery(esAdmin && !cerrada);
  const vencimientos = vencimientosQuery.data ?? [];
  const exhibiciones = exhibicionesQuery.data ?? [];

  if (!esAdmin || cerrada) {
    return null;
  }

  if (configQuery.isError || vencimientosQuery.isError || exhibicionesQuery.isError) {
    return (
      <div className="mb-6 rounded-md border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-950">
        No se pudieron consultar los vencimientos próximos.
      </div>
    );
  }

  if ((!vencimientosQuery.isSuccess && !exhibicionesQuery.isSuccess) || (vencimientos.length === 0 && exhibiciones.length === 0)) {
    return null;
  }

  function cerrar() {
    window.sessionStorage.setItem(storageKey, "1");
    setCerrada(true);
  }

  return (
    <section className="mb-6 rounded-md border border-red-300 bg-red-50 p-4 text-sm text-red-950">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="font-semibold">Hay avisos próximos de comodatos, préstamos o exhibiciones.</h2>
          <p className="mt-1 text-red-900">Se muestran vencimientos e inicios previstos dentro del período configurado.</p>
        </div>
        <button
          aria-label="Cerrar alerta de vencimientos próximos"
          className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-md border border-red-200 bg-white/70 hover:bg-white"
          onClick={cerrar}
          type="button"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
      {exhibiciones.length > 0 ? (
        <div className="mt-4 rounded-md border border-red-200 bg-white p-3">
          <h3 className="font-medium text-red-950">Exhibiciones próximas a iniciar</h3>
          <div className="mt-2 space-y-2 text-sm">
            {exhibiciones.map((exhibicion) => (
              <Link className="block rounded-md border border-red-100 px-3 py-2 hover:bg-red-50" href={`/exhibiciones/${exhibicion.id}`} key={exhibicion.id}>
                Exhibición {exhibicion.permanente ? "permanente " : ""}“{exhibicion.nombre}” {diasInicioLabel(exhibicion.diasRestantes)} ({formatDate(exhibicion.fechaInicio)})
              </Link>
            ))}
          </div>
        </div>
      ) : null}
      {vencimientos.length > 0 ? (
      <div className="mt-4 overflow-x-auto rounded-md border border-red-200 bg-white">
        <table className="w-full min-w-[760px] border-collapse text-left text-sm">
          <thead className="bg-red-100/70 text-red-950">
            <tr>
              <th className="px-3 py-2 font-medium">Inventario</th>
              <th className="px-3 py-2 font-medium">Objeto</th>
              <th className="px-3 py-2 font-medium">Depositante</th>
              <th className="px-3 py-2 font-medium">Carácter</th>
              <th className="px-3 py-2 font-medium">Vencimiento</th>
              <th className="px-3 py-2 font-medium">Días</th>
            </tr>
          </thead>
          <tbody>
            {vencimientos.map((objeto) => (
              <tr className="border-t border-red-100" key={objeto.id}>
                <td className="px-3 py-2 font-medium">{objeto.numeroInventario}</td>
                <td className="px-3 py-2">
                  <Link className="font-medium underline-offset-2 hover:underline" href={`/objetos/${objeto.id}`}>
                    {objeto.denominacionObjeto}
                  </Link>
                </td>
                <td className="px-3 py-2">{objeto.depositanteNombre}</td>
                <td className="px-3 py-2">{caracterLabels[objeto.caracterRecepcion] ?? objeto.caracterRecepcion}</td>
                <td className="px-3 py-2">{formatDate(objeto.fechaVencimiento)}</td>
                <td className="px-3 py-2">{diasRestantesLabel(objeto.diasRestantes)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      ) : null}
    </section>
  );
}
