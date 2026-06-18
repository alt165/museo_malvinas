"use client";

/* eslint-disable react-hooks/incompatible-library */

import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table";
import { Pencil, Search, Trash2 } from "lucide-react";
import Link from "next/link";
import { useMemo } from "react";
import type { RelacionObjetoResponseDTO } from "../types";
import { resumenDescripcion } from "../utils";

type RelacionesObjetosTableProps = {
  relaciones: RelacionObjetoResponseDTO[];
  canEdit: boolean;
  isDeleting?: boolean;
  onDelete: (id: number) => void;
};

export function RelacionesObjetosTable({
  canEdit,
  isDeleting = false,
  onDelete,
  relaciones
}: RelacionesObjetosTableProps) {
  const columns = useMemo<ColumnDef<RelacionObjetoResponseDTO>[]>(
    () => [
      {
        accessorKey: "objetoOrigenNombre",
        header: "Objeto origen",
        cell: ({ row }) => <span className="font-medium">{row.original.objetoOrigenNombre}</span>
      },
      {
        accessorKey: "objetoDestinoNombre",
        header: "Objeto destino",
        cell: ({ row }) => <span className="font-medium">{row.original.objetoDestinoNombre}</span>
      },
      {
        accessorKey: "tipoRelacion",
        header: "Tipo"
      },
      {
        accessorKey: "descripcion",
        header: "Descripcion",
        cell: ({ row }) => <span className="text-muted-foreground">{resumenDescripcion(row.original.descripcion)}</span>
      },
      {
        id: "acciones",
        header: "Acciones",
        cell: ({ row }) => (
          <div className="flex justify-end gap-2">
            <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/relaciones-objetos/${row.original.id}`}>
              <Search className="h-3.5 w-3.5" />
              Ver
            </Link>
            {canEdit ? (
              <>
                <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/relaciones-objetos/${row.original.id}/editar`}>
                  <Pencil className="h-3.5 w-3.5" />
                  Editar
                </Link>
                <button
                  className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs text-destructive hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={isDeleting}
                  onClick={() => onDelete(row.original.id)}
                  type="button"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  Baja
                </button>
              </>
            ) : null}
          </div>
        )
      }
    ],
    [canEdit, isDeleting, onDelete]
  );

  const table = useReactTable({ data: relaciones, columns, getCoreRowModel: getCoreRowModel() });

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
