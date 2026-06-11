import LegalPage from "../components/content/LegalPage";
import { useSiteProfile } from "../features/site/useSiteProfile";

export default function Copyright() {
  const { profile } = useSiteProfile();

  return (
    <LegalPage
      eyebrow="Copyright"
      title="版权协议"
      lead="本站所有原创内容采用知识共享 署名-非商业性使用-禁止演绎 4.0 国际许可协议（CC BY-NC-ND 4.0）进行许可。本页面说明本站内容的版权归属和使用许可。"
      updatedAt="2025年10月"
      contactEmail={profile.email}
      sections={[
        {
          title: "版权所有者",
          paragraphs: [
            "本站所有原创文章、图片、设计的版权归本站所有。用户发表的评论版权归用户本人所有，但授予本站展示和使用的权利。",
          ],
        },
        {
          title: "许可协议说明",
          paragraphs: ["CC BY-NC-ND 4.0 协议包含以下限制："],
        },
        {
          title: "署名（BY）",
          paragraphs: [
            "转载时必须注明作者和原文链接。建议在文章开头或明显位置添加超链接，格式如：本文转载自 [原文标题](原文链接)。",
          ],
        },
        {
          title: "非商业性使用（NC）",
          paragraphs: ["禁止将本站内容用于商业目的，包括但不限于："],
          bullets: [
            "在转载页面插入广告（如 Google AdSense、百度联盟等）",
            "要求付费才能阅读",
            "要求关注公众号、下载 App 等才能查看",
            "用于商业培训、出版等盈利行为",
          ],
        },
        {
          title: "禁止演绎（ND）",
          paragraphs: [
            "禁止对原文进行修改、改编或基于原文创作衍生作品。你可以完整转载（不修改原文内容，保留所有信息）或部分引用（作为参考资料引用部分内容，需注明出处）。不可以：",
          ],
          bullets: [
            "修改原文内容后发布",
            "翻译成其他语言后发布",
            "基于原文创作衍生作品",
          ],
        },
        {
          title: "受保护的内容",
          paragraphs: ["以下内容受版权保护："],
          bullets: [
            "本站所有原创文章（标题、正文、代码示例）",
            "文章配图和封面图片",
            "网站设计和页面布局",
            "原创图标和素材",
          ],
        },
        {
          title: "特殊许可",
          paragraphs: ["在以下情况下，可能获得额外授权："],
          bullets: [
            "学术研究：用于非营利性学术研究和教育目的，不做任何限制，可自由使用",
            "友链博主：被本站友链收录的博客可享有更宽松的引用权限",
            "个别授权：如有其他特殊需求，可通过评论或邮件联系协商",
          ],
        },
        {
          title: "侵权处理",
          paragraphs: [
            "如发现未经授权的使用，我们保留追究法律责任的权利。同时，我们也尊重他人的版权，如认为本站内容侵犯了你的权利，请联系我们删除。",
            "联系方式：{contactEmail}",
          ],
        },
        {
          title: "免责声明",
          paragraphs: [
            "本站内容仅供学习和参考，不对内容的准确性、完整性和时效性做任何保证。因使用本站内容造成的任何损失，本站不承担责任。",
          ],
        },
      ]}
    />
  );
}
