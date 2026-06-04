insert into settings (group_name, key_name, value_text)
select 'basic', 'basic.author', 'seaFall'
where not exists (select 1 from settings where group_name = 'basic' and key_name = 'basic.author');

insert into settings (group_name, key_name, value_text)
select 'basic', 'basic.author_email', 'hello@example.com'
where not exists (
  select 1 from settings where group_name = 'basic' and key_name = 'basic.author_email'
);

insert into settings (group_name, key_name, value_text)
select 'basic', 'basic.author_desc', '记录技术、产品和生活的个人博客'
where not exists (
  select 1 from settings where group_name = 'basic' and key_name = 'basic.author_desc'
);

insert into settings (group_name, key_name, value_text)
select 'basic', 'basic.author_avatar', '/avatar.webp'
where not exists (
  select 1 from settings where group_name = 'basic' and key_name = 'basic.author_avatar'
);

insert into settings (group_name, key_name, value_text)
select 'basic', 'basic.author_photo', '/avatar.webp'
where not exists (
  select 1 from settings where group_name = 'basic' and key_name = 'basic.author_photo'
);

insert into settings (group_name, key_name, value_text)
select 'basic', 'basic.icp', ''
where not exists (select 1 from settings where group_name = 'basic' and key_name = 'basic.icp');

insert into settings (group_name, key_name, value_text)
select 'basic', 'basic.police_record', ''
where not exists (
  select 1 from settings where group_name = 'basic' and key_name = 'basic.police_record'
);

insert into settings (group_name, key_name, value_text)
select 'basic', 'basic.admin_url', 'http://localhost:4000'
where not exists (
  select 1 from settings where group_name = 'basic' and key_name = 'basic.admin_url'
);

insert into settings (group_name, key_name, value_text)
select 'basic', 'basic.blog_url', 'http://localhost:3000'
where not exists (
  select 1 from settings where group_name = 'basic' and key_name = 'basic.blog_url'
);

insert into settings (group_name, key_name, value_text)
select 'basic', 'basic.home_url', 'http://localhost:3000'
where not exists (
  select 1 from settings where group_name = 'basic' and key_name = 'basic.home_url'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.title', 'ZBlog'
where not exists (select 1 from settings where group_name = 'blog' and key_name = 'blog.title');

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.subtitle', 'Modern Blog'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.subtitle'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.slogan', 'Build in public, write with clarity.'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.slogan'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.description', 'A practical blog about development, products, and notes.'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.description'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.keywords', 'blog, java, spring boot, nuxt'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.keywords'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.established', '2024-01-01'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.established'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.favicon', '/favicon.ico'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.favicon'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.background_image', '/bg.webp'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.background_image'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.screenshot', '/bg.webp'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.screenshot'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.announcement', '<p>本地演示环境已就绪，欢迎先浏览页面结构与交互。</p>'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.announcement'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.typing_texts', '["Write clearly","Ship steadily","Learn in public"]'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.typing_texts'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.sidebar_social',
       '[{"name":"GitHub","url":"https://github.com/seaFall98","icon":"github-line"},{"name":"Email","url":"mailto:hello@example.com","icon":"mail-line"}]'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.sidebar_social'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.footer_social',
       '[{"name":"GitHub","url":"https://github.com/seaFall98","icon":"github-line","position":"left"},{"name":"Email","url":"mailto:hello@example.com","icon":"mail-line","position":"right"}]'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.footer_social'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.footer_links',
       '[{"name":"归档","url":"/archive"},{"name":"分类","url":"/categories"},{"name":"标签","url":"/tags"}]'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.footer_links'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_describe', 'A builder who cares about product feel and technical quality.'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_describe'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_describe_tips', 'This page is still evolving and will be enriched later.'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_describe_tips'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_exhibition', '/bg.webp'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_exhibition'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_profile',
       '[{"label":"Name","value":"seaFall","color":"#43a6c6"},{"label":"Role","value":"Developer","color":"#5d7ce0"},{"label":"Stack","value":"Java + Vue","color":"#7c5ce0"}]'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_profile'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_personality', 'Calm, curious, and shipping-oriented.'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_personality'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_motto_main', '["Keep building","Keep learning"]'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_motto_main'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_motto_sub', 'Small steps compound into real progress.'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_motto_sub'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_socialize',
       '[{"name":"GitHub","url":"https://github.com/seaFall98"},{"name":"Email","url":"mailto:hello@example.com"}]'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_socialize'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_creation',
       '[{"name":"ZBlog","url":"http://localhost:3000"}]'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_creation'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_versions',
       '[{"name":"Frontend","version":"Nuxt 4"},{"name":"Backend","version":"Java 21"},{"name":"Infra","version":"Docker Compose"}]'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_versions'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_unions', '[]'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_unions'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.about_story', 'This local environment is seeded for functional preview and will be refined with real content.'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.about_story'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.custom_head', ''
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.custom_head'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.custom_body', ''
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.custom_body'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.emojis', ''
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.emojis'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.font', ''
where not exists (select 1 from settings where group_name = 'blog' and key_name = 'blog.font');

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.moments_size', '30'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.moments_size'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.message_content', '欢迎留言交流，分享你的想法或建议。'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.message_content'
);

insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.home_layout', 'waterfall'
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.home_layout'
);

insert into settings (group_name, key_name, value_text)
select 'upload', 'upload.storage_type', 'local'
where not exists (
  select 1 from settings where group_name = 'upload' and key_name = 'upload.storage_type'
);

insert into settings (group_name, key_name, value_text)
select 'upload', 'upload.max_file_size', '10'
where not exists (
  select 1 from settings where group_name = 'upload' and key_name = 'upload.max_file_size'
);

insert into settings (group_name, key_name, value_text)
select 'upload', 'upload.path_pattern', '{timestamp}_{random}{ext}'
where not exists (
  select 1 from settings where group_name = 'upload' and key_name = 'upload.path_pattern'
);

insert into settings (group_name, key_name, value_text)
select 'upload', 'upload.use_ssl', 'true'
where not exists (
  select 1 from settings where group_name = 'upload' and key_name = 'upload.use_ssl'
);

insert into menus (type, title, url, icon, sort_order)
select 'navigation', '首页', '/', 'ri-home-line', 1
where not exists (select 1 from menus where type = 'navigation' and url = '/');

insert into menus (type, title, url, icon, sort_order)
select 'navigation', '归档', '/archive', 'ri-archive-line', 2
where not exists (select 1 from menus where type = 'navigation' and url = '/archive');

insert into menus (type, title, url, icon, sort_order)
select 'navigation', '分类', '/categories', 'ri-folder-line', 3
where not exists (select 1 from menus where type = 'navigation' and url = '/categories');

insert into menus (type, title, url, icon, sort_order)
select 'navigation', '标签', '/tags', 'ri-price-tag-3-line', 4
where not exists (select 1 from menus where type = 'navigation' and url = '/tags');

insert into menus (type, title, url, icon, sort_order)
select 'navigation', '动态', '/moment', 'ri-bubble-chart-line', 5
where not exists (select 1 from menus where type = 'navigation' and url = '/moment');

insert into menus (type, title, url, icon, sort_order)
select 'navigation', '留言', '/message', 'ri-message-3-line', 6
where not exists (select 1 from menus where type = 'navigation' and url = '/message');

insert into menus (type, title, url, icon, sort_order)
select 'navigation', '关于', '/about', 'ri-information-line', 7
where not exists (select 1 from menus where type = 'navigation' and url = '/about');

insert into menus (type, title, url, icon, sort_order)
select 'footer', '分类', '/categories', 'ri-folder-line', 1
where not exists (select 1 from menus where type = 'footer' and url = '/categories');

insert into menus (type, title, url, icon, sort_order)
select 'footer', '标签', '/tags', 'ri-price-tag-3-line', 2
where not exists (select 1 from menus where type = 'footer' and url = '/tags');

insert into menus (type, title, url, icon, sort_order)
select 'footer', '统计', '/statistics', 'ri-bar-chart-line', 3
where not exists (select 1 from menus where type = 'footer' and url = '/statistics');

insert into menus (type, title, url, icon, sort_order)
select 'footer', '友链', '/friend', 'ri-links-line', 4
where not exists (select 1 from menus where type = 'footer' and url = '/friend');
