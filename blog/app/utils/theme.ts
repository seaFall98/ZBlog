export const isDark = ref(false);

if (import.meta.client) {
  const currentTheme = document.documentElement.getAttribute('data-theme');
  isDark.value = currentTheme === 'dark';

  watch(isDark, dark => {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
    localStorage.setItem('theme', dark ? 'dark' : 'light');
  });
}

export const toggleTheme = (): void => {
  isDark.value = !isDark.value;
};
