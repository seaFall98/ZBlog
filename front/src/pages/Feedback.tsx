import { useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRightIcon, CheckCircle2Icon, FileUpIcon, Loader2Icon } from "lucide-react";
import { toast } from "sonner";
import PageLayout from "../components/layout/PageLayout";
import { useAuth } from "../features/auth/AuthProvider";
import { feedbackApi } from "../features/feedback/feedbackApi";
import { rememberFeedback } from "../features/feedback/feedbackHistory";
import type { FeedbackSubmitPayload, FeedbackTicket, FeedbackType } from "../features/feedback/types";

const FEEDBACK_TYPES: Array<{ value: FeedbackType; label: string }> = [
  { value: "suggestion", label: "功能建议" },
  { value: "summary", label: "内容勘误" },
  { value: "inappropriate", label: "不当内容" },
  { value: "copyright", label: "版权反馈" },
];

const MAX_ATTACHMENT_COUNT = 5;
const MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024;

function currentPageUrl() {
  if (typeof window === "undefined") return "";
  return window.location.href;
}

function normalizeUploadError(error: unknown) {
  if (error instanceof Error) return error.message;
  return "附件上传失败，请稍后重试";
}

function formatFileSize(size: number) {
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`;
  return `${Math.ceil(size / 1024)} KB`;
}

export default function Feedback() {
  const { authenticated, user } = useAuth();
  const [reportType, setReportType] = useState<FeedbackType>("suggestion");
  const [reportUrl, setReportUrl] = useState(currentPageUrl());
  const [email, setEmail] = useState(user?.email ?? "");
  const [description, setDescription] = useState("");
  const [reason, setReason] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState<FeedbackTicket | null>(null);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!description.trim()) {
      toast.error("请先描述你遇到的问题");
      return;
    }

    setSubmitting(true);
    try {
      const attachmentFiles: string[] = [];
      for (const file of files) {
        try {
          const uploaded = await feedbackApi.uploadAttachment(file);
          attachmentFiles.push(uploaded.file_url);
        } catch (error) {
          throw new Error(normalizeUploadError(error));
        }
      }

      const payload: FeedbackSubmitPayload = {
        reportType,
        reportUrl: reportUrl.trim() || currentPageUrl(),
        email: email.trim() || undefined,
        description: description.trim(),
        reason: reason.trim() || undefined,
        attachmentFiles,
      };

      const ticket = await feedbackApi.submit(payload);
      rememberFeedback(ticket);
      setSubmitted(ticket);
      toast.success("反馈已提交，我们会在工单里继续处理");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "反馈提交失败");
    } finally {
      setSubmitting(false);
    }
  }

  function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(event.target.files ?? []);
    const valid = selected.filter((file) => file.size <= MAX_ATTACHMENT_SIZE).slice(0, MAX_ATTACHMENT_COUNT);
    if (selected.some((file) => file.size > MAX_ATTACHMENT_SIZE)) {
      toast.error("单个附件不能超过 10MB");
    }
    if (selected.length > MAX_ATTACHMENT_COUNT) {
      toast.error(`最多选择 ${MAX_ATTACHMENT_COUNT} 个附件`);
    }
    setFiles(valid);
    event.currentTarget.value = "";
  }

  return (
    <PageLayout>
      <div className="mx-auto max-w-5xl px-6 pb-24 pt-14 md:px-8">
        <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px, 4vw, 58px)", fontWeight: 400 }}>
              反馈
            </h1>
            <p className="mt-2 text-sm" style={{ color: "var(--muted-ink)" }}>
              提交后可在“我的反馈”查看处理进度。
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link
              to="/feedback/mine"
              className="inline-flex h-11 items-center gap-2 border px-4 text-sm transition hover:-translate-y-0.5"
              style={{ borderColor: "var(--ink)", background: "var(--ink)", color: "var(--warm-white)" }}
            >
              我的反馈
              <ArrowRightIcon size={15} />
            </Link>
            {!authenticated && (
              <Link to="/login" className="inline-flex h-11 items-center border px-4 text-sm transition hover:-translate-y-0.5" style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}>
                登录
              </Link>
            )}
          </div>
        </div>

        <div className="border bg-white p-5 shadow-custom sm:p-7" style={{ borderColor: "var(--warm-border)" }}>
            {submitted ? (
              <div className="min-h-[540px]">
                <div className="flex h-12 w-12 items-center justify-center rounded-full" style={{ background: "var(--section-bg)", color: "var(--olive)" }}>
                  <CheckCircle2Icon size={24} />
                </div>
                <p className="mt-8 text-xs uppercase tracking-[0.24em]" style={{ color: "var(--muted-ink)" }}>
                  Ticket Created
                </p>
                <h2 className="mt-3 text-3xl font-normal" style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)" }}>
                  {submitted.ticket_no}
                </h2>
                <p className="mt-4 max-w-xl text-sm leading-7" style={{ color: "var(--muted-ink)" }}>
                  工单已进入待处理队列。你可以在“我的反馈”查看进度、补充材料或继续回复。
                </p>
                <div className="mt-8 grid gap-3 border-t pt-6 text-sm" style={{ borderColor: "var(--warm-border)" }}>
                  <div className="flex items-center justify-between gap-4">
                    <span style={{ color: "var(--muted-ink)" }}>当前状态</span>
                    <span style={{ color: "var(--ink)" }}>{submitted.status_label ?? "待处理"}</span>
                  </div>
                  <div className="flex items-center justify-between gap-4">
                    <span style={{ color: "var(--muted-ink)" }}>记录方式</span>
                    <span style={{ color: "var(--ink)" }}>{authenticated ? "已绑定账号" : "已保存到本机 30 天"}</span>
                  </div>
                </div>
                {!authenticated && submitted.access_token && (
                  <div className="mt-6 border p-4 text-xs leading-6" style={{ borderColor: "var(--warm-border)", background: "var(--ivory)", color: "var(--muted-ink)" }}>
                    匿名工单的访问密钥已保存在当前浏览器。更换设备或清除浏览器数据后，需要使用访问密钥查询。
                  </div>
                )}
                <div className="mt-8 flex flex-wrap gap-3">
                  <Link
                    to={`/feedback/mine?ticket=${encodeURIComponent(submitted.ticket_no)}`}
                    className="inline-flex h-11 items-center gap-2 border px-4 text-sm transition hover:-translate-y-0.5"
                    style={{ borderColor: "var(--ink)", background: "var(--ink)", color: "var(--warm-white)" }}
                  >
                    打开工单
                    <ArrowRightIcon size={15} />
                  </Link>
                  <button
                    type="button"
                    className="inline-flex h-11 items-center border px-4 text-sm transition hover:-translate-y-0.5"
                    style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                    onClick={() => {
                      setSubmitted(null);
                      setDescription("");
                      setReason("");
                      setFiles([]);
                    }}
                  >
                    再提交一条
                  </button>
                </div>
              </div>
            ) : (
              <form className="grid gap-7" onSubmit={handleSubmit}>
                <div>
                  <label className="text-sm font-medium" style={{ color: "var(--ink)" }}>
                    反馈类型
                  </label>
                  <div className="mt-3 grid gap-3 sm:grid-cols-2">
                    {FEEDBACK_TYPES.map((item) => (
                      <button
                        key={item.value}
                        type="button"
                        className="border p-4 text-left transition hover:-translate-y-0.5"
                        style={{
                          borderColor: reportType === item.value ? "var(--ink)" : "var(--warm-border)",
                          background: reportType === item.value ? "var(--section-bg)" : "var(--warm-white)",
                        }}
                        onClick={() => setReportType(item.value)}
                      >
                        <span className="block text-sm font-medium" style={{ color: "var(--ink)" }}>
                          {item.label}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="grid gap-5 sm:grid-cols-2">
                  <label className="grid gap-2 text-sm">
                    <span style={{ color: "var(--ink)" }}>相关地址</span>
                    <input
                      value={reportUrl}
                      onChange={(event) => setReportUrl(event.target.value)}
                      className="h-11 border bg-transparent px-3 text-sm outline-none focus:border-[var(--olive)]"
                      style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                      placeholder="https://..."
                    />
                  </label>
                  <label className="grid gap-2 text-sm">
                    <span style={{ color: "var(--ink)" }}>邮箱</span>
                    <input
                      type="email"
                      value={email}
                      onChange={(event) => setEmail(event.target.value)}
                      className="h-11 border bg-transparent px-3 text-sm outline-none focus:border-[var(--olive)]"
                      style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                      placeholder="可选，便于匿名回复提醒"
                    />
                  </label>
                </div>

                <label className="grid gap-2 text-sm">
                  <span style={{ color: "var(--ink)" }}>问题描述</span>
                  <textarea
                    required
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    className="min-h-36 resize-y border bg-transparent px-3 py-3 text-sm leading-7 outline-none focus:border-[var(--olive)]"
                    style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                    placeholder="写清问题、位置和期望处理方式"
                  />
                </label>

                <label className="grid gap-2 text-sm">
                  <span style={{ color: "var(--ink)" }}>补充说明</span>
                  <textarea
                    value={reason}
                    onChange={(event) => setReason(event.target.value)}
                    className="min-h-24 resize-y border bg-transparent px-3 py-3 text-sm leading-7 outline-none focus:border-[var(--olive)]"
                    style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                    placeholder="可选"
                  />
                </label>

                <div className="border border-dashed p-4" style={{ borderColor: "var(--warm-border)", background: "var(--ivory)" }}>
                  <label className="flex cursor-pointer items-center justify-between gap-4">
                    <span className="flex min-w-0 items-center gap-3">
                      <span className="flex h-10 w-10 shrink-0 items-center justify-center border bg-white" style={{ borderColor: "var(--warm-border)", color: "var(--olive)" }}>
                        <FileUpIcon size={18} />
                      </span>
                      <span className="min-w-0">
                        <span className="block text-sm" style={{ color: "var(--ink)" }}>
                          上传截图或证明文件
                        </span>
                        <span className="mt-1 block text-xs" style={{ color: "var(--muted-ink)" }}>
                          可选，最多 5 个，单个不超过 10MB
                        </span>
                      </span>
                    </span>
                    <input
                      type="file"
                      multiple
                      accept="image/*,.pdf,.txt,.doc,.docx"
                      className="sr-only"
                      onChange={handleFileChange}
                    />
                  </label>
                  {files.length > 0 && (
                    <div className="mt-4 grid gap-2">
                      {files.map((file) => (
                        <div key={`${file.name}-${file.size}`} className="flex items-center justify-between gap-3 text-xs" style={{ color: "var(--muted-ink)" }}>
                          <span className="truncate">{file.name}</span>
                          <span>{formatFileSize(file.size)}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={submitting}
                  className="inline-flex h-12 items-center justify-center gap-2 border px-5 text-sm transition enabled:hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60"
                  style={{ borderColor: "var(--ink)", background: "var(--ink)", color: "var(--warm-white)" }}
                >
                  {submitting ? <Loader2Icon className="animate-spin" size={16} /> : null}
                  提交反馈
                </button>
              </form>
            )}
        </div>
      </div>
    </PageLayout>
  );
}
