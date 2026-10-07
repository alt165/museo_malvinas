export type UserRole = "ADMIN" | "MUSEOLOGO" | "VIEWER";

export type SessionUser = {
  id?: string;
  username?: string;
  email?: string;
  name?: string;
  roles: UserRole[];
};

export type AuthSession = {
  authenticated: boolean;
  loading: boolean;
  user: SessionUser | null;
};
