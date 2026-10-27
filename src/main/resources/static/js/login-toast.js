document.querySelectorAll('[data-login-toast]').forEach((toast) => {
    toast.hidden = false;
    const timer = window.setTimeout(() => toast.remove(), 5000);
    toast.querySelector('[data-dismiss-toast]').addEventListener('click', () => {
        window.clearTimeout(timer);
        toast.remove();
    });
});
