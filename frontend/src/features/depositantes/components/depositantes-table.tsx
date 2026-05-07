"use client";

/* eslint-disable react-hooks/incompatible-library */

import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table";
import { Pencil, Search, Trash2 } from "lucide-react";
import Link from "next/link";
import { useMemo } from "react";
import type { DepositanteResponseDTO } from "../types";
import { nombreVisible, telefonoVisible } from "../utils";

type DepositantesTableProps = {
  depositantes: DepositanteResponseDTO[];
  canEdit: boolean;
  isDeleting?: boolean;
  onDelete: (id: number) => void;
};

export function DepositantesTable({ canEdit, depositantes, isDeleting = false, onDelete }: DepositantesTableProps) {
  const columns = useMemo<ColumnDef<DepositanteResponseDTO>[]>(
    () => [
      {
        accessorKey: "tipo",
        header: "Tipo"
      },
      {
        id: "nombre",
        header: "Nombre / organizacion",
        cell: ({ row }) => <span className="font-medium">{nombreVisible(row.original)}</span>
      },
      {
        accessorKey: "contacto",
        header: "Email",
        cell: ({ row }) => row.original.contacto || "Sin email"
      },
      {
        id: "telefono",
        header: "Telefono",
        cell: ({ row }) => telefonoVisible(row.original)
      },
      {
        id: "acciones",
        header: "Acciones",
        cell: ({ row }) => (
          <div className="flex justify-end gap-2">
            <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/depositantes/${row.original.id}`}>
              <Search className="h-3.5 w-3.5" />
              Ver
            </Link>
            {canEdit ? (
              <>
                <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/depositantes/${row.original.id}/editar`}>
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

  const table = useReactTable({ data: depositantes, columns, getCoreRowModel: getCoreRowModel() });

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
