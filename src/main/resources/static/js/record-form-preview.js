document.querySelectorAll('[data-record-entry]').forEach((form) => {
    const page = form.closest('main');
    const defaults = {patientName:'Chưa nhập tên người bệnh',patientCode:'Chưa có',admissionDate:'Chưa có',dischargeDate:'Chưa có'};
    function update() {
        page.querySelectorAll('[data-preview-field]').forEach((target) => {
            const name = target.dataset.previewField;
            const input = form.elements.namedItem(name);
            const value = input instanceof HTMLSelectElement ? input.selectedOptions[0]?.textContent : input?.value.trim();
            target.textContent = value || defaults[name] || 'Chưa xác định';
        });
        const day = form.elements.namedItem('birthDay');
        const month = form.elements.namedItem('birthMonth');
        const year = form.elements.namedItem('birthYear');
        const complete = day.value && month.value && year.value;
        const date = new Date(Number(year.value), Number(month.value) - 1, Number(day.value));
        const valid = complete && date.getFullYear() === Number(year.value) && date.getMonth() + 1 === Number(month.value) && date.getDate() === Number(day.value);
        day.setCustomValidity(complete && !valid ? 'Ngày sinh không hợp lệ. Kiểm tra ngày, tháng và năm nhuận.' : '');
        page.querySelector('[data-preview-birthdate]').textContent = valid
            ? `${day.value.padStart(2,'0')}/${month.value.padStart(2,'0')}/${year.value}`
            : (complete ? 'Ngày sinh không hợp lệ' : 'Chưa nhập đủ ngày sinh');
        const deceased = form.elements.namedItem('deceased').checked;
        page.querySelector('[data-preview-deceased]').textContent = deceased ? 'Có' : 'Không';
        page.querySelector('[data-deceased-warning]').hidden = !deceased;
    }
    form.addEventListener('input', update);
    form.addEventListener('change', update);
    form.addEventListener('reset', () => setTimeout(update, 0));
    update();
});
