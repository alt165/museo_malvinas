"use client";

/* eslint-disable react-hooks/incompatible-library */

import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table";
import { Pencil, Search, ShieldCheck, ShieldOff } from "lucide-react";
import Link from "next/link";
import { useMemo } from "react";
import type { UsuarioKeycloakResponseDTO } from "../types";
import { nombreCompleto } from "../utils";

type UsuariosTableProps = {
  usuarios: UsuarioKeycloakResponseDTO[];
  isUpdating?: boolean;
  onToggleEnabled: (usuario: UsuarioKeycloakResponseDTO) => void;
};

export function UsuariosTable({ isUpdating = false, onToggleEnabled, usuarios }: UsuariosTableProps) {
  const columns = useMemo<ColumnDef<UsuarioKeycloakResponseDTO>[]>(
    () => [
      {
        accessorKey: "username",
        header: "Usuario",
        cell: ({ row }) => <span className="font-medium">{row.original.username}</span>
      },
      {
        id: "nombre",
        header: "Nombre",
        cell: ({ row }) => nombreCompleto(row.original)
      },
      {
        accessorKey: "email",
        header: "Email"
      },
      {
        accessorKey: "dni",
        header: "DNI"
      },
      {
        id: "roles",
        header: "Rol",
        cell: ({ row }) => row.original.roles.join(", ") || "Sin rol"
      },
      {
        id: "estado",
        header: "Estado",
        cell: ({ row }) => (
          <span className={row.original.habilitado ? "font-medium text-emerald-700" : "font-medium text-muted-foreground"}>
            {row.original.habilitado ? "Habilitado" : "Deshabilitado"}
          </span>
        )
      },
      {
        id: "acciones",
        header: "Acciones",
        cell: ({ row }) => (
          <div className="flex justify-end gap-2">
            <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/usuarios/${row.original.id}`}>
              <Search className="h-3.5 w-3.5" />
              Ver
            </Link>
            <Link className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted" href={`/usuarios/${row.original.id}/editar`}>
              <Pencil className="h-3.5 w-3.5" />
              Editar
            </Link>
            <button
              className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isUpdating}
              onClick={() => onToggleEnabled(row.original)}
              type="button"
            >
              {row.original.habilitado ? <ShieldOff className="h-3.5 w-3.5" /> : <ShieldCheck className="h-3.5 w-3.5" />}
              {row.original.habilitado ? "Deshabilitar" : "Habilitar"}
            </button>
          </div>
        )
      }
    ],
    [isUpdating, onToggleEnabled]
  );

  const table = useReactTable({ data: usuarios, columns, getCoreRowModel: getCoreRowModel() });

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
