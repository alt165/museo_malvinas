"use client";

import { useEffect } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const { authenticated, loading, login } = useAuth();

  useEffect(() => {
    if (loading) {
      return;
    }

    if (!loading && authenticated) {
      router.replace("/dashboard");
    }
  }, [authenticated, loading, router]);

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-primary px-6 py-10">
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-cover bg-center"
        style={{ backgroundImage: "url('/images/fondo-login.jpg')" }}
      />
      <div aria-hidden="true" className="absolute inset-0 bg-primary/75" />
      <div aria-hidden="true" className="absolute inset-x-0 bottom-0 h-2 bg-accent" />

      <form
        className="relative z-10 w-full max-w-md rounded-lg border border-white/30 bg-white/95 p-8 text-center shadow-2xl backdrop-blur"
        onSubmit={(event) => {
          event.preventDefault();
          void login();
        }}
      >
        <Image
          alt="Museo Malvinas"
          className="mx-auto h-28 w-28 rounded-full border-4 border-accent object-cover shadow-md"
          height={112}
          priority
          src="/images/logo-login.jpg"
          width={112}
        />
        <h1 className="mt-5 text-xl font-semibold leading-snug text-primary">
          Archivo Historico del Museo Malvinas, Antartida y Atlantico Sur
        </h1>
        <div className="mt-8 space-y-4 text-left">
          <label className="block text-sm font-medium text-primary" htmlFor="username">
            Usuario
          </label>
          <input
            autoComplete="username"
            className="h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/45"
            id="username"
            name="username"
            type="text"
          />
          <label className="block text-sm font-medium text-primary" htmlFor="password">
            Contrasena
          </label>
          <input
            autoComplete="current-password"
            className="h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/45"
            id="password"
            name="password"
            type="password"
          />
        </div>
        <button
          className="mt-6 inline-flex h-11 w-full items-center justify-center rounded-md bg-primary px-4 text-sm font-semibold text-primary-foreground shadow-sm transition hover:bg-primary/95 focus:outline-none focus:ring-2 focus:ring-secondary focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={loading}
          type="submit"
        >
          Iniciar Sesion
        </button>
      </form>
    </main>
  );
}
