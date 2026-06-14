import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { authApi } from "./authApi";
import type { AuthSession, AuthUser, LoginPayload, RegisterPayload, ResetPasswordPayload } from "./authTypes";
import { setApiAuthTokenProvider } from "../../lib/apiClient";

const STORAGE_KEY = "zblog:v2:auth";
const REFRESH_SKEW_MS = 2 * 60 * 1000;

type AuthContextValue = {
  user: AuthUser | null;
  session: AuthSession | null;
  authenticated: boolean;
  initializing: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  forgotPassword: (email: string) => Promise<void>;
  resetPassword: (payload: ResetPasswordPayload) => Promise<void>;
  refresh: () => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (user: AuthUser) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function toSession(response: {
  access_token: string;
  token_type: string;
  expires_in: number;
  user: AuthUser;
}): AuthSession {
  return {
    accessToken: response.access_token,
    tokenType: response.token_type,
    expiresAt: Date.now() + response.expires_in * 1000,
    user: response.user,
  };
}

function readStoredSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as AuthSession;
    if (!session.accessToken || !session.user || session.expiresAt <= Date.now()) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

function persistSession(session: AuthSession | null) {
  if (session) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => {
    // Read stored session AND synchronously set the token provider so it is
    // available before the first render and any child effects. This is the
    // only way to eliminate the timing gap on initial page load.
    const stored = readStoredSession();
    if (stored) {
      setApiAuthTokenProvider(() => stored.accessToken);
    }
    return stored;
  });
  const [initializing, setInitializing] = useState(true);

  const applySession = useCallback((nextSession: AuthSession | null) => {
    setSession(nextSession);
    persistSession(nextSession);
    // Set token provider synchronously so it is available before any child effects run.
    setApiAuthTokenProvider(() => nextSession?.accessToken ?? null);
  }, []);

  useEffect(() => {
    // Only set (never clear) — clearing is done in applySession(null) on logout/auth-expired.
    // React fires child effects before parent effects (bottom-up), so clearing here
    // would race with child API calls on login.
    if (session?.accessToken) {
      setApiAuthTokenProvider(() => session.accessToken);
    }
  }, [session?.accessToken]);

  const refreshingRef = useRef(false);

  const refresh = useCallback(async () => {
    if (!session || refreshingRef.current) return;
    refreshingRef.current = true;
    try {
      const response = await authApi.refresh();
      applySession(toSession(response));
    } finally {
      refreshingRef.current = false;
    }
  }, [applySession, session]);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      if (!session) {
        setInitializing(false);
        return;
      }
      if (session.expiresAt - Date.now() <= REFRESH_SKEW_MS) {
        try {
          const response = await authApi.refresh();
          if (!cancelled) {
            applySession(toSession(response));
          }
        } catch {
          if (!cancelled) {
            applySession(null);
          }
        }
      }
      if (!cancelled) {
        setInitializing(false);
      }
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!session) return;
    const delay = Math.max(0, session.expiresAt - Date.now() - REFRESH_SKEW_MS);
    const timer = window.setTimeout(() => {
      refresh().catch(() => applySession(null));
    }, delay);
    return () => window.clearTimeout(timer);
  }, [applySession, refresh, session]);

  // Listen for auth-expired events dispatched by the API client on 401 responses.
  useEffect(() => {
    const handler = () => applySession(null);
    window.addEventListener("zblog:auth-expired", handler);
    return () => window.removeEventListener("zblog:auth-expired", handler);
  }, [applySession]);

  const login = useCallback(
    async (payload: LoginPayload) => {
      applySession(toSession(await authApi.login(payload)));
    },
    [applySession],
  );

  const register = useCallback(
    async (payload: RegisterPayload) => {
      applySession(toSession(await authApi.register(payload)));
    },
    [applySession],
  );

  const forgotPassword = useCallback(async (email: string) => {
    await authApi.forgotPassword({ email });
  }, []);

  const resetPassword = useCallback(async (payload: ResetPasswordPayload) => {
    await authApi.resetPassword(payload);
  }, []);

  const logout = useCallback(async () => {
    try {
      if (session) {
        await authApi.logout();
      }
    } finally {
      applySession(null);
    }
  }, [applySession, session]);

  const updateUser = useCallback(
    (user: AuthUser) => {
      setSession((current) => {
        if (!current) return current;
        const nextSession = { ...current, user };
        persistSession(nextSession);
        return nextSession;
      });
    },
    [],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      session,
      authenticated: Boolean(session),
      initializing,
      login,
      register,
      forgotPassword,
      resetPassword,
      refresh,
      logout,
      updateUser,
    }),
    [forgotPassword, initializing, login, logout, refresh, register, resetPassword, session, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return value;
}
