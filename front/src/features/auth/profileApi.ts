import { apiClient } from "../../lib/apiClient";
import type { AuthUser } from "./authTypes";

export type UpdateProfilePayload = {
  email?: string;
  nickname?: string;
  avatar?: string;
  badge?: string | null;
  website?: string | null;
  bio?: string | null;
  current_password?: string;
};

export type ChangePasswordPayload = {
  old_password: string;
  new_password: string;
};

export type UploadFileResponse = {
  id: number;
  file_url: string;
  file_name: string;
  original_name: string;
  file_size: number;
};

export const profileApi = {
  getProfile() {
    return apiClient.get<AuthUser>("/user/profile");
  },

  updateProfile(payload: UpdateProfilePayload) {
    return apiClient.put<AuthUser>("/user/profile", payload);
  },

  changePassword(payload: ChangePasswordPayload) {
    return apiClient.put<null>("/user/password", payload);
  },

  deactivate(password: string) {
    return apiClient.delete<null>("/user/deactivate", { password });
  },

  uploadAvatar(file: File) {
    const body = new FormData();
    body.append("file", file);
    body.append("type", "用户头像");
    return apiClient.upload<UploadFileResponse>("/upload", body);
  },
};
