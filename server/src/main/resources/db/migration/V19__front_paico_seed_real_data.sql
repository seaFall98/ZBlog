alter table categories add column if not exists cover_url varchar(1000);

with seed_files(filename, original_name, file_url, file_type, file_size, upload_type) as (
  values
    ('front-category-writing.jpg', 'front-category-writing.jpg', 'https://images.unsplash.com/photo-1455390582262-044cdead277a?w=600&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-category-reading.jpg', 'front-category-reading.jpg', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=600&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-category-travel.jpg', 'front-category-travel.jpg', 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=600&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-category-life.jpg', 'front-category-life.jpg', 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-category-film.jpg', 'front-category-film.jpg', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-category-thoughts.jpg', 'front-category-thoughts.jpg', 'https://images.unsplash.com/photo-1518655048521-f130df041f66?w=600&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-post-autumn-light.jpg', 'front-post-autumn-light.jpg', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=1200&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-post-reading.jpg', 'front-post-reading.jpg', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=1200&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-post-city.jpg', 'front-post-city.jpg', 'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=1200&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-post-letters.jpg', 'front-post-letters.jpg', 'https://images.unsplash.com/photo-1455390582262-044cdead277a?w=1200&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-post-coffee.jpg', 'front-post-coffee.jpg', 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-post-film.jpg', 'front-post-film.jpg', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=1200&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-post-solitude.jpg', 'front-post-solitude.jpg', 'https://images.unsplash.com/photo-1518655048521-f130df041f66?w=1200&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-post-architecture.jpg', 'front-post-architecture.jpg', 'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=1200&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-album-kyoto.jpg', 'front-album-kyoto.jpg', 'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-album-shanghai.jpg', 'front-album-shanghai.jpg', 'https://images.unsplash.com/photo-1474181487882-5abf3f0ba6c2?w=800&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-album-winter.jpg', 'front-album-winter.jpg', 'https://images.unsplash.com/photo-1517685352821-92cf88aee5a5?w=800&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-album-botanicals.jpg', 'front-album-botanicals.jpg', 'https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=800&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-001.jpg', 'kyoto_001.jpg', 'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-002.jpg', 'kyoto_002.jpg', 'https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-003.jpg', 'kyoto_003.jpg', 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-004.jpg', 'kyoto_004.jpg', 'https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-005.jpg', 'kyoto_005.jpg', 'https://images.unsplash.com/photo-1578469645742-46cae010e5d4?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-006.jpg', 'kyoto_006.jpg', 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-007.jpg', 'kyoto_007.jpg', 'https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-008.jpg', 'kyoto_008.jpg', 'https://images.unsplash.com/photo-1486299267070-83823f5448dd?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-009.jpg', 'kyoto_009.jpg', 'https://images.unsplash.com/photo-1567016376408-0226e4d0c1ea?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-010.jpg', 'kyoto_010.jpg', 'https://images.unsplash.com/photo-1460627390041-532a28402358?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-011.jpg', 'kyoto_011.jpg', 'https://images.unsplash.com/photo-1512236393565-3a06b61b8e42?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-kyoto-012.jpg', 'kyoto_012.jpg', 'https://images.unsplash.com/photo-1583416750470-965b2707b355?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-shanghai-001.jpg', 'shanghai_001.jpg', 'https://images.unsplash.com/photo-1474181487882-5abf3f0ba6c2?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-shanghai-002.jpg', 'shanghai_002.jpg', 'https://images.unsplash.com/photo-1518005020951-eccb494ad742?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-shanghai-003.jpg', 'shanghai_003.jpg', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-winter-001.jpg', 'winter_001.jpg', 'https://images.unsplash.com/photo-1517685352821-92cf88aee5a5?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-winter-002.jpg', 'winter_002.jpg', 'https://images.unsplash.com/photo-1483664852095-d6cc6870702d?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-winter-003.jpg', 'winter_003.jpg', 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-botanicals-001.jpg', 'botanicals_001.jpg', 'https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-botanicals-002.jpg', 'botanicals_002.jpg', 'https://images.unsplash.com/photo-1501004318641-b39e6451bec6?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-photo-botanicals-003.jpg', 'botanicals_003.jpg', 'https://images.unsplash.com/photo-1497250681960-ef046c08a56e?w=900&q=85', 'image/jpeg', 0, 'front-seed'),
    ('front-moment-coffee.jpg', 'front-moment-coffee.jpg', 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-moment-botanicals.jpg', 'front-moment-botanicals.jpg', 'https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=600&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-friend-writing.jpg', 'front-friend-writing.jpg', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=80&q=80', 'image/jpeg', 0, 'front-seed'),
    ('front-friend-design.jpg', 'front-friend-design.jpg', 'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=80&q=80', 'image/jpeg', 0, 'front-seed')
)
insert into files (filename, original_name, file_url, file_type, file_size, upload_type)
select filename, original_name, file_url, file_type, file_size, upload_type
from seed_files seed
where not exists (select 1 from files f where f.file_url = seed.file_url and f.deleted_at is null);

insert into categories (name, slug, description, cover_url, sort_order)
select '写作', 'writing', '关于写作的思考与实践', 'https://images.unsplash.com/photo-1455390582262-044cdead277a?w=600&q=80', 10
where not exists (select 1 from categories where slug = 'writing');
insert into categories (name, slug, description, cover_url, sort_order)
select '阅读', 'reading', '书籍、评论与思想碎片', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=600&q=80', 20
where not exists (select 1 from categories where slug = 'reading');
insert into categories (name, slug, description, cover_url, sort_order)
select '旅行', 'travel', '路途中的风景与感悟', 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=600&q=80', 30
where not exists (select 1 from categories where slug = 'travel');
insert into categories (name, slug, description, cover_url, sort_order)
select '生活', 'life', '日常的细小惊喜', 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&q=80', 40
where not exists (select 1 from categories where slug = 'life');
insert into categories (name, slug, description, cover_url, sort_order)
select '影像', 'film', '镜头与光影的叙事', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600&q=80', 50
where not exists (select 1 from categories where slug = 'film');
insert into categories (name, slug, description, cover_url, sort_order)
select '随想', 'thoughts', '未成形的念头与灵感', 'https://images.unsplash.com/photo-1518655048521-f130df041f66?w=600&q=80', 60
where not exists (select 1 from categories where slug = 'thoughts');

insert into tags (name, slug, description)
select tag_name, tag_slug, 'Paico front baseline tag'
from (values
  ('秋日', 'autumn'), ('读书笔记', 'reading-notes'), ('散文', 'essay'), ('摄影', 'photography'),
  ('城市', 'city'), ('咖啡', 'coffee'), ('日记', 'diary'), ('电影', 'movie'),
  ('建筑', 'architecture'), ('光', 'light'), ('旅途', 'journey'), ('夜晚', 'night'),
  ('书信', 'letters'), ('记忆', 'memory'), ('孤独', 'solitude'), ('季节', 'season'),
  ('语言', 'language'), ('时间', 'time'), ('梦境', 'dream'), ('色彩', 'color')
) as seed(tag_name, tag_slug)
where not exists (select 1 from tags t where t.slug = seed.tag_slug);

insert into articles (slug, title, summary, cover_url, content_markdown, content_html, content_text, status, category_id, is_top, is_essence, published_at)
select 'autumn-light', '秋日午后的光线总是来得比预想中更温柔', '窗外的梧桐叶还挂着，金黄中带一点锈色，风一吹，就有几片打着旋儿落下来。这样的下午，适合什么都不做。', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=1200&q=80', '光的温度。窗外的梧桐叶还挂着，金黄中带一点锈色，风一吹，就有几片打着旋儿落下来。这样的下午，适合什么都不做。关于停下来。生活的质感由小小的静止时刻累积而成。', '<h2>光的温度</h2><p>窗外的梧桐叶还挂着，金黄中带一点锈色，风一吹，就有几片打着旋儿落下来。这样的下午，适合什么都不做，只是坐在那里，看光从窗缝里斜进来，落在书桌的一角，慢慢移动。</p><blockquote>每一个平凡的下午都藏着某种神圣的东西，只是我们太忙，忘记了停下来接住它。</blockquote><h2>关于停下来</h2><p>生活的质感，由这些小小的静止时刻累积而成。</p>', '光的温度。窗外的梧桐叶还挂着。每一个平凡的下午都藏着某种神圣的东西。关于停下来，生活的质感由小小的静止时刻累积而成。', 'PUBLISHED', (select id from categories where slug = 'life'), true, true, timestamp '2024-10-24 10:00:00'
where not exists (select 1 from articles where slug = 'autumn-light');
insert into articles (slug, title, summary, cover_url, content_markdown, content_html, content_text, status, category_id, is_top, is_essence, published_at)
select 'on-reading-slowly', '慢读：重新学习用眼睛触碰文字', '我们已经太习惯快速阅读了——扫描，提取，关闭。但真正的阅读需要摩擦，需要停顿，需要一点阻力。', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=1200&q=80', '速度的代价。真正的阅读需要摩擦，需要停顿，需要一点阻力。身体阅读。好的阅读者，是用整个身体在读。', '<h2>速度的代价</h2><p>我们已经太习惯快速阅读了——扫描，提取，关闭。但真正的阅读需要摩擦，需要停顿，需要一点阻力。</p><h2>身体阅读</h2><p>慢读不只是速度的问题，它是一种身体姿态。</p><blockquote>好的阅读者，是用整个身体在读，而不仅仅是用眼睛。</blockquote>', '速度的代价。真正的阅读需要摩擦，需要停顿，需要一点阻力。身体阅读。好的阅读者，是用整个身体在读。', 'PUBLISHED', (select id from categories where slug = 'reading'), true, true, timestamp '2024-10-18 10:00:00'
where not exists (select 1 from articles where slug = 'on-reading-slowly');
insert into articles (slug, title, summary, cover_url, content_markdown, content_html, content_text, status, category_id, is_top, is_essence, published_at)
select 'city-at-dusk', '城市在黄昏时会短暂地变得可爱', '太阳快落山的那二十分钟，城市里所有坚硬的东西都软化了。玻璃幕墙变成橙色的镜子，路面反光，人们的脸被镀了一层暖调。', 'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=1200&q=80', '黄金二十分钟。太阳快落山的那二十分钟，城市里所有坚硬的东西都软化了。', '<h2>黄金二十分钟</h2><p>太阳快落山的那二十分钟，城市里所有坚硬的东西都软化了。这个城市，平时充满噪音和效率，在这一刻忽然变得有点动人。</p>', '黄金二十分钟。太阳快落山的那二十分钟，城市里所有坚硬的东西都软化了。', 'PUBLISHED', (select id from categories where slug = 'travel'), true, true, timestamp '2024-10-10 10:00:00'
where not exists (select 1 from articles where slug = 'city-at-dusk');
insert into articles (slug, title, summary, cover_url, content_markdown, content_html, content_text, status, category_id, is_top, is_essence, published_at)
select 'on-letters', '写信这件事，在消失之前值得被记录', '我收到最后一封手写信，是2017年。寄信人是我的外婆，信纸是淡蓝色的，字迹有些颤抖。', 'https://images.unsplash.com/photo-1455390582262-044cdead277a?w=1200&q=80', '最后的手写信。信的重量。写信是一种非常缓慢的关心。每一封信都是一次愿意慢下来的证明。', '<h2>最后的手写信</h2><p>我收到最后一封手写信，是2017年。寄信人是我的外婆，信纸是淡蓝色的，字迹有些颤抖。</p><h2>信的重量</h2><p>写信是一种非常缓慢的关心。</p><blockquote>每一封信都是一次愿意慢下来的证明。</blockquote>', '最后的手写信。信的重量。写信是一种非常缓慢的关心。每一封信都是一次愿意慢下来的证明。', 'PUBLISHED', (select id from categories where slug = 'writing'), false, false, timestamp '2024-09-28 10:00:00'
where not exists (select 1 from articles where slug = 'on-letters');
insert into articles (slug, title, summary, cover_url, content_markdown, content_html, content_text, status, category_id, is_top, is_essence, published_at)
select 'coffee-ritual', '咖啡仪式：一个人的早晨需要一点庄重感', '每天早上，我会花十五分钟手冲咖啡。不只是因为它好喝，更因为这个过程本身。', 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=1200&q=80', '仪式的意义。每天早上，我会花十五分钟手冲咖啡。这是一天中最安静的十五分钟。', '<h2>仪式的意义</h2><p>每天早上，我会花十五分钟手冲咖啡。不只是因为它好喝，更因为这个过程本身。</p>', '仪式的意义。每天早上，我会花十五分钟手冲咖啡。这是一天中最安静的十五分钟。', 'PUBLISHED', (select id from categories where slug = 'life'), false, false, timestamp '2024-09-15 10:00:00'
where not exists (select 1 from articles where slug = 'coffee-ritual');
insert into articles (slug, title, summary, cover_url, content_markdown, content_html, content_text, status, category_id, is_top, is_essence, published_at)
select 'film-photography', '胶片摄影教会我的几件事', '用胶片拍照，每一次快门都是一次承诺。你不能无限重拍，不能即时查看，你必须信任自己的判断。', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=1200&q=80', '稀缺感的价值。用胶片拍照，每一次快门都是一次承诺。', '<h2>稀缺感的价值</h2><p>用胶片拍照，每一次快门都是一次承诺。你不能无限重拍，不能即时查看，你必须信任自己的判断。</p>', '稀缺感的价值。用胶片拍照，每一次快门都是一次承诺。', 'PUBLISHED', (select id from categories where slug = 'film'), false, false, timestamp '2024-09-02 10:00:00'
where not exists (select 1 from articles where slug = 'film-photography');
insert into articles (slug, title, summary, cover_url, content_markdown, content_html, content_text, status, category_id, is_top, is_essence, published_at)
select 'on-solitude', '孤独有时候是一种奢侈', '不是所有人都能承受真正的孤独，而我花了很多年才学会把它当作礼物而不是惩罚。', 'https://images.unsplash.com/photo-1518655048521-f130df041f66?w=1200&q=80', '孤独的质地。孤独是一个人和自己相处的能力。这个能力越强，你越自由。', '<h2>孤独的质地</h2><p>不是所有人都能承受真正的孤独，而我花了很多年才学会把它当作礼物而不是惩罚。</p><blockquote>孤独是一个人和自己相处的能力。这个能力越强，你越自由。</blockquote>', '孤独的质地。孤独是一个人和自己相处的能力。这个能力越强，你越自由。', 'PUBLISHED', (select id from categories where slug = 'thoughts'), false, false, timestamp '2024-08-20 10:00:00'
where not exists (select 1 from articles where slug = 'on-solitude');
insert into articles (slug, title, summary, cover_url, content_markdown, content_html, content_text, status, category_id, is_top, is_essence, published_at)
select 'architecture-walk', '城市行走：用脚步丈量一座建筑的温度', '那栋老楼的清水混凝土已经有了岁月的痕迹，光打在上面有一种温柔的粗粝感。', 'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=1200&q=80', '混凝土的温度。好的建筑不说话，但它让你觉得你可以停下来。', '<h2>混凝土的温度</h2><p>那栋老楼的清水混凝土已经有了岁月的痕迹，光打在上面有一种温柔的粗粝感。</p><p>好的建筑不说话，但它让你觉得你可以停下来。</p>', '混凝土的温度。好的建筑不说话，但它让你觉得你可以停下来。', 'PUBLISHED', (select id from categories where slug = 'travel'), false, false, timestamp '2024-08-05 10:00:00'
where not exists (select 1 from articles where slug = 'architecture-walk');

insert into article_tags (article_id, tag_id)
select a.id, t.id
from articles a
join (values
  ('autumn-light', 'autumn'), ('autumn-light', 'diary'), ('autumn-light', 'light'), ('autumn-light', 'season'),
  ('on-reading-slowly', 'reading-notes'), ('on-reading-slowly', 'essay'), ('on-reading-slowly', 'language'),
  ('city-at-dusk', 'city'), ('city-at-dusk', 'light'), ('city-at-dusk', 'night'),
  ('on-letters', 'letters'), ('on-letters', 'memory'), ('on-letters', 'time'),
  ('coffee-ritual', 'coffee'), ('coffee-ritual', 'diary'), ('coffee-ritual', 'season'),
  ('film-photography', 'photography'), ('film-photography', 'movie'), ('film-photography', 'color'),
  ('on-solitude', 'solitude'), ('on-solitude', 'time'), ('on-solitude', 'dream'),
  ('architecture-walk', 'architecture'), ('architecture-walk', 'city'), ('architecture-walk', 'journey')
) as seed(article_slug, tag_slug) on seed.article_slug = a.slug
join tags t on t.slug = seed.tag_slug
where not exists (select 1 from article_tags at where at.article_id = a.id and at.tag_id = t.id);

insert into albums (title, slug, description, cover_url, sort_order, is_public)
select '京都·秋', 'kyoto-2024', '2024年秋天，十天的私人巡礼', 'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800&q=80', 10, true
where not exists (select 1 from albums where slug = 'kyoto-2024');
insert into albums (title, slug, description, cover_url, sort_order, is_public)
select '上海街角', 'shanghai-streets', '城市的毛细血管里，那些被忽略的细节', 'https://images.unsplash.com/photo-1474181487882-5abf3f0ba6c2?w=800&q=80', 20, true
where not exists (select 1 from albums where slug = 'shanghai-streets');
insert into albums (title, slug, description, cover_url, sort_order, is_public)
select '冬日光影', 'winter-light', '冬天的光线总是带着某种哀愁', 'https://images.unsplash.com/photo-1517685352821-92cf88aee5a5?w=800&q=80', 30, true
where not exists (select 1 from albums where slug = 'winter-light');
insert into albums (title, slug, description, cover_url, sort_order, is_public)
select '植物研究', 'botanicals', '细小的叶片，无声的生长', 'https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=800&q=80', 40, true
where not exists (select 1 from albums where slug = 'botanicals');

insert into album_photos (album_id, file_id, image_url, title, description, sort_order, is_public, taken_at)
select al.id, f.id, seed.image_url, seed.title, seed.description, seed.sort_order, true, seed.taken_at
from (values
  ('kyoto-2024', 'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=900&q=85', '金阁寺晨雾', '清晨六点，雾气还没散开，金阁寺倒映在水里，有一种梦的质感。', 10, timestamp '2024-11-03 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=900&q=85', '岚山竹林', '竹子很高，光从缝隙里碎下来，踩着影子走，安静得像一首无词歌。', 20, timestamp '2024-11-04 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=900&q=85', '祗园石板路', '下雨天的祗园最好看，石板反光，屋檐滴水，时间好像停了。', 30, timestamp '2024-11-05 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=900&q=85', '枯山水', '白砂被耙出波纹，一块石头，什么都没有，又好像什么都有了。', 40, timestamp '2024-11-06 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1578469645742-46cae010e5d4?w=900&q=85', '红叶坠落', '枫叶落在石阶上，像是谁把画撕碎了，随意散着。', 50, timestamp '2024-11-07 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=900&q=85', '寺庙门口', '一个老僧人走过，袈裟的橙色和银杏叶的黄，撞出了一种诗意。', 60, timestamp '2024-11-08 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=900&q=85', '鸟居群影', '数不清的鸟居排成隧道，红色在黄昏里像要燃起来。', 70, timestamp '2024-11-09 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1486299267070-83823f5448dd?w=900&q=85', '市场的早晨', '锦市场的商贩刚开店，豆腐、咸鱼、腌菜——生活气息扑面而来。', 80, timestamp '2024-11-10 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1567016376408-0226e4d0c1ea?w=900&q=85', '茶室窗景', '一格木窗，窗外是一株老梅，这就是所谓的借景。', 90, timestamp '2024-11-11 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1460627390041-532a28402358?w=900&q=85', '夜之寂静', '夜晚的京都不属于游客，它把最好的一面留给了独自走夜路的人。', 100, timestamp '2024-11-12 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1512236393565-3a06b61b8e42?w=900&q=85', '石庭倒影', '水面上天光云影，和石头的沉默对话。', 110, timestamp '2024-11-13 10:00:00'),
  ('kyoto-2024', 'https://images.unsplash.com/photo-1583416750470-965b2707b355?w=900&q=85', '秋色渐深', '最后一天，树上的叶子已经红透，再待两天就要全落了。', 120, timestamp '2024-11-13 11:00:00'),
  ('shanghai-streets', 'https://images.unsplash.com/photo-1474181487882-5abf3f0ba6c2?w=900&q=85', '天桥下的风', '傍晚的高架桥下，车流像一条缓慢发光的河。', 10, timestamp '2024-09-02 10:00:00'),
  ('shanghai-streets', 'https://images.unsplash.com/photo-1518005020951-eccb494ad742?w=900&q=85', '玻璃幕墙', '云和楼群互相倒映，城市短暂地变得透明。', 20, timestamp '2024-09-03 10:00:00'),
  ('shanghai-streets', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=900&q=85', '路口植物', '水泥缝里的绿色，是城市最小声的反抗。', 30, timestamp '2024-09-04 10:00:00'),
  ('winter-light', 'https://images.unsplash.com/photo-1517685352821-92cf88aee5a5?w=900&q=85', '冷窗', '冬天的窗户像一层安静的滤镜，把世界调成低饱和。', 10, timestamp '2024-01-08 10:00:00'),
  ('winter-light', 'https://images.unsplash.com/photo-1483664852095-d6cc6870702d?w=900&q=85', '雪后小路', '脚印把雪地分成许多细碎的故事。', 20, timestamp '2024-01-09 10:00:00'),
  ('winter-light', 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=900&q=85', '夜雪', '夜色里的雪有一点蓝，像很远的回声。', 30, timestamp '2024-01-10 10:00:00'),
  ('botanicals', 'https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=900&q=85', '叶脉', '一片叶子的纹理，也足够构成一张地图。', 10, timestamp '2023-08-12 10:00:00'),
  ('botanicals', 'https://images.unsplash.com/photo-1501004318641-b39e6451bec6?w=900&q=85', '室内绿意', '植物把房间的呼吸变慢了一点。', 20, timestamp '2023-08-13 10:00:00'),
  ('botanicals', 'https://images.unsplash.com/photo-1497250681960-ef046c08a56e?w=900&q=85', '向光', '所有叶子都在用自己的方式靠近光。', 30, timestamp '2023-08-14 10:00:00')
) as seed(album_slug, image_url, title, description, sort_order, taken_at)
join albums al on al.slug = seed.album_slug
left join files f on f.file_url = seed.image_url and f.deleted_at is null
where not exists (select 1 from album_photos ap where ap.album_id = al.id and ap.image_url = seed.image_url);

insert into moments (content_json, is_publish, publish_time)
select seed.content_json, true, seed.publish_time
from (values
  ('{"text":"今天的咖啡泡得很好，奶泡细腻，在光线里像一片云。窗外下着小雨，不想出门，不想做事，只是坐着，很满足。","images":["https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600&q=80"],"mood":"慵懒"}', timestamp '2024-10-25 10:00:00'),
  ('{"text":"在旧书市场发现一本1973年的《植物图鉴》，手绘插图，纸张已经泛黄，但每一页都是认真的。买了，放在书架上没打算读，只是喜欢它在那里。","images":["https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=600&q=80","https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=600&q=80"],"mood":"满足"}', timestamp '2024-10-20 10:00:00'),
  ('{"text":"今晚散步，路过一家卖花的小店，老板在给白色的花束系丝带，动作很慢，很仔细。我站在外面看了很久，没进去，也没走开。","images":[],"mood":"平静"}', timestamp '2024-10-15 10:00:00'),
  ('{"text":"下午三点，窗外的梧桐树上有只鸟，叫了几声，停了一会儿，又叫。时间就这样过去了二十分钟。我也不知道我在等什么。","images":["https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&q=80"],"mood":"空旷"}', timestamp '2024-10-08 10:00:00'),
  ('{"text":"收拾房间，从抽屉底部翻出一张六年前的电影票。那天的天气，现在完全记不得了。但我记得散场时外面正在下雨，我们没带伞。","images":[],"mood":"怀旧"}', timestamp '2024-09-30 10:00:00'),
  ('{"text":"在公园里拍了一组枯叶的照片，用的旧相机，不知道效果怎么样。但拍的时候很专注，专注到忘记了其他一切事情。这种状态很少有。","images":["https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600&q=80","https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=600&q=80"],"mood":"专注"}', timestamp '2024-09-22 10:00:00')
) as seed(content_json, publish_time)
where not exists (select 1 from moments m where m.content_json = seed.content_json);

insert into guestbook_messages (nickname, content, status, pinned, created_at)
select seed.nickname, seed.content, 'approved', false, seed.created_at
from (values
  ('云上的月亮', '每次看你的文章都有一种被轻轻按住的感觉，谢谢你写这些东西。', timestamp '2024-10-23 10:00:00'),
  ('一只猫', '秋日那篇，读了三遍。最后那句"只是独自持有"，记住了。', timestamp '2024-10-21 10:00:00'),
  ('南风知我意', '我也是那种会在书上折角的人，看到你说"慢读"感觉找到了同类。', timestamp '2024-10-18 10:00:00'),
  ('路过的旅人', '相册里京都的照片让我想起上次去日本，那种安静感是真实的。', timestamp '2024-10-15 10:00:00'),
  ('松柏青青', '博客做得真好，看完好久都不想刷手机了。这是很高的评价。', timestamp '2024-10-10 10:00:00'),
  ('海棠开了', '胶片那篇读了，去找出压箱底的相机。谢谢提醒我还有这个东西。', timestamp '2024-10-05 10:00:00')
) as seed(nickname, content, created_at)
where not exists (
  select 1 from guestbook_messages gm
  where gm.nickname = seed.nickname and gm.content = seed.content and gm.deleted = false
);

insert into friend_types (name, sort_order, is_visible)
select seed.name, seed.sort_order, true
from (values
  ('写作', 10), ('影像', 20), ('旅行', 30), ('生活', 40), ('设计', 50), ('自然', 60)
) as seed(name, sort_order)
where not exists (select 1 from friend_types ft where ft.name = seed.name);

insert into friends (name, url, description, avatar, sort_order, type_id, is_pending, accessible)
select seed.name, seed.url, seed.description, seed.avatar, seed.sort_order, ft.id, false, 1
from (values
  ('字里行间', 'https://example.com/writing', '关于书写与阅读的安静角落', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=80&q=80', '写作', 10),
  ('光影笔记', 'https://example.com/film-notes', '纪录片导演的影像思考', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=80&q=80', '影像', 20),
  ('旅途拾遗', 'https://example.com/travel', '独自旅行的人的自言自语', 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=80&q=80', '旅行', 30),
  ('素日记', 'https://example.com/daily', '记录平凡生活里的小美好', 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=80&q=80', '生活', 40),
  ('设计余白', 'https://example.com/design', '关于设计和审美的个人观察', 'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=80&q=80', '设计', 50),
  ('植物人类学', 'https://example.com/botanicals', '用植物理解人类与自然的关系', 'https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=80&q=80', '自然', 60)
) as seed(name, url, description, avatar, type_name, sort_order)
join friend_types ft on ft.name = seed.type_name
where not exists (select 1 from friends f where f.name = seed.name);
