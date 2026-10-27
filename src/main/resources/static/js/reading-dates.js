document.querySelectorAll('[data-reading-dates]').forEach((picker) => {
    const rows = picker.querySelector('[data-date-rows]');
    const addButton = picker.querySelector('[data-add-date]');
    function refresh() {
        const dateRows = [...rows.querySelectorAll('.reading-date-row')];
        addButton.hidden = dateRows.length >= 5;
        dateRows.forEach((row, index) => {
            row.querySelector('input').setAttribute('aria-label', `Ngày đọc ${index + 1}`);
            row.querySelector('[data-remove-date]').hidden = dateRows.length === 1;
        });
    }
    addButton.addEventListener('click', () => {
        if (rows.children.length >= 5) return;
        const row = rows.firstElementChild.cloneNode(true);
        const input = row.querySelector('input');
        input.value = '';
        rows.append(row);
        refresh();
        input.focus();
    });
    rows.addEventListener('click', (event) => {
        const button = event.target.closest('[data-remove-date]');
        if (!button || rows.children.length <= 1) return;
        const row = button.closest('.reading-date-row');
        const nextInput = row.nextElementSibling?.querySelector('input') ?? row.previousElementSibling?.querySelector('input');
        row.remove();
        refresh();
        nextInput?.focus();
    });
    refresh();
});
