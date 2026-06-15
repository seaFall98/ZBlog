import { type FormEvent, type ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  BadgeCheckIcon,
  CameraIcon,
  Globe2Icon,
  KeyRoundIcon,
  MailIcon,
  SaveIcon,
  ShieldCheckIcon,
  Trash2Icon,
  UserRoundIcon,
} from "lucide-react";
import { toast } from "sonner";
import PageLayout from "../components/layout/PageLayout";
import { useAuth } from "../features/auth/AuthProvider";
import { profileApi } from "../features/auth/profileApi";
import { ApiEnvelopeError } from "../lib/apiEnvelope";
import { ApiHttpError } from "../lib/apiClient";

function errorMessage(error: unknown) {
  if (error instanceof ApiEnvelopeError) return error.message || "请求失败";
  if (error instanceof ApiHttpError) return error.message || "请求失败";
  return "请求失败";
}

function toNullable(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

const RESERVED_BADGES = new Set(["站长", "博主", "管理员", "admin", "root", "super_admin"]);

function formatDate(value: string | null) {
  if (!value) return "未记录";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "未记录";
  return date.toLocaleDateString("zh-CN", { year: "numeric", month: "long", day: "numeric" });
}

function FieldShell({ label, icon, children }: { label: string; icon: ReactNode; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-2 flex items-center gap-2 text-xs" style={{ color: "var(--muted-ink)" }}>
        {icon}
        {label}
      </span>
      {children}
    </label>
  );
}

function TextInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className={`h-11 w-full border bg-transparent px-3 text-sm outline-none transition-colors focus:border-current ${props.className ?? ""}`}
      style={{ borderColor: "var(--warm-border)", color: "var(--ink)", ...props.style }}
    />
  );
}

