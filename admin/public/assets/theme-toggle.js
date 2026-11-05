(() => {
  const preferenceKey = "bulletin-admin-theme";
  const root = document.documentElement;
  const systemTheme = window.matchMedia("(prefers-color-scheme: dark)");

  const getPreference = () => {
    try {
      return window.localStorage.getItem(preferenceKey);
    } catch {
      return null;
    }
  };

  const applyTheme = (theme) => {
    const isDark = theme === "dark";
    root.classList.toggle("dark", isDark);
    root.style.colorScheme = isDark ? "dark" : "light";

    const button = document.getElementById("theme-toggle");
    if (!button) return;

    const label = isDark ? "Switch to light theme" : "Switch to dark theme";
    button.setAttribute("aria-label", label);
    button.setAttribute("aria-pressed", String(isDark));
    button.title = label;
    button.querySelector('[data-theme-icon="sun"]').classList.toggle("hidden", !isDark);
    button.querySelector('[data-theme-icon="moon"]').classList.toggle("hidden", isDark);
  };

  const savedPreference = getPreference();
  applyTheme(savedPreference === "dark" || (savedPreference !== "light" && systemTheme.matches) ? "dark" : "light");

  const connectToggle = () => {
    const button = document.getElementById("theme-toggle");
    if (!button) return;

    button.addEventListener("click", () => {
      const nextTheme = root.classList.contains("dark") ? "light" : "dark";
      try {
        window.localStorage.setItem(preferenceKey, nextTheme);
      } catch {
        // The theme still works for this page when storage is unavailable.
      }
      applyTheme(nextTheme);
    });
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", connectToggle, { once: true });
  } else {
    connectToggle();
  }

  systemTheme.addEventListener("change", (event) => {
    if (!getPreference()) applyTheme(event.matches ? "dark" : "light");
  });
})();
