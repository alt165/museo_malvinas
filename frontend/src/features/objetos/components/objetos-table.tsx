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
import { ArrowDown, ArrowUp, ChevronsUpDown, MoveRight, Pencil, Search, Trash2 } from "lucide-react";
import type { ObjetoMuseoResponseDTO, ObjetoSortField, ObjetosSort } from "../types";
import { resumenDescripcion } from "../utils";

type ObjetosTableProps = {
  objetos: ObjetoMuseoResponseDTO[];
  canEdit: boolean;
  deletingId?: number | null;
  onDelete?: (objeto: ObjetoMuseoResponseDTO) => void;
  onMove?: (objeto: ObjetoMuseoResponseDTO) => void;
  sort: ObjetosSort;
  onSortChange: (field: ObjetoSortField) => void;
};

export function ObjetosTable({ canEdit, deletingId, objetos, onDelete, onMove, onSortChange, sort }: ObjetosTableProps) {
  const columns = useMemo<ColumnDef<ObjetoMuseoResponseDTO>[]>(
    () => [
      {
        accessorKey: "numeroInventario",
        header: () => <SortHeader field="numeroInventario" label="Numero de inventario" onSortChange={onSortChange} sort={sort} />,
        cell: ({ row }) => <span className="font-medium">{row.original.numeroInventario}</span>
      },
      {
        accessorKey: "denominacionObjeto",
        header: () => <SortHeader field="denominacionObjeto" label="Denominacion" onSortChange={onSortChange} sort={sort} />,
        cell: ({ row }) => row.original.denominacionObjeto
      },
      {
        id: "categorias",
        header: "Categorias",
        cell: ({ row }) => row.original.categorias?.map((categoria) => categoria.nombre).join(", ") || "Sin categorias"
      },
      {
        accessorKey: "descripcion",
        header: () => <SortHeader field="descripcion" label="Descripcion" onSortChange={onSortChange} sort={sort} />,
        cell: ({ row }) => <span className="text-muted-foreground">{resumenDescripcion(row.original.descripcion)}</span>
      },
      {
        accessorKey: "fechaIngreso",
        header: () => <SortHeader field="fechaIngreso" label="Fecha de ingreso" onSortChange={onSortChange} sort={sort} />,
        cell: ({ row }) => formatearFecha(row.original.fechaIngreso)
      },
      {
        accessorKey: "estadoConservacion",
        header: () => <SortHeader field="estadoConservacion" label="Estado" onSortChange={onSortChange} sort={sort} />,
        cell: ({ row }) => row.original.estadoConservacion ?? "Sin dato"
      },
      {
        id: "acciones",
        header: "Acciones",
        cell: ({ row }) => (
          <div className="flex justify-end gap-2">
            <Link
              className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted"
              href={`/objetos/${row.original.id}`}
            >
              <Search className="h-3.5 w-3.5" />
              Ver
            </Link>
            {canEdit ? (
              <>
                <Link
                  className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted"
                  href={`/objetos/${row.original.id}/editar`}
                >
                  <Pencil className="h-3.5 w-3.5" />
                  Editar
                </Link>
                {onMove ? (
                  <button
                    className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted"
                    onClick={() => onMove(row.original)}
                    type="button"
                  >
                    <MoveRight className="h-3.5 w-3.5" />
                    Mover
                  </button>
                ) : null}
                {onDelete ? (
                  <button
                    className="inline-flex h-8 items-center gap-1 rounded-md border border-red-200 px-2 text-xs text-red-700 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60"
                    disabled={deletingId === row.original.id}
                    onClick={() => onDelete(row.original)}
                    type="button"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    Baja
                  </button>
                ) : null}
              </>
            ) : null}
          </div>
        )
      }
    ],
    [canEdit, deletingId, onDelete, onMove, onSortChange, sort]
  );

  const table = useReactTable({
    data: objetos,
    columns,
    getCoreRowModel: getCoreRowModel()
  });

  return (
    <div className="overflow-hidden rounded-lg border">
      <table className="w-full border-collapse text-sm">
        <thead className="bg-primary text-primary-foreground">
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id}>
              {headerGroup.headers.map((header) => (
                <th className="px-4 py-3 text-left font-semibold text-white" key={header.id}>
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

function SortHeader({
  field,
  label,
  onSortChange,
  sort
}: {
  field: ObjetoSortField;
  label: string;
  onSortChange: (field: ObjetoSortField) => void;
  sort: ObjetosSort;
}) {
  const activo = sort.field === field;
  const Icon = activo ? (sort.direction === "asc" ? ArrowUp : ArrowDown) : ChevronsUpDown;

  return (
    <button
      className="inline-flex items-center gap-1 text-left font-semibold text-white hover:text-secondary"
      onClick={() => onSortChange(field)}
      type="button"
    >
      <span>{label}</span>
      <Icon className="h-3.5 w-3.5" />
    </button>
  );
}

function formatearFecha(fecha?: string | null) {
  if (!fecha) {
    return "Sin dato";
  }

  const [year, month, day] = fecha.split("-");
  if (!year || !month || !day) {
    return fecha;
  }

  return `${day}/${month}/${year}`;
}
