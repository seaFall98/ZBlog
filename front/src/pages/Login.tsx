import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { EyeIcon, EyeOffIcon, LockIcon, MailIcon, UserIcon } from "lucide-react";
import { toast } from "sonner";
import PageLayout from "../components/layout/PageLayout";
import { useAuth } from "../features/auth/AuthProvider";
import { ApiEnvelopeError } from "../lib/apiEnvelope";
import { ApiHttpError } from "../lib/apiClient";
import qqIcon from "../assets/auth/qq.svg";
import wechatIcon from "../assets/auth/wechat.svg";

type AuthMode = "login" | "register" | "forgot";

function GitHubMark() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="currentColor">
      <path d="M12 2C6.48 2 2 6.58 2 12.26c0 4.52 2.87 8.35 6.84 9.7.5.1.68-.22.68-.49 0-.24-.01-.88-.01-1.73-2.78.62-3.37-1.38-3.37-1.38-.45-1.18-1.11-1.5-1.11-1.5-.91-.64.07-.63.07-.63 1 .07 1.53 1.06 1.53 1.06.9 1.57 2.35 1.12 2.92.86.09-.67.35-1.12.63-1.38-2.22-.26-4.55-1.14-4.55-5.07 0-1.12.39-2.04 1.03-2.76-.1-.26-.45-1.3.1-2.72 0 0 .84-.28 2.75 1.05A9.3 9.3 0 0 1 12 6.93c.85 0 1.7.12 2.5.34 1.9-1.33 2.74-1.05 2.74-1.05.55 1.42.2 2.46.1 2.72.64.72 1.03 1.64 1.03 2.76 0 3.94-2.34 4.81-4.57 5.06.36.32.68.94.68 1.9 0 1.38-.01 2.49-.01 2.83 0 .27.18.6.69.49A10.07 10.07 0 0 0 22 12.26C22 6.58 17.52 2 12 2Z" />
    </svg>
  );
}

