// 회원 이외의 서비스에서는 fetch대신 아래의 fetchWithAuth 사용
// AccessToken 만료 시, RefreshToken으로 AccessToken 재발급함.
// 해당 파일에 아래와 같은 순서로 작성해야 사용 가능
// <script src="/js/member/module/service.js"></script>
// <script src="/js/auth/auth.js"></script>

async function fetchWithAuth(url, options = {}) {
    options.credentials = 'include'; // 쿠키 포함

    let response = await fetch(url, options);

    if (response.status === 401) {
        // 401이면 리프레시 토큰으로 토큰 재발급 시도
        try {
            const newAccessToken = await memberService.refresh();
            console.log(newAccessToken);
            // 새 토큰은 쿠키에 담겨서 자동 적용됨 (HttpOnly 쿠키라면)
            // 토큰 재발급 성공하면 다시 원래 요청 시도
            response = await fetch(url, options);
        } catch (refreshError) {
            // 리프레시 토큰도 만료되었거나 재발급 실패
            // 로그아웃 처리 등 필요
            throw refreshError;
        }
    }

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Fetch error");
    }

    return response;
}