"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const { authenticated, loading, login } = useAuth();
  const loginStarted = useRef(false);

  useEffect(() => {
    if (loading || loginStarted.current) {
      return;
    }

    if (authenticated) {
      router.replace("/dashboard");
      return;
    }

    loginStarted.current = true;
    void login();
  }, [authenticated, loading, login, router]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-primary px-6 text-sm font-medium text-primary-foreground">
      Redirigiendo al inicio de sesion...
    </main>
  );
}
