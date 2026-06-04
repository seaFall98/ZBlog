insert into settings (group_name, key_name, value_text)
select 'blog', 'blog.barrage_background_image', ''
where not exists (
  select 1 from settings where group_name = 'blog' and key_name = 'blog.barrage_background_image'
);
