update settings
set value_text = 'INFJ-A'
where group_name = 'blog'
  and key_name = 'blog.about_personality'
  and value_text = 'Calm, curious, and shipping-oriented.';

insert into menus (type, title, url, icon, sort_order)
select 'footer', '内容', '', 'ri-book-open-line', 1
where not exists (select 1 from menus where type = 'footer' and title = '内容' and parent_id is null);

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '归档', '/archive', 'ri-archive-line', 1
from menus p
where p.type = 'footer' and p.title = '内容' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and url = '/archive' and parent_id = p.id);

update menus
set parent_id = (select id from menus where type = 'footer' and title = '内容' and parent_id is null order by id limit 1)
where type = 'footer'
  and parent_id is null
  and url in ('/categories', '/tags')
  and exists (select 1 from menus where type = 'footer' and title = '内容' and parent_id is null);

insert into menus (type, title, url, icon, sort_order)
select 'footer', '站点', '', 'ri-compass-line', 2
where not exists (select 1 from menus where type = 'footer' and title = '站点' and parent_id is null);

update menus
set parent_id = (select id from menus where type = 'footer' and title = '站点' and parent_id is null order by id limit 1)
where type = 'footer'
  and parent_id is null
  and url in ('/statistics', '/friend')
  and exists (select 1 from menus where type = 'footer' and title = '站点' and parent_id is null);

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'footer', p.id, '关于', '/about', 'ri-information-line', 3
from menus p
where p.type = 'footer' and p.title = '站点' and p.parent_id is null
  and not exists (select 1 from menus where type = 'footer' and url = '/about' and parent_id = p.id);

insert into menus (type, title, url, icon, sort_order)
select 'aggregate', '常用入口', '', 'ri-apps-line', 1
where not exists (select 1 from menus where type = 'aggregate' and title = '常用入口' and parent_id is null);

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'aggregate', p.id, '首页', '/', 'ri-home-line', 1
from menus p
where p.type = 'aggregate' and p.title = '常用入口' and p.parent_id is null
  and not exists (select 1 from menus where type = 'aggregate' and url = '/' and parent_id = p.id);

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'aggregate', p.id, '归档', '/archive', 'ri-archive-line', 2
from menus p
where p.type = 'aggregate' and p.title = '常用入口' and p.parent_id is null
  and not exists (select 1 from menus where type = 'aggregate' and url = '/archive' and parent_id = p.id);

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'aggregate', p.id, '分类', '/categories', 'ri-folder-line', 3
from menus p
where p.type = 'aggregate' and p.title = '常用入口' and p.parent_id is null
  and not exists (select 1 from menus where type = 'aggregate' and url = '/categories' and parent_id = p.id);

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'aggregate', p.id, '标签', '/tags', 'ri-price-tag-3-line', 4
from menus p
where p.type = 'aggregate' and p.title = '常用入口' and p.parent_id is null
  and not exists (select 1 from menus where type = 'aggregate' and url = '/tags' and parent_id = p.id);

insert into menus (type, parent_id, title, url, icon, sort_order)
select 'aggregate', p.id, '关于', '/about', 'ri-information-line', 5
from menus p
where p.type = 'aggregate' and p.title = '常用入口' and p.parent_id is null
  and not exists (select 1 from menus where type = 'aggregate' and url = '/about' and parent_id = p.id);
