"use client";

/* eslint-disable react-hooks/incompatible-library */

import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef
} from "@tanstack/react-table";
import Link from "next/link";
import { useMemo } from "react";
import { Pencil, Search } from "lucide-react";
import type { InventarioResponseDTO } from "../types";
import { formatDate } from "../utils";

type InventarioTableProps = {
  inventarios: InventarioResponseDTO[];
  canEdit: boolean;
};

export function InventarioTable({ canEdit, inventarios }: InventarioTableProps) {
  const columns = useMemo<ColumnDef<InventarioResponseDTO>[]>(
    () => [
      {
        accessorKey: "objetoNombre",
        header: "Objeto",
        cell: ({ row }) => <span className="font-medium">{row.original.objetoNombre}</span>
      },
      {
        accessorKey: "ubicacionNombre",
        header: "Ubicacion"
      },
      {
        accessorKey: "estado",
        header: "Estado"
      },
      {
        accessorKey: "estadoConservacion",
        header: "Conservacion"
      },
      {
        accessorKey: "fechaIngreso",
        header: "Ingreso",
        cell: ({ row }) => formatDate(row.original.fechaIngreso)
      },
      {
        accessorKey: "fechaSalida",
        header: "Salida",
        cell: ({ row }) => formatDate(row.original.fechaSalida)
      },
      {
        id: "acciones",
        header: "Acciones",
        cell: ({ row }) => (
          <div className="flex justify-end gap-2">
            <Link
              className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted"
              href={`/inventario/${row.original.id}`}
            >
              <Search className="h-3.5 w-3.5" />
              Ver
            </Link>
            {canEdit ? (
              <Link
                className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted"
                href={`/inventario/${row.original.id}/editar`}
              >
                <Pencil className="h-3.5 w-3.5" />
                Editar
              </Link>
            ) : null}
          </div>
        )
      }
    ],
    [canEdit]
  );

  const table = useReactTable({
    data: inventarios,
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
