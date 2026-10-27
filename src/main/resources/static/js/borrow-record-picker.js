document.querySelectorAll('[data-borrow-picker]').forEach((picker) => {
    const dialog = picker.querySelector('[data-record-dialog]');
    const selectedList = picker.querySelector('[data-selected-records]');
    const results = picker.querySelector('[data-record-results]');
    const search = picker.querySelector('[data-record-search]');
    const status = picker.querySelector('[data-record-status]');
    const error = picker.querySelector('[data-picker-error]');
    const openButton = picker.querySelector('[data-open-picker]');
    const previous = picker.querySelector('[data-record-prev]');
    const next = picker.querySelector('[data-record-next]');
    const selected = new Map();
    const expanded = new Set();
    selectedList.querySelectorAll('[data-selected-record]').forEach((row) => selected.set(row.dataset.id,
        { id: row.dataset.id, archiveNumber: row.dataset.archive, patientName: row.dataset.patient, deceased: row.dataset.deceased === 'true' }));
    let page = 0, sequence = 0, timer;
    const form = picker.closest('form');
    const purpose = form.querySelector('[data-borrow-purpose]');
    function reasonRequired() { return selected.size > 5 || [...selected.values()].some((record) => record.deceased); }
    function updateSummary() {
        const count = form.querySelector('[data-summary-count]');
        const reason = form.querySelector('[data-summary-reason]');
        const warning = form.querySelector('[data-reason-warning]');
        if (count) count.textContent = selected.size;
        if (reason) reason.textContent = purpose.value.trim() || (reasonRequired() ? 'Chưa nhập (bắt buộc)' : 'Không bắt buộc');
        if (warning) warning.hidden = !reasonRequired();
    }
    function renderSelection() {
        selectedList.replaceChildren();
        for (const record of selected.values()) {
            const row = document.createElement('li');
            if (record.deceased) row.classList.add('borrow-deceased');
            const input = document.createElement('input');
            input.type = 'hidden'; input.name = 'recordIds'; input.value = record.id;
            const label = document.createElement('span');
            label.textContent = `${record.archiveNumber} · ${record.patientName}${record.deceased ? ' · Hồ sơ tử vong' : ''}`;
            const remove = document.createElement('button');
            remove.type = 'button'; remove.className = 'remove-selection'; remove.textContent = '×';
            remove.setAttribute('aria-label', `Bỏ chọn hồ sơ ${record.archiveNumber}`);
            remove.addEventListener('click', () => { selected.delete(record.id); expanded.delete(record.id); renderSelection(); openButton.focus(); });
            const details = document.createElement('details'); details.className = 'borrow-record-details';
            const summary = document.createElement('summary'); summary.textContent = 'Thông tin hồ sơ';
            const content = document.createElement('div'); details.append(summary, content);
            let loaded = false, loading = false;
            details.addEventListener('toggle', async () => {
                if (!details.open) { expanded.delete(record.id); return; }
                expanded.add(record.id);
                if (loaded || loading) return;
                loading = true; content.textContent = 'Đang tải thông tin…';
                try {
                    const url = new URL(`${picker.dataset.searchUrl}/${encodeURIComponent(record.id)}`, window.location.href);
                    const response = await fetch(url, { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
                    if (!response.ok || !(response.headers.get('content-type') || '').includes('application/json')) throw new Error('Unavailable');
                    const preview = await response.json();
                    if (!details.isConnected) return;
                    const info = document.createElement('dl');
                    for (const [title, value] of [['Số bệnh án', preview.medicalRecordNumber], ['Khoa', preview.departmentName],
                        ['Hồ sơ tử vong', preview.deceased ? 'Có — cả lượt cần Giám đốc duyệt' : 'Không'], ['Loại bệnh án', preview.recordTypeName], ['Ngày vào viện', preview.admissionDate], ['Ngày ra viện', preview.dischargeDate]]) {
                        const term = document.createElement('dt'); term.textContent = title;
                        const description = document.createElement('dd'); description.textContent = value || 'Chưa có';
                        info.append(term, description);
                    }
                    content.replaceChildren(info); loaded = true;
                } catch (_) {
                    content.textContent = 'Hồ sơ không còn khả dụng hoặc phiên đã hết hạn. Bỏ chọn và tải lại danh sách.';
                } finally { loading = false; }
            });
            row.append(input, label, remove, details); selectedList.append(row);
            details.open = expanded.has(record.id);
        }
        picker.querySelector('[data-selected-count]').textContent = selected.size;
        if (!selected.size) {
            const empty = document.createElement('li'); empty.className = 'borrow-selection-empty';
            empty.textContent = 'Chưa chọn hồ sơ. Bấm “Chọn hồ sơ mượn…” để thêm vào phiếu.'; selectedList.append(empty);
        }
        const clear = picker.querySelector('[data-clear-records]');
        if (clear) clear.hidden = selected.size === 0;
        purpose.required = reasonRequired();
        picker.closest('form').querySelector('[data-purpose-hint]').textContent = reasonRequired()
            ? '(bắt buộc: trên 5 hồ sơ hoặc có hồ sơ tử vong)' : '(không bắt buộc: tối đa 5 hồ sơ, không có hồ sơ tử vong)';
        results.querySelectorAll('input[type=checkbox]').forEach((input) => { input.checked = selected.has(input.value); });
        updateSummary();
    }
    async function load(targetPage) {
        const current = ++sequence;
        status.textContent = 'Đang tìm hồ sơ…'; previous.disabled = true; next.disabled = true;
        const url = new URL(picker.dataset.searchUrl, window.location.href);
        url.searchParams.set('q', search.value); url.searchParams.set('page', targetPage);
        try {
            const response = await fetch(url, { headers: { Accept: 'application/json' }, credentials: 'same-origin' });
            if (!response.ok || !(response.headers.get('content-type') || '').includes('application/json')) throw new Error('Unavailable');
            const data = await response.json();
            if (current !== sequence) return;
            page = data.page; results.replaceChildren();
            for (const record of data.items) {
                const label = document.createElement('label'); label.className = record.deceased ? 'borrow-result borrow-deceased' : 'borrow-result';
                const input = document.createElement('input'); input.type = 'checkbox'; input.value = record.id; input.checked = selected.has(record.id);
                input.addEventListener('change', () => {
                    if (input.checked && !selected.has(record.id) && selected.size >= 20) {
                        input.checked = false; error.textContent = status.textContent = 'Không thể chọn thêm hồ sơ trong lần đăng ký này.'; return;
                    }
                    error.textContent = '';
                    if (input.checked) selected.set(record.id, record); else selected.delete(record.id);
                    renderSelection();
                });
                const text = document.createElement('span'); text.textContent = `${record.archiveNumber} · ${record.patientName}${record.deceased ? ' · Hồ sơ tử vong' : ''}`;
                label.append(input, text); results.append(label);
            }
            status.textContent = data.total ? `Tìm thấy ${data.total} hồ sơ.` : 'Không có hồ sơ phù hợp đang khả dụng.';
            picker.querySelector('[data-record-page]').textContent = `Trang ${page + 1}/${Math.max(1, Math.ceil(data.total / data.size))}`;
            previous.disabled = page === 0; next.disabled = (page + 1) * data.size >= data.total;
        } catch (_) {
            if (current !== sequence) return;
            results.replaceChildren(); status.textContent = 'Không tải được danh sách. Hãy tải lại trang hoặc đăng nhập lại.';
        }
    }
    openButton.addEventListener('click', () => { dialog.showModal(); search.focus(); load(0); });
    picker.querySelectorAll('[data-close-picker]').forEach((button) => button.addEventListener('click', () => dialog.close()));
    search.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(() => load(0), 250); });
    search.addEventListener('keydown', (event) => { if (event.key === 'Enter') { event.preventDefault(); clearTimeout(timer); load(0); } });
    previous.addEventListener('click', () => load(page - 1)); next.addEventListener('click', () => load(page + 1));
    picker.querySelector('[data-clear-records]')?.addEventListener('click', () => { selected.clear(); expanded.clear(); error.textContent = ''; renderSelection(); });
    form.querySelector('[data-reset-borrow]')?.addEventListener('click', () => { selected.clear(); expanded.clear(); purpose.value = ''; error.textContent = ''; renderSelection(); openButton.focus(); });
    purpose.addEventListener('input', updateSummary);
    picker.closest('form').addEventListener('submit', (event) => {
        if (!selected.size) { event.preventDefault(); error.textContent = 'Vui lòng chọn ít nhất một hồ sơ.'; openButton.focus(); }
    });
    renderSelection();
});
