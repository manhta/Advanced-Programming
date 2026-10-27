document.querySelectorAll('.site-header').forEach((header) => {
    const toggle = header.querySelector('[data-nav-toggle]');
    if (!toggle) return;
    header.classList.add('portal-enhanced');
    const setOpen = (open) => {
        header.classList.toggle('is-menu-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Đóng menu điều hướng' : 'Mở menu điều hướng');
    };
    toggle.addEventListener('click', () => setOpen(!header.classList.contains('is-menu-open')));
    const accountMenu = header.querySelector('[data-account-menu]');
    header.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        if (accountMenu?.open) {
            accountMenu.open = false;
            accountMenu.querySelector('summary').focus();
        } else { setOpen(false); toggle.focus(); }
    });
    document.addEventListener('click', (event) => {
        if (!header.contains(event.target)) setOpen(false);
        if (accountMenu?.open && !accountMenu.contains(event.target)) accountMenu.open = false;
    });
    header.querySelectorAll('nav a').forEach((link) => {
        const path = new URL(link.href).pathname;
        if (path === window.location.pathname || (path.endsWith('/home') && window.location.pathname.endsWith('/home'))) {
            link.classList.add('nav-active'); link.setAttribute('aria-current', 'page');
        }
    });
});
