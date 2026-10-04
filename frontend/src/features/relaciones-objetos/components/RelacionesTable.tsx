"use client";

import Link from "next/link";
import { Search } from "lucide-react";
import { RowActionLink, RowActions } from "@/components/common/row-actions";
import type { RelacionElementoResponseDTO } from "../types";

export function RelacionesTable({ relaciones }: { relaciones: RelacionElementoResponseDTO[] }) {
  return (
    <div className="overflow-hidden rounded-lg border">
      <table className="w-full border-collapse text-sm">
        <thead className="bg-primary text-primary-foreground">
          <tr>
            <th className="px-4 py-3 text-left font-semibold text-white">Tipo</th>
            <th className="px-4 py-3 text-left font-semibold text-white">Elemento relacionado</th>
            <th className="px-4 py-3 text-left font-semibold text-white">Relación</th>
            <th className="px-4 py-3 text-left font-semibold text-white">Descripción</th>
            <th className="px-4 py-3 text-left font-semibold text-white">Acciones</th>
          </tr>
        </thead>
        <tbody>
          {relaciones.map((relacion) => {
            const href = relacion.tipoElemento === "PERSONA" ? `/veteranos/${relacion.elementoId}` : `/objetos/${relacion.elementoId}`;
            return (
              <tr className="border-t" key={relacion.idRelacion}>
                <td className="px-4 py-3 align-top"><span className="rounded-full border bg-muted px-2.5 py-1 text-xs font-semibold">{relacion.tipoElemento === "PERSONA" ? "Persona" : "Objeto"}</span></td>
                <td className="px-4 py-3 align-top">
                  <Link className="font-medium text-primary underline-offset-4 hover:underline" href={href}>
                    {[relacion.numeroInventario, relacion.denominacion].filter(Boolean).join(" - ")}
                  </Link>
                </td>
                <td className="px-4 py-3 align-top"><p className="font-medium">{relacion.tipoRelacion}</p><p className="text-xs text-muted-foreground">{relacion.direccion}</p></td>
                <td className="px-4 py-3 align-top text-muted-foreground">{relacion.descripcion || "Sin descripción"}</td>
                <td className="px-4 py-3 align-top"><RowActions className="justify-start"><RowActionLink href={href} icon={Search} label="Ver detalle" /></RowActions></td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
