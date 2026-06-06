import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { EyeIcon, EyeOffIcon } from "lucide-react";
import { toast } from "sonner";
import PageLayout from "../components/layout/PageLayout";

export default function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      toast.error("请填写账号和密码");
      return;
    }
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      if (username === "admin" && password === "123456") {
        toast.success("登录成功，欢迎回来 ✨");
        navigate("/");
      } else {
        toast.error("账号或密码错误，请重试");
      }
    }, 1200);
  };

  return (
    <PageLayout>
      <div
        className="min-h-screen flex items-center justify-center px-8"
        style={{ background: "var(--ivory)" }}
      >
        <div className="w-full max-w-md">
          {/* Logo area */}
          <div className="text-center mb-12">
            <Link
              to="/"
              className="inline-block"
            >
              <div
                style={{
                  fontFamily: "var(--fontDisplay)",
                  fontSize: "28px",
                  fontWeight: 400,
                  color: "var(--ink)",
                  letterSpacing: "0.04em",
                }}
              >
                寂静之书
              </div>
            </Link>
            <p className="mt-2 text-xs" style={{ color: "var(--muted-ink)" }}>
              管理员登录
            </p>
          </div>

          {/* Form card */}
          <div
            className="p-10"
            style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)" }}
          >
            <form onSubmit={handleSubmit} className="flex flex-col gap-6">
              {/* Username */}
              <div>
                <label
                  className="block text-xs mb-2"
                  style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)", letterSpacing: "0.06em" }}
                >
                  账号
                </label>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="admin"
                  className="w-full h-11 px-4 text-sm bg-transparent border outline-none focus:border-current transition-colors"
                  style={{
                    borderColor: "var(--warm-border)",
                    color: "var(--ink)",
                    fontFamily: "var(--fontSans)",
                  }}
                  autoComplete="username"
                />
              </div>

              {/* Password */}
              <div>
                <label
                  className="block text-xs mb-2"
                  style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)", letterSpacing: "0.06em" }}
                >
                  密码
                </label>
                <div className="relative">
                  <input
                    type={showPwd ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="123456"
                    className="w-full h-11 px-4 pr-11 text-sm bg-transparent border outline-none focus:border-current transition-colors"
                    style={{
                      borderColor: "var(--warm-border)",
                      color: "var(--ink)",
                      fontFamily: "var(--fontSans)",
                    }}
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPwd(!showPwd)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 opacity-50 hover:opacity-80 transition-opacity"
                    style={{ color: "var(--ink)" }}
                  >
                    {showPwd ? <EyeOffIcon size={16} /> : <EyeIcon size={16} />}
                  </button>
                </div>
              </div>

              {/* Submit */}
              <button
                type="submit"
                disabled={loading}
                className="mt-2 h-11 w-full text-sm font-medium transition-opacity hover:opacity-80 disabled:opacity-50"
                style={{
                  background: "var(--ink)",
                  color: "var(--warm-white)",
                  fontFamily: "var(--fontSans)",
                  letterSpacing: "0.05em",
                }}
              >
                {loading ? "登录中..." : "登 录"}
              </button>
            </form>

            <div className="mt-6 text-center">
              <p className="text-xs" style={{ color: "var(--muted-ink)" }}>
                演示账号：admin / 123456
              </p>
            </div>
          </div>

          <div className="mt-8 text-center">
            <Link
              to="/"
              className="text-xs transition-opacity hover:opacity-70"
              style={{ color: "var(--muted-ink)" }}
            >
              ← 返回首页
            </Link>
          </div>
        </div>
      </div>
    </PageLayout>
  );
}
