"use client";

/* eslint-disable react-hooks/incompatible-library */

import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef
} from "@tanstack/react-table";
import { useMemo } from "react";
import type { MovimientoInventarioResponseDTO } from "../types";
import { formatDateTime } from "../utils";

type MovimientosInventarioTableProps = {
  movimientos: MovimientoInventarioResponseDTO[];
};

export function MovimientosInventarioTable({ movimientos }: MovimientosInventarioTableProps) {
  const columns = useMemo<ColumnDef<MovimientoInventarioResponseDTO>[]>(
    () => [
      {
        accessorKey: "objetoNombre",
        header: "Objeto",
        cell: ({ row }) => <span className="font-medium">{row.original.objetoNombre}</span>
      },
      {
        accessorKey: "tipo",
        header: "Tipo"
      },
      {
        id: "origen",
        header: "Origen",
        cell: ({ row }) => row.original.ubicacionOrigenNombre || "No aplica"
      },
      {
        id: "destino",
        header: "Destino",
        cell: ({ row }) => row.original.ubicacionDestinoNombre || "No aplica"
      },
      {
        accessorKey: "fecha",
        header: "Fecha",
        cell: ({ row }) => formatDateTime(row.original.fecha)
      },
      {
        accessorKey: "observaciones",
        header: "Observaciones",
        cell: ({ row }) => row.original.observaciones || "Sin observaciones"
      }
    ],
    []
  );

  const table = useReactTable({
    data: movimientos,
    columns,
    getCoreRowModel: getCoreRowModel()
  });

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
