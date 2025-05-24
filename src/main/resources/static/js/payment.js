document.addEventListener('DOMContentLoaded', () => {
    // DOM 요소
    const paymentForm = document.getElementById('payment-form');
    const cardNumberInput = document.getElementById('card-number');
    const expiryDateInput = document.getElementById('expiry-date');
    const paymentResult = document.getElementById('payment-result');
    const successMessage = document.querySelector('.success-message');
    const errorMessage = document.querySelector('.error-message');
    const paymentIdSpan = document.getElementById('payment-id');
    const errorMessageSpan = document.getElementById('error-message');
    const productNameEl = document.getElementById('product-name');
    const productPriceEl = document.getElementById('product-price');

    // 세션 스토리지에서 선택된 크레딧 정보 가져오기
    const loadSelectedCreditInfo = () => {
        const selectedCredit = sessionStorage.getItem('selectedCredit');
        const selectedPrice = sessionStorage.getItem('selectedPrice');
        const productName = sessionStorage.getItem('productName');

        if (selectedCredit && selectedPrice) {
            // 상품 정보 업데이트
            productNameEl.textContent = productName || `${selectedCredit} 크레딧`;
            productPriceEl.textContent = Number(selectedPrice).toLocaleString('ko-KR');
        } else {
            // 크레딧을 선택하지 않은 경우 기본값 유지 또는 상품 선택 페이지로 리다이렉트
            if (!productNameEl.textContent) {
                alert('구매할 크레딧을 선택해주세요.');
                window.location.href = '/credit-shop.html';
            }
        }
    };

    // 페이지 로드 시 선택된 크레딧 정보 로드
    loadSelectedCreditInfo();

    // 카드번호 입력 포맷팅 (4자리마다 공백 추가)
    cardNumberInput.addEventListener('input', (e) => {
        let value = e.target.value.replace(/\s/g, '');
        let formattedValue = '';
        
        for (let i = 0; i < value.length; i++) {
            if (i > 0 && i % 4 === 0) {
                formattedValue += ' ';
            }
            formattedValue += value[i];
        }
        
        e.target.value = formattedValue;
    });

    // 유효기간 입력 포맷팅 (MM/YY)
    expiryDateInput.addEventListener('input', (e) => {
        let value = e.target.value.replace(/\D/g, '');
        
        if (value.length > 2) {
            value = value.substring(0, 2) + '/' + value.substring(2, 4);
        }
        
        e.target.value = value;
    });

    // 폼 제출 처리
    paymentForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // 폼 유효성 검사
        if (!validateForm()) {
            return;
        }
        
        // 카드 정보 객체 생성
        const cardInfo = {
            cardNumber: cardNumberInput.value.replace(/\s/g, ''),
            expiryDate: expiryDateInput.value,
            cvv: document.getElementById('cvv').value,
            ownerName: document.getElementById('card-owner').value,
            email: document.getElementById('email').value
        };
        
        // 결제 처리 요청
        try {
            const response = await processPayment(cardInfo);
            displayPaymentSuccess(response);
            
            // 결제 성공 시 세션 스토리지 정리
            sessionStorage.removeItem('selectedCredit');
            sessionStorage.removeItem('selectedPrice');
            sessionStorage.removeItem('productName');
        } catch (error) {
            displayPaymentError(error.message || '결제 처리 중 오류가 발생했습니다.');
        }
    });

    // 폼 유효성 검사 함수
    function validateForm() {
        // 카드번호 검사 (16자리 숫자)
        const cardNumber = cardNumberInput.value.replace(/\s/g, '');
        if (!/^\d{16}$/.test(cardNumber)) {
            alert('유효한 카드번호를 입력해주세요 (16자리)');
            return false;
        }
        
        // 유효기간 검사 (MM/YY 형식)
        const expiryDate = expiryDateInput.value;
        if (!/^\d{2}\/\d{2}$/.test(expiryDate)) {
            alert('유효기간을 MM/YY 형식으로 입력해주세요');
            return false;
        }
        
        // 현재 날짜와 유효기간 비교
        const [month, year] = expiryDate.split('/');
        const expiryMonth = parseInt(month, 10);
        const expiryYear = parseInt('20' + year, 10);
        
        const currentDate = new Date();
        const currentMonth = currentDate.getMonth() + 1;
        const currentYear = currentDate.getFullYear();
        
        if (expiryYear < currentYear || (expiryYear === currentYear && expiryMonth < currentMonth)) {
            alert('만료된 카드입니다.');
            return false;
        }
        
        // CVV 검사 (3자리 숫자)
        const cvv = document.getElementById('cvv').value;
        if (!/^\d{3}$/.test(cvv)) {
            alert('CVV는 3자리 숫자로 입력해주세요');
            return false;
        }
        
        return true;
    }

    // 결제 처리 API 호출 함수
    async function processPayment(cardInfo) {
        const response = await fetch('/api/payment/process', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                cardInfo: cardInfo,
                productName: productNameEl.textContent,
                productPrice: productPriceEl.textContent.replace(/,/g, '')
            })
        });
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || '결제 처리 중 오류가 발생했습니다.');
        }
        
        return await response.json();
    }

    // 결제 성공 표시 함수
    function displayPaymentSuccess(response) {
        paymentForm.style.display = 'none';
        paymentResult.classList.remove('hidden');
        successMessage.style.display = 'block';
        paymentIdSpan.textContent = response.paymentId;
    }

    // 결제 오류 표시 함수
    // 브라우저 환경인지 확인
    if (typeof window !== 'undefined' && typeof document !== 'undefined') {
        document.addEventListener('DOMContentLoaded', () => {
            // DOM 요소
            const paymentForm = document.getElementById('payment-form');
            const cardNumberInput = document.getElementById('card-number');
            const expiryDateInput = document.getElementById('expiry-date');
            const paymentResult = document.getElementById('payment-result');
            const successMessage = document.querySelector('.success-message');
            const errorMessage = document.querySelector('.error-message');
            const paymentIdSpan = document.getElementById('payment-id');
            const errorMessageSpan = document.getElementById('error-message');
            const productNameEl = document.getElementById('product-name');
            const productPriceEl = document.getElementById('product-price');
    
            // 세션 스토리지에서 선택된 크레딧 정보 가져오기
            const loadSelectedCreditInfo = () => {
                const selectedCredit = sessionStorage.getItem('selectedCredit');
                const selectedPrice = sessionStorage.getItem('selectedPrice');
                const productName = sessionStorage.getItem('productName');
    
                if (selectedCredit && selectedPrice && productNameEl && productPriceEl) {
                    // 상품 정보 업데이트
                    productNameEl.textContent = productName || `${selectedCredit} 크레딧`;
                    productPriceEl.textContent = Number(selectedPrice).toLocaleString('ko-KR');
                } else {
                    // 크레딧을 선택하지 않은 경우 기본값 유지 또는 상품 선택 페이지로 리다이렉트
                    if (productNameEl && !productNameEl.textContent) {
                        alert('구매할 크레딧을 선택해주세요.');
                        window.location.href = '/credit-shop.html';
                    }
                }
            };
    
            // 페이지 로드 시 선택된 크레딧 정보 로드
            loadSelectedCreditInfo();
    
            // 폼 요소가 존재하는 경우에만 이벤트 리스너 추가
            if (cardNumberInput) {
                // 카드번호 입력 포맷팅 (4자리마다 공백 추가)
                cardNumberInput.addEventListener('input', (e) => {
                    let value = e.target.value.replace(/\s/g, '');
                    let formattedValue = '';
                    
                    for (let i = 0; i < value.length; i++) {
                        if (i > 0 && i % 4 === 0) {
                            formattedValue += ' ';
                        }
                        formattedValue += value[i];
                    }
                    
                    e.target.value = formattedValue;
                });
            }
    
            if (expiryDateInput) {
                // 유효기간 입력 포맷팅 (MM/YY)
                expiryDateInput.addEventListener('input', (e) => {
                    let value = e.target.value.replace(/\D/g, '');
                    
                    if (value.length > 2) {
                        value = value.substring(0, 2) + '/' + value.substring(2, 4);
                    }
                    
                    e.target.value = value;
                });
            }
    
            // 폼 제출 처리
            if (paymentForm) {
                paymentForm.addEventListener('submit', async (e) => {
                    e.preventDefault();
                    
                    // 폼 유효성 검사
                    if (!validateForm()) {
                        return;
                    }
                    
                    // 카드 정보 객체 생성
                    const cardInfo = {
                        cardNumber: cardNumberInput.value.replace(/\s/g, ''),
                        expiryDate: expiryDateInput.value,
                        cvv: document.getElementById('cvv').value,
                        ownerName: document.getElementById('card-owner').value,
                        email: document.getElementById('email').value
                    };
                    
                    // 결제 처리 요청
                    try {
                        const response = await processPayment(cardInfo);
                        displayPaymentSuccess(response);
                        
                        // 결제 성공 시 세션 스토리지 정리
                        sessionStorage.removeItem('selectedCredit');
                        sessionStorage.removeItem('selectedPrice');
                        sessionStorage.removeItem('productName');
                    } catch (error) {
                        displayPaymentError(error.message || '결제 처리 중 오류가 발생했습니다.');
                    }
                });
            }
    
            // 폼 유효성 검사 함수
            function validateForm() {
                if (!cardNumberInput || !expiryDateInput) return false;
                
                // 카드번호 검사 (16자리 숫자)
                const cardNumber = cardNumberInput.value.replace(/\s/g, '');
                if (!/^\d{16}$/.test(cardNumber)) {
                    alert('유효한 카드번호를 입력해주세요 (16자리)');
                    return false;
                }
                
                // 유효기간 검사 (MM/YY 형식)
                const expiryDate = expiryDateInput.value;
                if (!/^\d{2}\/\d{2}$/.test(expiryDate)) {
                    alert('유효기간을 MM/YY 형식으로 입력해주세요');
                    return false;
                }
                
                // 현재 날짜와 유효기간 비교
                const [month, year] = expiryDate.split('/');
                const expiryMonth = parseInt(month, 10);
                const expiryYear = parseInt('20' + year, 10);
                
                const currentDate = new Date();
                const currentMonth = currentDate.getMonth() + 1;
                const currentYear = currentDate.getFullYear();
                
                if (expiryYear < currentYear || (expiryYear === currentYear && expiryMonth < currentMonth)) {
                    alert('만료된 카드입니다.');
                    return false;
                }
                
                // CVV 검사 (3자리 숫자)
                const cvv = document.getElementById('cvv').value;
                if (!/^\d{3}$/.test(cvv)) {
                    alert('CVV는 3자리 숫자로 입력해주세요');
                    return false;
                }
                
                return true;
            }
    
            // 결제 처리 API 호출 함수
            async function processPayment(cardInfo) {
                const response = await fetch('/api/payment/process', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        cardInfo: cardInfo,
                        productName: productNameEl ? productNameEl.textContent : '크레딧',
                        productPrice: productPriceEl ? productPriceEl.textContent.replace(/,/g, '') : '0'
                    })
                });
                
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || '결제 처리 중 오류가 발생했습니다.');
                }
                
                return await response.json();
            }
    
            // 결제 성공 표시 함수
            function displayPaymentSuccess(response) {
                if (paymentForm && paymentResult) {
                    paymentForm.style.display = 'none';
                    paymentResult.classList.remove('hidden');
                    successMessage.style.display = 'block';
                    paymentIdSpan.textContent = response.paymentId;
                }
            }
    
            // 결제 오류 표시 함수
            // 환경 확인 - Node.js인지 브라우저인지 감지
            (function() {
                // Node.js 환경 (서버 측)
                if (typeof window === 'undefined' || typeof document === 'undefined') {
                    // Node.js에서 사용할 유틸리티 함수만 내보내기
                    module.exports = {
                        formatPrice: function(price) {
                            return Number(price).toLocaleString('ko-KR');
                        }
                    };
                    return; // 이후 브라우저 코드는 실행하지 않음
                }
                
                // 브라우저 환경 (클라이언트 측)
                // DOMContentLoaded 이벤트에 대한 리스너 등록
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', initPaymentPage);
                } else {
                    // 이미 DOM이 로드된 경우 바로 초기화
                    initPaymentPage();
                }
                
                // 결제 페이지 초기화 함수
                function initPaymentPage() {
                    // DOM 요소
                    const paymentForm = document.getElementById('payment-form');
                    const cardNumberInput = document.getElementById('card-number');
                    const expiryDateInput = document.getElementById('expiry-date');
                    const paymentResult = document.getElementById('payment-result');
                    const successMessage = document.querySelector('.success-message');
                    const errorMessage = document.querySelector('.error-message');
                    const paymentIdSpan = document.getElementById('payment-id');
                    const errorMessageSpan = document.getElementById('error-message');
                    const productNameEl = document.getElementById('product-name');
                    const productPriceEl = document.getElementById('product-price');
                    
                    // 필요한 요소가 존재하는지 확인
                    if (!paymentForm) {
                        console.log('결제 폼 요소를 찾을 수 없습니다. 현재 결제 페이지가 아닌 것 같습니다.');
                        return;
                    }
                    
                    // 세션 스토리지에서 선택된 크레딧 정보 가져오기
                    loadSelectedCreditInfo();
                    
                    // 카드 입력 필드가 있는 경우에만 이벤트 리스너 추가
                    if (cardNumberInput) {
                        // 카드번호 입력 포맷팅 (4자리마다 공백 추가)
                        cardNumberInput.addEventListener('input', formatCardNumber);
                    }
                    
                    if (expiryDateInput) {
                        // 유효기간 입력 포맷팅 (MM/YY)
                        expiryDateInput.addEventListener('input', formatExpiryDate);
                    }
                    
                    // 폼 제출 이벤트 핸들러 추가
                    paymentForm.addEventListener('submit', handleFormSubmit);
                    
                    // 세션 스토리지에서 선택된 크레딧 정보 가져오기
                    function loadSelectedCreditInfo() {
                        const selectedCredit = sessionStorage.getItem('selectedCredit');
                        const selectedPrice = sessionStorage.getItem('selectedPrice');
                        const productName = sessionStorage.getItem('productName');
                        
                        if (selectedCredit && selectedPrice && productNameEl && productPriceEl) {
                            // 상품 정보 업데이트
                            productNameEl.textContent = productName || `${selectedCredit} 크레딧`;
                            productPriceEl.textContent = formatPrice(selectedPrice);
                        } else if (productNameEl && !productNameEl.textContent && window.location.pathname.includes('payment.html')) {
                            // 크레딧 정보가 없고 결제 페이지인 경우 안내 표시
                            alert('구매할 크레딧을 먼저 선택해주세요.');
                            window.location.href = '/credit-shop.html';
                        }
                    }
                    
                    // 카드번호 포맷팅 함수
                    function formatCardNumber(e) {
                        let value = e.target.value.replace(/\s/g, '');
                        let formattedValue = '';
                        
                        for (let i = 0; i < value.length; i++) {
                            if (i > 0 && i % 4 === 0) {
                                formattedValue += ' ';
                            }
                            formattedValue += value[i];
                        }
                        
                        e.target.value = formattedValue;
                    }
                    
                    // 유효기간 포맷팅 함수
                    function formatExpiryDate(e) {
                        let value = e.target.value.replace(/\D/g, '');
                        
                        if (value.length > 2) {
                            value = value.substring(0, 2) + '/' + value.substring(2, 4);
                        }
                        
                        e.target.value = value;
                    }
                    
                    // 폼 제출 처리 함수
                    async function handleFormSubmit(e) {
                        e.preventDefault();
                        
                        // 폼 유효성 검사
                        if (!validateForm()) {
                            return;
                        }
                        
                        // 카드 정보 객체 생성
                        const cardInfo = {
                            cardNumber: cardNumberInput.value.replace(/\s/g, ''),
                            expiryDate: expiryDateInput.value,
                            cvv: document.getElementById('cvv').value,
                            ownerName: document.getElementById('card-owner').value,
                            email: document.getElementById('email').value
                        };
                        
                        // 결제 처리 요청
                        try {
                            const response = await processPayment(cardInfo);
                            displayPaymentSuccess(response);
                            
                            // 결제 성공 시 세션 스토리지 정리
                            sessionStorage.removeItem('selectedCredit');
                            sessionStorage.removeItem('selectedPrice');
                            sessionStorage.removeItem('productName');
                        } catch (error) {
                            displayPaymentError(error.message || '결제 처리 중 오류가 발생했습니다.');
                        }
                    }
                    
                    // 폼 유효성 검사 함수
                    function validateForm() {
                        if (!cardNumberInput || !expiryDateInput) return false;
                        
                        // 카드번호 검사 (16자리 숫자)
                        const cardNumber = cardNumberInput.value.replace(/\s/g, '');
                        if (!/^\d{16}$/.test(cardNumber)) {
                            alert('유효한 카드번호를 입력해주세요 (16자리)');
                            return false;
                        }
                        
                        // 유효기간 검사 (MM/YY 형식)
                        const expiryDate = expiryDateInput.value;
                        if (!/^\d{2}\/\d{2}$/.test(expiryDate)) {
                            alert('유효기간을 MM/YY 형식으로 입력해주세요');
                            return false;
                        }
                        
                        // 현재 날짜와 유효기간 비교
                        const [month, year] = expiryDate.split('/');
                        const expiryMonth = parseInt(month, 10);
                        const expiryYear = parseInt('20' + year, 10);
                        
                        const currentDate = new Date();
                        const currentMonth = currentDate.getMonth() + 1;
                        const currentYear = currentDate.getFullYear();
                        
                        if (expiryYear < currentYear || (expiryYear === currentYear && expiryMonth < currentMonth)) {
                            alert('만료된 카드입니다.');
                            return false;
                        }
                        
                        // CVV 검사 (3자리 숫자)
                        const cvv = document.getElementById('cvv').value;
                        if (!/^\d{3}$/.test(cvv)) {
                            alert('CVV는 3자리 숫자로 입력해주세요');
                            return false;
                        }
                        
                        return true;
                    }
                    
                    // 결제 처리 API 호출 함수
                    async function processPayment(cardInfo) {
                        const response = await fetch('/api/payment/process', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                cardInfo: cardInfo,
                                productName: productNameEl ? productNameEl.textContent : '크레딧',
                                productPrice: productPriceEl ? productPriceEl.textContent.replace(/,/g, '') : '0'
                            })
                        });
                        
                        if (!response.ok) {
                            const errorData = await response.json();
                            throw new Error(errorData.message || '결제 처리 중 오류가 발생했습니다.');
                        }
                        
                        return await response.json();
                    }
                    
                    // 결제 성공 표시 함수
                    function displayPaymentSuccess(response) {
                        if (paymentForm && paymentResult) {
                            paymentForm.style.display = 'none';
                            paymentResult.classList.remove('hidden');
                            successMessage.style.display = 'block';
                            paymentIdSpan.textContent = response.paymentId;
                        }
                    }
                    
                    // 결제 오류 표시 함수
                    function displayPaymentError(message) {
                        if (paymentResult) {
                            paymentResult.classList.remove('hidden');
                            errorMessage.style.display = 'block';
                            errorMessageSpan.textContent = message;
                        }
                    }
                }
                
                // 가격 포맷팅 함수 (1000 -> 1,000)
                function formatPrice(price) {
                    return Number(price).toLocaleString('ko-KR');
                }
            })();
        });
    }
    
    // Node.js 환경에서 사용하기 위한 함수 내보내기
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = {
            // 서버에서 필요한 함수만 내보내기
            formatPrice: function(price) {
                return Number(price).toLocaleString('ko-KR');
            }
        };
    }
});
