import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import * as authApi from "../api/auth";

type AuthUser = {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
};

type AuthState = {
  accessToken: string | null;
  user: AuthUser | null;
};

type AuthContextValue = {
  state: AuthState;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  register: (req: authApi.RegisterRequest) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function readInitialState(): AuthState {
  const accessToken = localStorage.getItem("accessToken");
  const userRaw = localStorage.getItem("user");
  const user = userRaw ? (JSON.parse(userRaw) as AuthUser) : null;
  return { accessToken, user };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>(() => readInitialState());

  useEffect(() => {
    function refreshUser() {
      setState(readInitialState());
    }

    window.addEventListener("user-updated", refreshUser);
    return () => window.removeEventListener("user-updated", refreshUser);
  }, []);

  const value = useMemo<AuthContextValue>(() => {
    return {
      state,
      isAuthenticated: !!state.accessToken,
      login: async (email: string, password: string) => {
        const data = await authApi.login({ email, password });

        localStorage.setItem("accessToken", data.accessToken);
        const user = {
          userId: data.userId,
          email: data.email,
          firstName: data.firstName,
          lastName: data.lastName,
          roles: data.roles,
        };
        localStorage.setItem("user", JSON.stringify(user));

        setState({
          accessToken: data.accessToken,
          user,
        });
      },
      logout: () => {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("user");
        setState({ accessToken: null, user: null });
      },
      register: async (req: authApi.RegisterRequest) => {
        const data = await authApi.register(req);

        const user = {
          userId: data.userId,
          email: data.email,
          firstName: data.firstName,
          lastName: data.lastName,
          roles: data.roles,
        };
        localStorage.setItem("accessToken", data.accessToken);
        localStorage.setItem("user", JSON.stringify(user));

        setState({
          accessToken: data.accessToken,
          user,
        });
      },
    };
  }, [state]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