function authErrorMessage(error: unknown) {
  if (error instanceof ApiHttpError) {
    if (error.status === 429) {
      return "尝试次数过多，请稍后再试";
    }
    return error.message || "请求失败";
  }
  if (error instanceof ApiEnvelopeError) {
    if (error.message.includes("Too many")) {
      return "尝试次数过多，请稍后再试";
    }
    return error.message || "请求失败";
  }
  return "请求失败";
}

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { authenticated, login, register, forgotPassword, resetPassword } = useAuth();
  const [mode, setMode] = useState<AuthMode>(() => {
    if (location.pathname === "/register") return "register";
    if (location.pathname === "/forgot-password") return "forgot";
    return "login";
  });
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [code, setCode] = useState("");
  const [codeSent, setCodeSent] = useState(false);
  const [resendSeconds, setResendSeconds] = useState(0);
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);

  const from = (location.state as { from?: string } | null)?.from ?? "/";

  const copy = useMemo(() => {
    if (mode === "register") {
      return { title: "创建账号", action: "注册并登录" };
    }
    if (mode === "forgot") {
      return { title: "找回密码", action: codeSent ? "重置密码" : "发送验证码" };
    }
    return { title: "欢迎回来", action: "登录" };
  }, [codeSent, mode]);

  useEffect(() => {
    if (authenticated) {
      navigate(from, { replace: true });
    }
  }, [authenticated, from, navigate]);

  useEffect(() => {
    if (resendSeconds <= 0) return;
    const timer = window.setTimeout(() => setResendSeconds((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearTimeout(timer);
  }, [resendSeconds]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      toast.error("请填写邮箱");
      return;
    }
    if (mode !== "forgot" || codeSent) {
      if (!password.trim()) {
        toast.error("请填写密码");
        return;
      }
    }
    if ((mode === "register" || (mode === "forgot" && codeSent)) && password !== confirmPassword) {
      toast.error("两次输入的密码不一致");
      return;
    }

    setLoading(true);
    try {
      if (mode === "register") {
        if (!nickname.trim()) {
          toast.error("请填写昵称");
          return;
        }
        await register({ email: trimmedEmail, nickname: nickname.trim(), password });
        toast.success("注册成功");
        navigate(from, { replace: true });
        return;
      }
      if (mode === "forgot") {
        if (!codeSent) {
          await forgotPassword(trimmedEmail);
          setCodeSent(true);
          setResendSeconds(60);
          toast.success("验证码已发送");
          return;
        }
        if (!code.trim()) {
          toast.error("请填写验证码");
          return;
        }
        await resetPassword({ email: trimmedEmail, code: code.trim(), password });
        toast.success("密码已重置，请重新登录");
        setMode("login");
        setPassword("");
        setConfirmPassword("");
        setCode("");
        setCodeSent(false);
        return;
      }
      await login({ email: trimmedEmail, password });
      toast.success("登录成功");
      navigate(from, { replace: true });
    } catch (error) {
      toast.error(authErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const switchMode = (nextMode: AuthMode) => {
    setMode(nextMode);
    setPassword("");
    setConfirmPassword("");
    setCode("");
    setCodeSent(false);
    setResendSeconds(0);
  };

  const resendCode = async () => {
    const trimmedEmail = email.trim();
    if (!trimmedEmail || resendSeconds > 0) return;
    setLoading(true);
    try {
      await forgotPassword(trimmedEmail);
      setResendSeconds(60);
      toast.success("验证码已发送");
    } catch (error) {
      toast.error(authErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <PageLayout>
      <main className="min-h-screen px-6 py-24" style={{ background: "var(--ivory)" }}>
        <div className="mx-auto grid min-h-[calc(100vh-12rem)] w-full max-w-5xl items-center gap-12 md:grid-cols-[1fr_420px]">
          <section className="hidden md:block">
            <Link
              to="/"
              className="inline-block"
              style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)", fontSize: "30px" }}
            >
              寂静之书
            </Link>
            <div className="mt-10 h-px w-24" style={{ background: "var(--warm-border)" }} />
            <p className="mt-8 max-w-sm text-sm leading-7" style={{ color: "var(--muted-ink)" }}>
              账号用于评论、回复通知和个人资料管理。
            </p>
          </section>

          <section
            className="border px-7 py-8 shadow-sm"
            style={{ background: "var(--warm-white)", borderColor: "var(--warm-border)" }}
          >
            <div className="mb-7">
              <h1
                className="text-2xl"
                style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)", letterSpacing: "0" }}
              >
                {copy.title}
              </h1>
              <div className="mt-3 h-px w-full" style={{ background: "var(--warm-border)" }} />
            </div>

            <form onSubmit={submit} className="flex flex-col gap-5">
              <label className="block">
                <span className="mb-2 block text-xs" style={{ color: "var(--muted-ink)" }}>
                  邮箱
                </span>
                <span className="relative block">
                  <MailIcon className="absolute left-3 top-1/2 -translate-y-1/2" size={16} style={{ color: "var(--muted-ink)" }} />
                  <input
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    type="email"
                    autoComplete="email"
                    className="h-11 w-full border bg-transparent pl-10 pr-3 text-sm outline-none transition-colors focus:border-current"
                    style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                  />
                </span>
              </label>

              {mode === "register" && (
                <label className="block">
                  <span className="mb-2 block text-xs" style={{ color: "var(--muted-ink)" }}>
                    昵称
                  </span>
                  <span className="relative block">
                    <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2" size={16} style={{ color: "var(--muted-ink)" }} />
                    <input
                      value={nickname}
                      onChange={(event) => setNickname(event.target.value)}
                      autoComplete="nickname"
                      className="h-11 w-full border bg-transparent pl-10 pr-3 text-sm outline-none transition-colors focus:border-current"
                      style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                    />
                  </span>
                </label>
              )}

              {mode === "forgot" && codeSent && (
                <label className="block">
                  <span className="mb-2 flex items-center justify-between text-xs" style={{ color: "var(--muted-ink)" }}>
                    <span>验证码</span>
                    <button
                      type="button"
                      onClick={resendCode}
                      disabled={resendSeconds > 0 || loading}
                      className="transition-opacity hover:opacity-70 disabled:opacity-45"
                    >
                      {resendSeconds > 0 ? `${resendSeconds}s` : "重新发送"}
                    </button>
                  </span>
                  <input
                    value={code}
                    onChange={(event) => setCode(event.target.value)}
                    className="h-11 w-full border bg-transparent px-3 text-sm outline-none transition-colors focus:border-current"
                    style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                  />
                </label>
              )}

              {(mode !== "forgot" || codeSent) && (
                <label className="block">
                  <span className="mb-2 block text-xs" style={{ color: "var(--muted-ink)" }}>
                    密码
                  </span>
                  <span className="relative block">
                    <LockIcon className="absolute left-3 top-1/2 -translate-y-1/2" size={16} style={{ color: "var(--muted-ink)" }} />
                    <input
                      value={password}
                      onChange={(event) => setPassword(event.target.value)}
                      type={showPwd ? "text" : "password"}
                      autoComplete={mode === "login" ? "current-password" : "new-password"}
                      className="h-11 w-full border bg-transparent pl-10 pr-11 text-sm outline-none transition-colors focus:border-current"
                      style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPwd((value) => !value)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 transition-opacity hover:opacity-70"
                      style={{ color: "var(--muted-ink)" }}
                      aria-label={showPwd ? "隐藏密码" : "显示密码"}
                    >
                      {showPwd ? <EyeOffIcon size={16} /> : <EyeIcon size={16} />}
                    </button>
                  </span>
                </label>
              )}

              {(mode === "register" || (mode === "forgot" && codeSent)) && (
                <label className="block">
                  <span className="mb-2 block text-xs" style={{ color: "var(--muted-ink)" }}>
                    确认密码
                  </span>
                  <input
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    type={showPwd ? "text" : "password"}
                    autoComplete="new-password"
                    className="h-11 w-full border bg-transparent px-3 text-sm outline-none transition-colors focus:border-current"
                    style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                  />
                </label>
              )}

              {mode === "login" && (
                <div className="flex justify-end">
                  <button
                    type="button"
                    onClick={() => switchMode("forgot")}
                    className="text-xs transition-opacity hover:opacity-70"
                    style={{ color: "var(--muted-ink)" }}
                  >
                    忘记密码
                  </button>
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="mt-1 h-11 w-full text-sm font-medium transition-opacity hover:opacity-85 disabled:opacity-50"
                style={{ background: "var(--ink)", color: "var(--warm-white)" }}
              >
                {loading ? "处理中..." : copy.action}
              </button>
            </form>

            <div className="my-7 flex items-center gap-3">
              <div className="h-px flex-1" style={{ background: "var(--warm-border)" }} />
              <span className="text-xs" style={{ color: "var(--muted-ink)" }}>
                其他登录方式
              </span>
              <div className="h-px flex-1" style={{ background: "var(--warm-border)" }} />
            </div>

            <div className="flex justify-center gap-3">
              {[
                { label: "GitHub", icon: <GitHubMark /> },
                { label: "QQ", icon: <img src={qqIcon} alt="" className="h-[19px] w-[19px]" /> },
                { label: "微信", icon: <img src={wechatIcon} alt="" className="h-[19px] w-[19px]" /> },
              ].map((item) => (
                <button
                  key={item.label}
                  type="button"
                  aria-label={item.label}
                  className="flex h-10 w-10 items-center justify-center rounded-full border transition-colors hover:bg-secondary"
                  style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                >
                  {item.icon}
                </button>
              ))}
            </div>

            <div className="mt-7 flex items-center justify-between text-xs" style={{ color: "var(--muted-ink)" }}>
              <button type="button" onClick={() => switchMode(mode === "register" ? "login" : "register")}>
                {mode === "register" ? "已有账号，去登录" : "创建账号"}
              </button>
              {mode !== "login" && (
                <button type="button" onClick={() => switchMode("login")}>
                  返回登录
                </button>
              )}
            </div>
          </section>
        </div>
      </main>
    </PageLayout>
  );
}
