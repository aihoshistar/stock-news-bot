(function () {
    const REFRESH_INTERVAL_MS = 30_000;
    const contentArea = document.getElementById('content-area');
    const indicator = document.getElementById('refresh-indicator');

    async function refresh() {
        try {
            const response = await fetch(window.location.href, {
                headers: { 'X-Requested-With': 'Fragment' }
            });
            if (!response.ok) throw new Error('갱신 실패: ' + response.status);

            const html = await response.text();
            const temp = document.createElement('div');
            temp.innerHTML = html.trim();

            // 서버가 fragment의 최상위 div만 보내므로 그대로 교체합니다.
            const newContent = temp.firstElementChild;
            if (newContent) {
                contentArea.replaceWith(newContent);
                newContent.id = 'content-area';
            }

            showIndicator('마지막 갱신: ' + new Date().toLocaleTimeString('ko-KR'));
        } catch (e) {
            showIndicator('갱신 실패 — 재시도 중...');
            console.error(e);
        }
    }

    function showIndicator(text) {
        if (indicator) indicator.textContent = text;
    }

    setInterval(refresh, REFRESH_INTERVAL_MS);
    showIndicator('자동 갱신 30초 간격');
})();