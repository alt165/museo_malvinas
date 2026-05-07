"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { resetPasswordSchema, type ResetPasswordFormValues } from "../schemas";
import { getValidationErrors } from "../utils";

type ResetPasswordFormProps = {
  isSubmitting?: boolean;
  submitError?: unknown;
  onSubmit: (values: ResetPasswordFormValues, onDone: () => void) => void;
};

export function ResetPasswordForm({ isSubmitting = false, onSubmit, submitError }: ResetPasswordFormProps) {
  const {
    formState: { errors, isSubmitSuccessful },
    handleSubmit,
    register,
    reset,
    setError
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { contrasena: "" }
  });

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    if (validationErrors.contrasena) {
      setError("contrasena", { message: validationErrors.contrasena });
    }
  }, [setError, submitError]);

  useEffect(() => {
    if (isSubmitSuccessful && !submitError) {
      reset({ contrasena: "" });
    }
  }, [isSubmitSuccessful, reset, submitError]);

  return (
    <form
      className="max-w-2xl space-y-4 rounded-lg border p-5"
      onSubmit={handleSubmit((values) => onSubmit(values, () => reset({ contrasena: "" })))}
    >
      <label className="space-y-2 text-sm font-medium">
        <span>Nueva contrasena temporal</span>
        <input
          className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          autoComplete="new-password"
          type="password"
          {...register("contrasena")}
        />
        {errors.contrasena?.message ? <p className="font-normal text-destructive">{errors.contrasena.message}</p> : null}
      </label>
      <button
        className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm font-medium hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60"
        disabled={isSubmitting}
        type="submit"
      >
        {isSubmitting ? "Reseteando..." : "Resetear contrasena temporal"}
      </button>
    </form>
  );
}
