insert into menus (type, title, url, icon, sort_order)
select 'navigation', '相册', '/album', 'ri-gallery-line', 6
where not exists (select 1 from menus where type = 'navigation' and url = '/album');

update menus
set sort_order = sort_order + 1
where type = 'navigation'
  and url in ('/message', '/about')
  and sort_order >= 6;
