"use client";

/* eslint-disable react-hooks/incompatible-library */

import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table";
import Link from "next/link";
import { useMemo } from "react";
import { Pencil, Search, Trash2 } from "lucide-react";
import type { ObjetoVeteranoResponseDTO, VeteranoResponseDTO } from "../types";

type VeteranosTableProps = {
  veteranos: VeteranoResponseDTO[];
  objetos: ObjetoVeteranoResponseDTO[];
  canEdit: boolean;
  isDeleting?: boolean;
  onDelete: (id: number) => void;
};

export function VeteranosTable({ canEdit, isDeleting = false, objetos, onDelete, veteranos }: VeteranosTableProps) {
  const columns = useMemo<ColumnDef<VeteranoResponseDTO>[]>(() => [
    { accessorKey: "nombre", header: "Nombre" },
    { accessorKey: "apellido", header: "Apellido" },
    { accessorKey: "fuerza", header: "Fuerza" },
    { id: "objetos", header: "Objetos", cell: ({ row }) => objetos.filter((objeto) => objeto.veteranoId === row.original.id).length },
    { id: "acciones", header: "Acciones", cell: ({ row }) => (
      <div className="flex justify-end gap-2">
        <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/veteranos/${row.original.id}`}><Search className="h-3.5 w-3.5" />Ver</Link>
        {canEdit ? <><Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/veteranos/${row.original.id}/editar`}><Pencil className="h-3.5 w-3.5" />Editar</Link><button className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs text-destructive hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60" disabled={isDeleting} onClick={() => onDelete(row.original.id)} type="button"><Trash2 className="h-3.5 w-3.5" />Baja</button></> : null}
      </div>
    )}
  ], [canEdit, isDeleting, objetos, onDelete]);
  const table = useReactTable({ data: veteranos, columns, getCoreRowModel: getCoreRowModel() });
  return <div className="overflow-hidden rounded-lg border"><table className="w-full border-collapse text-sm"><thead className="bg-muted/60">{table.getHeaderGroups().map((hg) => <tr key={hg.id}>{hg.headers.map((h) => <th className="px-4 py-3 text-left font-medium" key={h.id}>{h.isPlaceholder ? null : flexRender(h.column.columnDef.header, h.getContext())}</th>)}</tr>)}</thead><tbody>{table.getRowModel().rows.map((row) => <tr className="border-t" key={row.id}>{row.getVisibleCells().map((cell) => <td className="px-4 py-3 align-top" key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</td>)}</tr>)}</tbody></table></div>;
}
