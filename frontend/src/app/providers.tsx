"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "@/lib/auth/auth-provider";
import { EditingModeProvider } from "@/lib/editing-mode";
import { queryClient } from "@/lib/api/query-client";

export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider><EditingModeProvider>{children}</EditingModeProvider></AuthProvider>
    </QueryClientProvider>
  );
}
