"use client";

/* eslint-disable react-hooks/incompatible-library */

import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table";
import { Pencil, Search, Trash2 } from "lucide-react";
import Link from "next/link";
import { useMemo } from "react";
import type { ColeccionObjetoResponseDTO } from "../types";
import { resumenDescripcion } from "../utils";

type ColeccionesTableProps = {
  canDelete: boolean;
  canEdit: boolean;
  colecciones: ColeccionObjetoResponseDTO[];
  isDeleting?: boolean;
  onDelete: (id: number) => void;
};

export function ColeccionesTable({ canDelete, canEdit, colecciones, isDeleting = false, onDelete }: ColeccionesTableProps) {
  const columns = useMemo<ColumnDef<ColeccionObjetoResponseDTO>[]>(
    () => [
      {
        accessorKey: "nombre",
        header: "Nombre",
        cell: ({ row }) => <span className="font-medium">{row.original.nombre}</span>
      },
      {
        accessorKey: "descripcion",
        header: "Descripcion",
        cell: ({ row }) => <span className="text-muted-foreground">{resumenDescripcion(row.original.descripcion)}</span>
      },
      {
        accessorKey: "cantidadObjetos",
        header: "Objetos",
        cell: ({ row }) => row.original.cantidadObjetos ?? 0
      },
      {
        id: "acciones",
        header: "Acciones",
        cell: ({ row }) => (
          <div className="flex justify-end gap-2">
            <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/objetos/colecciones/${row.original.id}`}>
              <Search className="h-3.5 w-3.5" />
              Ver
            </Link>
            {canEdit ? (
              <>
                <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/objetos/colecciones/${row.original.id}/editar`}>
                  <Pencil className="h-3.5 w-3.5" />
                  Editar
                </Link>
                {canDelete ? (
                  <button
                    className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs text-destructive hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
                    disabled={isDeleting}
                    onClick={() => onDelete(row.original.id)}
                    type="button"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    Eliminar
                  </button>
                ) : null}
              </>
            ) : null}
          </div>
        )
      }
    ],
    [canDelete, canEdit, isDeleting, onDelete]
  );

  const table = useReactTable({ data: colecciones, columns, getCoreRowModel: getCoreRowModel() });

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
