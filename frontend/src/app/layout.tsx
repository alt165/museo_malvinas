import type { Metadata } from "next";
import "reactflow/dist/style.css";
import "./globals.css";
import { AppProviders } from "./providers";
import { runtimeConfigScript } from "@/lib/runtime-config";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Museo Malvinas",
  description: "Administracion del inventario del museo",
  icons: {
    icon: "/favicon.png"
  }
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  const runtimeConfig = runtimeConfigScript({
    apiBaseUrl: process.env.MUSEO_PUBLIC_APP_URL ?? "http://localhost:8080",
    keycloakUrl: process.env.MUSEO_PUBLIC_AUTH_URL ?? "http://localhost:8081",
    keycloakRealm: process.env.MUSEO_KEYCLOAK_REALM ?? "museo",
    keycloakClientId: process.env.MUSEO_KEYCLOAK_CLIENT_ID ?? "museo-frontend"
  });
  return (
    <html lang="es">
      <body>
        <script dangerouslySetInnerHTML={{ __html: `window.__MUSEO_RUNTIME_CONFIG__=${runtimeConfig};` }} />
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}
