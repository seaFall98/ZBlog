import { apiClient } from "../../lib/apiClient";
import type {
  ForgotPasswordPayload,
  LoginPayload,
  LoginResponse,
  RegisterPayload,
  ResetPasswordPayload,
} from "./authTypes";

export const authApi = {
  login(payload: LoginPayload) {
    return apiClient.post<LoginResponse>("/auth/login", payload);
  },

  register(payload: RegisterPayload) {
    return apiClient.post<LoginResponse>("/auth/register", payload);
  },

  forgotPassword(payload: ForgotPasswordPayload) {
    return apiClient.post<{ sent: boolean }>("/auth/forgot-password", payload);
  },

  resetPassword(payload: ResetPasswordPayload) {
    return apiClient.post<null>("/auth/reset-password", payload);
  },

  refresh() {
    return apiClient.post<LoginResponse>("/auth/refresh");
  },

  logout() {
    return apiClient.post<null>("/auth/logout");
  },
};
