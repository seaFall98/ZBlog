delete from menus
where type = 'navigation'
  and lower(title) = 'smoke'
  and url = '/smoke';

delete from menus
where type = 'footer'
  and parent_id is not null;

delete from menus
where type = 'footer'
  and parent_id is null;

insert into menus (type, title, url, icon, sort_order)
select 'footer', '导航', '', 'ri-compass-line', 1
where not exists (select 1 from menus where type = 'footer' and title = '导航' and parent_id is null);

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '首页', '/', 'ri-home-line', 1
from menus p
where p.type = 'footer' and p.title = '导航' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/');

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '分类', '/categories', 'ri-folder-line', 2
from menus p
where p.type = 'footer' and p.title = '导航' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/categories');

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '标签', '/tags', 'ri-price-tag-3-line', 3
from menus p
where p.type = 'footer' and p.title = '导航' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/tags');

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '归档', '/archive', 'ri-archive-line', 4
from menus p
where p.type = 'footer' and p.title = '导航' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/archive');

insert into menus (type, title, url, icon, sort_order)
select 'footer', '协议', '', 'ri-shield-check-line', 2
where not exists (select 1 from menus where type = 'footer' and title = '协议' and parent_id is null);

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '隐私政策', '/privacy', 'ri-lock-line', 1
from menus p
where p.type = 'footer' and p.title = '协议' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/privacy');

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, 'Cookies', '/cookies', 'ri-cookie-line', 2
from menus p
where p.type = 'footer' and p.title = '协议' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/cookies');

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '版权声明', '/copyright', 'ri-copyright-line', 3
from menus p
where p.type = 'footer' and p.title = '协议' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/copyright');

insert into menus (type, title, url, icon, sort_order)
select 'footer', '关于', '', 'ri-information-line', 3
where not exists (select 1 from menus where type = 'footer' and title = '关于' and parent_id is null);

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '关于博客', '/about', 'ri-information-line', 1
from menus p
where p.type = 'footer' and p.title = '关于' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/about');

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '留言板', '/message', 'ri-message-3-line', 2
from menus p
where p.type = 'footer' and p.title = '关于' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/message');

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '反馈投诉', '/feedback', 'ri-feedback-line', 3
from menus p
where p.type = 'footer' and p.title = '关于' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/feedback');

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '友链申请', '/friend', 'ri-links-line', 4
from menus p
where p.type = 'footer' and p.title = '关于' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and parent_id = p.id and url = '/friend');
