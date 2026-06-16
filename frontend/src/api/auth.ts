import { api } from "./api";

export type LoginRequest = { email: string; password: string; };

export type RegisterRequest = LoginRequest & {
  confirmPassword: string;
  firstName: string;
  lastName: string;
  levelId: number;
  positionId: number;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
};

export async function login(req: LoginRequest): Promise<AuthResponse> {
  const res = await api.post<AuthResponse>("/api/auth/login", req);
  return res.data;
}

export async function register(req: RegisterRequest): Promise<AuthResponse> {
  const res = await api.post<AuthResponse>("/api/auth/register", req);
  return res.data;
}
