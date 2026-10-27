document.querySelectorAll('[data-request-review]').forEach((form) => {
    const result = form.querySelector('[data-review-result]');
    const note = form.querySelector('[data-review-note]');
    const label = form.querySelector('[data-review-note-label]');
    const schedule = form.querySelector('[data-review-schedule]');
    const submit = form.querySelector('[data-review-submit]');
    function update() {
        const approval = result.value === 'APPROVED';
        note.required = result.value === 'NEEDS_INFO' || result.value === 'REJECTED';
        const labels = {
            APPROVED: 'Ghi chú phê duyệt (không bắt buộc)',
            PENDING_APPROVAL: 'Nhận xét trình Giám đốc (không bắt buộc)',
            NEEDS_INFO: 'Cần bổ sung',
            REJECTED: 'Lý do hủy / từ chối'
        };
        const buttons = {
            APPROVED: 'Xác nhận phê duyệt và lịch nhận',
            PENDING_APPROVAL: 'Trình Giám đốc phê duyệt',
            NEEDS_INFO: 'Gửi yêu cầu bổ sung',
            REJECTED: 'Xác nhận từ chối'
        };
        label.textContent = labels[result.value];
        submit.textContent = buttons[result.value];
        if (schedule) {
            schedule.hidden = !approval;
            schedule.querySelectorAll('input').forEach((input) => {
                input.disabled = !approval;
                input.required = approval;
            });
        }
    }
    result.addEventListener('change', update);
    update();
});
