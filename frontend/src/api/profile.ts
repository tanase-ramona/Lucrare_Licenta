import { api } from "./api";

export type UserProfile = {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  levelId: number | null;
  levelName: string | null;
  positionId: number | null;
  positionName: string | null;
  roles: string[];
};

export type UpdateProfileRequest = {
  firstName: string;
  lastName: string;
  levelId: number;
  positionId: number;
};

export async function getMyProfile(): Promise<UserProfile> {
  const res = await api.get<UserProfile>("/api/profile/me");
  return res.data;
}

export async function updateMyProfile(req: UpdateProfileRequest): Promise<UserProfile> {
  const res = await api.put<UserProfile>("/api/profile/me", req);
  return res.data;
}
