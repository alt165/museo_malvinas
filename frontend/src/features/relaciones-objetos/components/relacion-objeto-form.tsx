"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { useObjetosQuery } from "@/features/objetos/queries";
import { ApiClientError } from "@/lib/errors/api-error";
import type { RelacionObjetoRequestDTO, RelacionObjetoResponseDTO } from "../types";
import { relacionObjetoSchema, type RelacionObjetoFormValues } from "../schemas";
import { getApiErrorMessage, getValidationErrors } from "../utils";

type RelacionObjetoFormProps = {
  initialValue?: RelacionObjetoResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: RelacionObjetoRequestDTO) => void;
};

export function RelacionObjetoForm({
  initialValue,
  isSubmitting = false,
  onSubmit,
  submitError,
  submitLabel
}: RelacionObjetoFormProps) {
  const objetosQuery = useObjetosQuery();
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError
  } = useForm<RelacionObjetoFormValues>({
    resolver: zodResolver(relacionObjetoSchema),
    defaultValues: {
      objetoOrigenId: initialValue?.objetoOrigenId ?? 0,
      objetoDestinoId: initialValue?.objetoDestinoId ?? 0,
      tipoRelacion: initialValue?.tipoRelacion ?? "",
      descripcion: initialValue?.descripcion ?? ""
    }
  });

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (
        field === "objetoOrigenId" ||
        field === "objetoDestinoId" ||
        field === "tipoRelacion" ||
        field === "descripcion"
      ) {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  if (objetosQuery.isLoading) {
    return <LoadingState label="Cargando objetos..." />;
  }

  if (objetosQuery.isError) {
    return (
      <ErrorState
        message={getApiErrorMessage(objetosQuery.error)}
        requestId={objetosQuery.error instanceof ApiClientError ? objetosQuery.error.requestId : undefined}
      />
    );
  }

  const objetos = objetosQuery.data ?? [];

  return (
    <form
      className="max-w-4xl space-y-5 rounded-lg border p-5"
      onSubmit={handleSubmit((values) =>
        onSubmit({
          objetoOrigenId: values.objetoOrigenId,
          objetoDestinoId: values.objetoDestinoId,
          tipoRelacion: values.tipoRelacion.trim(),
          descripcion: values.descripcion?.trim() || null
        })
      )}
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <Field label="Objeto origen" error={errors.objetoOrigenId?.message}>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            {...register("objetoOrigenId", { valueAsNumber: true })}
          >
            <option value={0}>Seleccionar objeto</option>
            {objetos.map((objeto) => (
              <option key={objeto.id} value={objeto.id}>
                {objeto.numeroInventario} - {objeto.nombre}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Objeto destino" error={errors.objetoDestinoId?.message}>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            {...register("objetoDestinoId", { valueAsNumber: true })}
          >
            <option value={0}>Seleccionar objeto</option>
            {objetos.map((objeto) => (
              <option key={objeto.id} value={objeto.id}>
                {objeto.numeroInventario} - {objeto.nombre}
              </option>
            ))}
          </select>
        </Field>
      </div>
      <Field label="Tipo de relacion" error={errors.tipoRelacion?.message}>
        <input
          className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          {...register("tipoRelacion")}
        />
      </Field>
      <Field label="Descripcion" error={errors.descripcion?.message}>
        <textarea
          className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          {...register("descripcion")}
        />
      </Field>
      <div className="flex items-center gap-3">
        <button
          className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? "Guardando..." : submitLabel}
        </button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/relaciones-objetos">
          Cancelar
        </Link>
      </div>
    </form>
  );
}

function Field({ children, error, label }: { children: React.ReactNode; error?: string; label: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      {children}
      {error ? <p className="font-normal text-destructive">{error}</p> : null}
    </label>
  );
}
