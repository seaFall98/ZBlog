export type AuthUser = {
  id: number;
  email: string;
  nickname: string;
  avatar: string;
  badge: string | null;
  website: string | null;
  bio: string | null;
  role: string;
  is_enabled: boolean;
  deleted_at: string | null;
  last_login: string | null;
  created_at: string | null;
  has_password: boolean;
  linked_oauths?: string[];
};

export type LoginResponse = {
  access_token: string;
  token_type: string;
  expires_in: number;
  user: AuthUser;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type RegisterPayload = {
  email: string;
  nickname: string;
  password: string;
  website?: string;
};

export type ForgotPasswordPayload = {
  email: string;
};

export type ResetPasswordPayload = {
  email: string;
  code: string;
  password: string;
};

export type AuthSession = {
  accessToken: string;
  tokenType: string;
  expiresAt: number;
  user: AuthUser;
};
