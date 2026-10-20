"use client";

import { useEffect, useRef, useSyncExternalStore } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";

const subscribeLogout = (callback: () => void) => {
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
};
const getLogoutSnapshot = () => window.sessionStorage.getItem("museo.logout") === "1";
const getServerLogoutSnapshot = () => false;

export default function LoginPage() {
  const router = useRouter();
  const { authenticated, loading, login } = useAuth();
  const loginStarted = useRef(false);
  const loggedOut = useSyncExternalStore(subscribeLogout, getLogoutSnapshot, getServerLogoutSnapshot);
  const returnPath = typeof window !== "undefined"
    ? new URLSearchParams(window.location.search).get("redirect")
    : null;

  useEffect(() => {
    if (loading || loginStarted.current) {
      return;
    }

    if (authenticated) {
      router.replace("/dashboard");
      return;
    }

    if (loggedOut) return;
    loginStarted.current = true;
    void login(returnPath);
  }, [authenticated, loading, login, loggedOut, returnPath, router]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-primary px-6 text-sm font-medium text-primary-foreground">
      {loggedOut ? (
        <button className="rounded bg-white px-4 py-2 text-primary" onClick={() => void login(returnPath)}>
          Iniciar sesión
        </button>
      ) : "Redirigiendo al inicio de sesion..."}
    </main>
  );
}
