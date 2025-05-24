// 즉시 실행 함수로 감싸서 변수 스코프 보호
(function () {
    // 환경 확인 - Node.js인지 브라우저인지 감지
    if (typeof window === 'undefined' || typeof document === 'undefined') {
        // Node.js 환경 (서버 측) - 필요한 유틸리티 함수만 내보내기
        if (typeof module !== 'undefined' && module.exports) {
            module.exports = {
                formatPrice: function (price) {
                    return Number(price).toLocaleString('ko-KR');
                }
            };
        }
        return; // 이후 브라우저 코드는 실행하지 않음
    }

    // 브라우저 환경 (클라이언트 측)
    // DOM이 로드된 후 초기화 함수 실행
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCreditShop);
    } else {
        initCreditShop(); // 이미 DOM이 로드된 경우 바로 초기화
    }

    // 크레딧 상점 초기화 함수
    function initCreditShop() {
        // 현재 페이지가 credit-shop.html인지 확인 (선택적)
        if (!window.location.pathname.includes('credit-shop.html') &&
            !document.querySelector('.credit-packages')) {
            return; // 크레딧 샵 페이지가 아니면 초기화하지 않음
        }

        // DOM 요소
        const creditPackages = document.querySelectorAll('.credit-package');
        const selectedCreditAmount = document.getElementById('selected-credit-amount');
        const selectedPrice = document.getElementById('selected-price');
        const proceedPaymentBtn = document.getElementById('proceed-payment');

        // 필요한 요소가 존재하지 않으면 초기화하지 않음
        if (!creditPackages.length || !selectedCreditAmount || !selectedPrice || !proceedPaymentBtn) {
            console.log('Credit shop elements not found in the current page');
            return;
        }

        // 선택된 패키지 정보
        let selectedPackage = null;

        // 각 패키지에 클릭 이벤트 리스너 추가
        creditPackages.forEach(clicked_package => {
            clicked_package.addEventListener('click', () => {
                // 이전에 선택된 패키지의 선택 상태 제거
                if (selectedPackage) {
                    selectedPackage.classList.remove('selected');
                }

                // 현재 패키지 선택 상태로 설정
                clicked_package.classList.add('selected');
                selectedPackage = clicked_package;

                // 선택된 정보 업데이트
                const creditAmount = clicked_package.getAttribute('data-credit');
                const price = clicked_package.getAttribute('data-price');

                selectedCreditAmount.textContent = creditAmount;
                selectedPrice.textContent = formatPrice(price);

                // 결제 버튼 활성화
                proceedPaymentBtn.removeAttribute('disabled');
            });
        });

        // 결제 진행 버튼 클릭 이벤트
        proceedPaymentBtn.addEventListener('click', () => {
            if (!selectedPackage) return;

            const creditAmount = selectedPackage.getAttribute('data-credit');
            const price = selectedPackage.getAttribute('data-price');

            // 선택된 크레딧 정보를 세션 스토리지에 저장
            try {
                sessionStorage.setItem('selectedCredit', creditAmount);
                sessionStorage.setItem('selectedPrice', price);
                sessionStorage.setItem('productName', `${creditAmount} 크레딧`);

                // 결제 페이지로 이동
                window.location.href = '/payment.html';
            } catch (e) {
                console.error('세션 스토리지 저장 오류:', e);
                alert('설정 저장 중 오류가 발생했습니다. 브라우저 설정을 확인해주세요.');
            }
        });

        // 초기 인기 패키지를 자동 선택
        autoSelectPopularPackage();
    }

    // 가격 포맷팅 함수 (1000 -> 1,000)
    function formatPrice(price) {
        return Number(price).toLocaleString('ko-KR');
    }

    // 초기 인기 패키지를 자동 선택
    function autoSelectPopularPackage() {
        const popularPackage = document.querySelector('.credit-package.popular');
        if (popularPackage) {
            popularPackage.click();
        }
    }
})();
