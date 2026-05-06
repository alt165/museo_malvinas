"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const { authenticated, loading, login } = useAuth();

  useEffect(() => {
    if (loading) {
      return;
    }

    if (authenticated) {
      router.replace("/dashboard");
      return;
    }

    void login();
  }, [authenticated, loading, login, router]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-background p-6">
      <div className="w-full max-w-sm space-y-4 rounded-lg border bg-card p-6 text-center">
        <div>
          <h1 className="text-xl font-semibold">Ingreso administrativo</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Se redirigira a Keycloak para iniciar sesion.
          </p>
        </div>
        <button
          className="inline-flex h-10 w-full items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90"
          onClick={() => void login()}
          type="button"
        >
          Ingresar
        </button>
      </div>
    </main>
  );
}