export default function Profile() {
  const navigate = useNavigate();
  const { user, updateUser, logout } = useAuth();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [nickname, setNickname] = useState(user?.nickname ?? "");
  const [email, setEmail] = useState(user?.email ?? "");
  const [website, setWebsite] = useState(user?.website ?? "");
  const [bio, setBio] = useState(user?.bio ?? "");
  const [badge, setBadge] = useState(user?.badge ?? "");
  const [avatar, setAvatar] = useState(user?.avatar ?? "");
  const [currentPassword, setCurrentPassword] = useState("");
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [deactivatePassword, setDeactivatePassword] = useState("");
  const [profileLoading, setProfileLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [deactivating, setDeactivating] = useState(false);

  const initial = useMemo(() => (nickname.trim() || email.trim() || "U").slice(0, 1).toUpperCase(), [email, nickname]);
  const emailChanged = Boolean(user && email.trim().toLowerCase() !== user.email.toLowerCase());

  useEffect(() => {
    let alive = true;
    profileApi
      .getProfile()
      .then((profile) => {
        if (!alive) return;
        updateUser(profile);
        setNickname(profile.nickname ?? "");
        setEmail(profile.email ?? "");
        setWebsite(profile.website ?? "");
        setBio(profile.bio ?? "");
        setBadge(profile.badge ?? "");
        setAvatar(profile.avatar ?? "");
      })
      .catch((error) => {
        if (alive) toast.error(errorMessage(error));
      })
      .finally(() => {
        if (alive) setProfileLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [updateUser]);

  const saveProfile = async (event: FormEvent) => {
    event.preventDefault();
    if (!nickname.trim()) {
      toast.error("请填写昵称");
      return;
    }
    if (!email.trim()) {
      toast.error("请填写邮箱");
      return;
    }
    if (bio.length > 500) {
      toast.error("简介最多 500 字");
      return;
    }
    const normalizedBadge = badge.trim().toLowerCase();
    if (normalizedBadge && (RESERVED_BADGES.has(badge.trim()) || RESERVED_BADGES.has(normalizedBadge))) {
      toast.error("这个铭牌名称已保留");
      return;
    }
    if (emailChanged && !currentPassword.trim()) {
      toast.error("修改邮箱需要当前密码");
      return;
    }

    setSaving(true);
    try {
      const updated = await profileApi.updateProfile({
        nickname: nickname.trim(),
        email: email.trim().toLowerCase(),
        website: toNullable(website),
        bio: toNullable(bio),
        badge: toNullable(badge),
        avatar,
        current_password: emailChanged ? currentPassword : undefined,
      });
      updateUser(updated);
      setCurrentPassword("");
      if (emailChanged) {
        toast.success("邮箱已更新，请重新登录");
        await logout();
        navigate("/login", { replace: true, state: { from: "/profile" } });
        return;
      }
      toast.success("资料已保存");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const uploadAvatar = async (file: File | undefined) => {
    if (!file) return;
    if (!["image/jpeg", "image/png", "image/gif", "image/webp"].includes(file.type)) {
      toast.error("头像仅支持 jpg、png、webp、gif");
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      toast.error("头像不能超过 2MB");
      return;
    }
    setAvatarUploading(true);
    try {
      const uploaded = await profileApi.uploadAvatar(file);
      const updated = await profileApi.updateProfile({ avatar: uploaded.file_url });
      setAvatar(uploaded.file_url);
      updateUser(updated);
      toast.success("头像已更新");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setAvatarUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const changePassword = async (event: FormEvent) => {
    event.preventDefault();
    if (!oldPassword || !newPassword) {
      toast.error("请填写完整密码");
      return;
    }
    if (newPassword !== confirmPassword) {
      toast.error("两次输入的新密码不一致");
      return;
    }
    setPasswordSaving(true);
    try {
      await profileApi.changePassword({ old_password: oldPassword, new_password: newPassword });
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
      toast.success("密码已更新");
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setPasswordSaving(false);
    }
  };

  const deactivate = async () => {
    if (!deactivatePassword.trim()) {
      toast.error("请填写当前密码");
      return;
    }
    if (!window.confirm("确认注销当前账号？注销后账号将不可用。")) {
      return;
    }
    setDeactivating(true);
    try {
      await profileApi.deactivate(deactivatePassword);
      toast.success("账号已注销");
      await logout();
      navigate("/", { replace: true });
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setDeactivating(false);
    }
  };

  if (!user) return null;

  return (
    <PageLayout>
      <main className="min-h-screen px-5 py-16 md:px-8 md:py-20" style={{ background: "var(--ivory)" }}>
        <div className="mx-auto max-w-6xl">
          <header className="mb-8 flex flex-wrap items-end justify-between gap-4 border-b pb-6" style={{ borderColor: "var(--warm-border)" }}>
            <h1 className="text-3xl md:text-4xl" style={{ color: "var(--ink)", letterSpacing: "0" }}>
              个人资料
            </h1>
            {profileLoading && (
              <span className="text-xs" style={{ color: "var(--muted-ink)" }}>
                同步中...
              </span>
            )}
          </header>

          <section className="grid gap-8 lg:grid-cols-[320px_minmax(0,1fr)]">
            <aside className="space-y-5">
              <div className="border bg-card p-6 shadow-sm" style={{ borderColor: "var(--warm-border)" }}>
                <div className="mx-auto h-28 w-28 overflow-hidden rounded-full border" style={{ borderColor: "var(--warm-border)", background: "var(--section-bg)" }}>
                  {avatar ? (
                    <img src={avatar} alt={nickname} className="h-full w-full object-cover" />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center text-4xl" style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)" }}>
                      {initial}
                    </div>
                  )}
                </div>
                <div className="mt-5 text-center">
                  <h2 className="text-2xl" style={{ color: "var(--ink)", letterSpacing: "0" }}>
                    {nickname || "读者"}
                  </h2>
                  <div className="mt-3 flex justify-center">
                    <span className="inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs" style={{ borderColor: "var(--warm-border)", background: "var(--section-bg)", color: "var(--ink)" }}>
                      <BadgeCheckIcon size={14} />
                      {badge.trim() || "读者"}
                    </span>
                  </div>
                </div>
                <input ref={fileInputRef} type="file" accept="image/jpeg,image/png,image/gif,image/webp" className="hidden" onChange={(event) => uploadAvatar(event.target.files?.[0])} />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={avatarUploading}
                  className="mt-6 flex h-11 w-full items-center justify-center gap-2 border text-sm transition-colors hover:bg-secondary disabled:opacity-50"
                  style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                >
                  <CameraIcon size={16} />
                  {avatarUploading ? "上传中..." : "更换头像"}
                </button>
              </div>

              <div className="grid gap-3 text-sm">
                {[
                  ["账号邮箱", email],
                  ["加入时间", formatDate(user.created_at)],
                  ["最近登录", formatDate(user.last_login)],
                ].map(([label, value]) => (
                  <div key={label} className="border px-4 py-3" style={{ borderColor: "var(--warm-border)", background: "var(--warm-white)" }}>
                    <span className="block text-xs" style={{ color: "var(--muted-ink)" }}>
                      {label}
                    </span>
                    <span className="mt-1 block truncate" style={{ color: "var(--ink)" }}>
                      {value}
                    </span>
                  </div>
                ))}
              </div>
            </aside>

            <div className="space-y-8">
              <form onSubmit={saveProfile} className="border bg-card p-6 shadow-sm md:p-8" style={{ borderColor: "var(--warm-border)" }}>
                <div className="mb-7">
                  <h3 className="text-2xl" style={{ color: "var(--ink)", letterSpacing: "0" }}>
                    基本资料
                  </h3>
                </div>

                <div className="grid gap-5 md:grid-cols-2">
                  <FieldShell label="昵称" icon={<UserRoundIcon size={15} />}>
                    <TextInput value={nickname} onChange={(event) => setNickname(event.target.value)} />
                  </FieldShell>
                  <FieldShell label="邮箱" icon={<MailIcon size={15} />}>
                    <TextInput value={email} onChange={(event) => setEmail(event.target.value)} type="email" />
                  </FieldShell>
                  <FieldShell label="个人网站" icon={<Globe2Icon size={15} />}>
                    <TextInput value={website} onChange={(event) => setWebsite(event.target.value)} placeholder="https://example.com" />
                  </FieldShell>
                  <FieldShell label="铭牌" icon={<BadgeCheckIcon size={15} />}>
                    <TextInput value={badge} onChange={(event) => setBadge(event.target.value)} placeholder="读者" />
                  </FieldShell>
                  <div className="md:col-span-2">
                    <FieldShell label="简介" icon={<UserRoundIcon size={15} />}>
                      <textarea
                        value={bio}
                        onChange={(event) => setBio(event.target.value)}
                        rows={5}
                        maxLength={500}
                        className="w-full resize-none border bg-transparent px-3 py-3 text-sm leading-6 outline-none transition-colors focus:border-current"
                        style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                      />
                    </FieldShell>
                    <div className="mt-2 text-right text-xs" style={{ color: "var(--muted-ink)" }}>
                      {bio.length}/500
                    </div>
                  </div>
                  {emailChanged && (
                    <div className="md:col-span-2">
                      <FieldShell label="当前密码" icon={<KeyRoundIcon size={15} />}>
                        <TextInput value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} type="password" autoComplete="current-password" />
                      </FieldShell>
                    </div>
                  )}
                </div>

                <div className="mt-7 flex justify-end">
                  <button type="submit" disabled={saving} className="flex h-11 items-center justify-center gap-2 px-5 text-sm font-medium transition-opacity hover:opacity-85 disabled:opacity-50" style={{ background: "var(--ink)", color: "var(--warm-white)" }}>
                    <SaveIcon size={16} />
                    {saving ? "保存中..." : "保存资料"}
                  </button>
                </div>
              </form>

              <div className="grid gap-8 xl:grid-cols-2">
                <form onSubmit={changePassword} className="border bg-card p-6 shadow-sm" style={{ borderColor: "var(--warm-border)" }}>
                  <div className="mb-6 flex items-center gap-3">
                    <span className="flex h-10 w-10 items-center justify-center rounded-full" style={{ background: "var(--section-bg)", color: "var(--ink)" }}>
                      <ShieldCheckIcon size={18} />
                    </span>
                    <h3 className="text-xl" style={{ color: "var(--ink)", letterSpacing: "0" }}>
                      安全设置
                    </h3>
                  </div>
                  <div className="space-y-4">
                    <TextInput value={oldPassword} onChange={(event) => setOldPassword(event.target.value)} type="password" autoComplete="current-password" placeholder="当前密码" />
                    <TextInput value={newPassword} onChange={(event) => setNewPassword(event.target.value)} type="password" autoComplete="new-password" placeholder="新密码" />
                    <TextInput value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} type="password" autoComplete="new-password" placeholder="确认新密码" />
                  </div>
                  <button type="submit" disabled={passwordSaving} className="mt-5 h-10 w-full border text-sm transition-colors hover:bg-secondary disabled:opacity-50" style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}>
                    {passwordSaving ? "更新中..." : "更新密码"}
                  </button>
                </form>

                <section className="border bg-card p-6 shadow-sm" style={{ borderColor: "var(--warm-border)" }}>
                  <div className="mb-6 flex items-center gap-3">
                    <span className="flex h-10 w-10 items-center justify-center rounded-full" style={{ background: "#F7ECE9", color: "#9A3A2F" }}>
                      <Trash2Icon size={18} />
                    </span>
                    <h3 className="text-xl" style={{ color: "var(--ink)", letterSpacing: "0" }}>
                      注销账号
                    </h3>
                  </div>
                  <TextInput value={deactivatePassword} onChange={(event) => setDeactivatePassword(event.target.value)} type="password" autoComplete="current-password" placeholder="当前密码" />
                  <button type="button" onClick={deactivate} disabled={deactivating} className="mt-5 h-10 w-full text-sm font-medium transition-opacity hover:opacity-85 disabled:opacity-50" style={{ background: "#9A3A2F", color: "white" }}>
                    {deactivating ? "处理中..." : "注销账号"}
                  </button>
                </section>
              </div>
            </div>
          </section>
        </div>
      </main>
    </PageLayout>
  );
}
