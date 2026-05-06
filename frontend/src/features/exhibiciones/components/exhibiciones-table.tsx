"use client";

/* eslint-disable react-hooks/incompatible-library */

import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table";
import Link from "next/link";
import { useMemo } from "react";
import { Pencil, Search, SquareCheckBig } from "lucide-react";
import type { ExhibicionResponseDTO } from "../types";
import { formatDate } from "../utils";

type ExhibicionesTableProps = {
  exhibiciones: ExhibicionResponseDTO[];
  canEdit: boolean;
  onFinalizar: (id: number) => void;
  finalizandoId?: number;
};

export function ExhibicionesTable({ canEdit, exhibiciones, finalizandoId, onFinalizar }: ExhibicionesTableProps) {
  const columns = useMemo<ColumnDef<ExhibicionResponseDTO>[]>(
    () => [
      { accessorKey: "nombre", header: "Nombre", cell: ({ row }) => <span className="font-medium">{row.original.nombre}</span> },
      { accessorKey: "tipo", header: "Tipo" },
      { accessorKey: "estado", header: "Estado" },
      { accessorKey: "fechaInicio", header: "Inicio", cell: ({ row }) => formatDate(row.original.fechaInicio) },
      { accessorKey: "fechaFin", header: "Fin", cell: ({ row }) => formatDate(row.original.fechaFin) },
      {
        id: "acciones",
        header: "Acciones",
        cell: ({ row }) => (
          <div className="flex justify-end gap-2">
            <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/exhibiciones/${row.original.id}`}>
              <Search className="h-3.5 w-3.5" />
              Ver
            </Link>
            {canEdit ? (
              <>
                <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/exhibiciones/${row.original.id}/editar`}>
                  <Pencil className="h-3.5 w-3.5" />
                  Editar
                </Link>
                {row.original.estado !== "FINALIZADA" ? (
                  <button
                    className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted disabled:opacity-60"
                    disabled={finalizandoId === row.original.id}
                    onClick={() => onFinalizar(row.original.id)}
                    type="button"
                  >
                    <SquareCheckBig className="h-3.5 w-3.5" />
                    Finalizar
                  </button>
                ) : null}
              </>
            ) : null}
          </div>
        )
      }
    ],
    [canEdit, finalizandoId, onFinalizar]
  );

  const table = useReactTable({ data: exhibiciones, columns, getCoreRowModel: getCoreRowModel() });

  return (
    <div className="overflow-hidden rounded-lg border">
      <table className="w-full border-collapse text-sm">
        <thead className="bg-muted/60">
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id}>
              {headerGroup.headers.map((header) => (
                <th className="px-4 py-3 text-left font-medium" key={header.id}>
                  {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody>
          {table.getRowModel().rows.map((row) => (
            <tr className="border-t" key={row.id}>
              {row.getVisibleCells().map((cell) => (
                <td className="px-4 py-3 align-top" key={cell.id}>
                  {flexRender(cell.column.columnDef.cell, cell.getContext())}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
