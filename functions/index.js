// Firebase Admin SDK 초기화
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Node.js 18 이상에서는 fetch가 내장되어 있어 별도 설치 불필요.
// 만약 Node.js 16 이하를 사용한다면 'node-fetch' 라이브러리를 설치해야 합니다.
// (설치 방법은 아래 3단계에서 설명)
// const fetch = require('node-fetch'); // Node.js 16 이하에서 필요

// Callable Cloud Function 정의
// 클라이언트(Android 앱)에서 호출할 함수입니다.
exports.verifyKakaoTokenAndCreateFirebaseToken = functions.https.onCall(async (data, context) => {
    // 1. 클라이언트로부터 Kakao Access Token 받기
    const kakaoAccessToken = data.accessToken;

    // 토큰 유효성 검사 (기본적인 null/undefined/타입 체크)
    if (!kakaoAccessToken || typeof kakaoAccessToken !== 'string') {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Kakao Access Token (accessToken) is required and must be a string.'
        );
    }

    try {
        // 2. 카카오 사용자 정보 API 호출 (카카오 토큰 유효성 검증 및 사용자 정보 획득)
        // 이 요청을 통해 카카오 서버에 카카오 Access Token이 유효한지 확인하고,
        // 해당 토큰에 연결된 카카오 사용자의 고유 ID와 프로필 정보를 얻습니다.
        const kakaoResponse = await fetch('https://kapi.kakao.com/v2/user/me', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${kakaoAccessToken}`, // 획득한 카카오 Access Token을 Authorization 헤더에 포함
                'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8' // 필요한 경우 추가 (카카오 문서 참조)
            }
        });

        // 카카오 API 응답이 성공적인지 확인 (HTTP 상태 코드 200 OK)
        if (!kakaoResponse.ok) {
            const errorData = await kakaoResponse.json(); // 카카오 API에서 반환한 에러 메시지 파싱
            console.error('Kakao API error details:', errorData); // 디버깅을 위해 에러 상세 정보 로깅
            throw new functions.https.HttpsError(
                'unauthenticated', // Firebase에서 제공하는 표준 에러 코드
                `Failed to verify Kakao token. Kakao API responded with: ${errorData.msg || kakaoResponse.statusText}`
            );
        }

        const kakaoUser = await kakaoResponse.json(); // 카카오 사용자 정보 (JSON 형식) 파싱

        // 카카오 사용자 ID를 Firebase UID로 사용
        // Firebase UID는 문자열이며 최대 128자입니다.
        // `kakao:` 프리픽스를 붙여 다른 인증 방식(이메일/비번, Google 등)의 UID와 충돌을 방지하고
        // 어떤 소셜 계정으로 로그인했는지 식별하기 용이하게 합니다.
        const uid = `kakao:${kakaoUser.id}`;

        // 3. Firebase Custom Token 생성
        // admin.auth().createCustomToken(uid, customClaims) 함수를 사용하여 Custom Token을 생성합니다.
        // `customClaims` 객체는 Firebase 사용자 프로필에 추가할 수 있는 임의의 JSON 데이터입니다.
        // 이메일, 닉네임, 프로필 이미지 등 카카오에서 받은 유용한 정보를 여기에 저장할 수 있습니다.
        // 이 정보는 사용자가 Firebase에 로그인한 후 클라이언트 앱에서 `firebase.User.getIdTokenResult(true)`를 통해 접근할 수 있습니다.
        const firebaseToken = await admin.auth().createCustomToken(uid, {
            // 카카오 계정 정보 (선택 사항이며, 사용자의 카카오 동의 항목에 따라 존재 여부가 다름)
            email: kakaoUser.kakao_account?.email, // '?'는 Optional Chaining으로, kakao_account가 없으면 undefined 반환
            profile_image: kakaoUser.properties?.profile_image,
            nickname: kakaoUser.properties?.nickname,
            // 필요하다면 여기에 추가적인 정보를 Custom Claim으로 더 넣을 수 있습니다.
            // 예: isKakaoUser: true
        });

        // 4. 생성된 Firebase Custom Token을 클라이언트에 반환
        // 클라이언트는 이 토큰을 받아 Firebase에 로그인합니다.
        return { firebaseToken: firebaseToken };

    } catch (error) {
        // Cloud Function 실행 중 발생한 모든 에러를 처리합니다.
        console.error('An error occurred during Kakao token verification or Firebase token creation:', error);

        // 클라이언트에게 Firebase HttpsError 형식으로 에러를 전달합니다.
        if (error instanceof functions.https.HttpsError) {
            throw error; // 이미 Firebase 표준 HttpsError라면 그대로 던짐
        } else {
            // 일반 JavaScript 에러는 'internal' 에러로 변환하여 전달
            throw new functions.https.HttpsError(
                'internal', // Firebase에서 제공하는 일반적인 내부 서버 에러 코드
                'Failed to authenticate with Kakao and Firebase.', // 사용자에게 표시될 일반적인 메시지
                error.message // 개발 단계에서 디버깅을 위해 실제 에러 메시지를 포함
            );
        }
    }
});