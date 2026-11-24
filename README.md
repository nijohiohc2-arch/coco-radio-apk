
# 재희미용실 원장님 라디오 - 코코 APK

## 이게 뭔가요?
사제 안드로이드 네비(안드로이드 5~14)용 APK 프로젝트입니다.
- 시동 켜면 자동 실행 (BOOT_COMPLETED 리시버)
- WebView가 크림·복숭아 PWA(index.html)를 로드
- 지도 앱으로 넘어가도 소리 계속 (Foreground Service)
- KBS는 API에서 HLS 주소 실시간 해석: https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/{code}
  코드 21=1라디오, 22=해피FM, 23=3라디오, 24=클래식FM, 25=쿨FM, 26=한민족

## 빌드 방법 (PC에 Android Studio 있으면 1분)

1. Android Studio > Open > 이 폴더(coco-radio-apk) 열기
2. File > Sync Project
3. Build > Build Bundle(s) / APK(s) > Build APK(s)
   설치용: app/build/outputs/apk/release/app-release.apk (coco-radio-v2)
4. APK를 USB로 네비에 복사

## 네비 설치

1. 네비 설정 > 보안 > 출처 불명 앱 허용 ON
2. 파일 관리자에서 coco-radio-v2.apk (또는 app-release.apk) 설치
   예전에 깐 버전이 있으면 먼저 지운 뒤 설치하세요 (서명 키가 바뀌었습니다).
3. 네비 설정 > 부팅 시 자동실행 / Autostart 앱에 "재희미용실 라디오" 추가
4. 네비 WiFi 또는 테더링으로 인터넷 연결 (방송은 인터넷 필요)

## 휴대폰은 PWA로
휴대폰에서는 APK 말고 PWA가 더 편합니다. 
index.html을 그냥 브라우저로 열고 "홈 화면에 추가" 하면 됩니다. 차 블루투스로 연결.

## 문제 해결
- 방송이 안 나와요: 네비 인터넷 확인, YTN/TBS/CBS는 바로 되는데 KBS/MBC/SBS는 토큰 때문에 브라우저보다 APK에서 더 잘 됩니다. WebView UA에 CocoRadio 추가되어 있음.
- 소리 끊김: 네비 절전 모드 해제, 백그라운드 제한 없음으로 설정.

문의: 재희미용실 원장님 라디오

## v2 참고
- 안드로이드 14: FOREGROUND_SERVICE_MEDIA_PLAYBACK / mediaPlayback 타입, 알림 권한(13+) 요청.
- 부팅 자동실행: 코드는 들어 있지만 안드로이드 10 이상에서는 시스템이 막아 안 될 수 있습니다. 네비 설정의 자동실행 목록에 앱을 넣어 주세요.
- 디버그/릴리스 APK는 같은 키로 서명되어 서로 덮어씌워 설치됩니다.

## v3 참고
- 설치 패키지명(applicationId)은 `com.jaehui.cocoradio2` 입니다. 예전 v1(`com.jaehui.cocoradio`)과 서명이 달라도 같이 설치됩니다.
- 코드 패키지/namespace는 `com.jaehui.cocoradio` 그대로입니다.
- 홈 화면 이름은 "재희미용실 라디오 2" 입니다. 설치용 파일은 coco-radio-v3.apk 입니다.

## v4 참고
- 재생은 WebView가 아니라 ExoPlayer(서비스)가 담당합니다. 구형 네비 WebView/CORS/HLS.js 문제를 피하기 위함입니다.
- KBS·MBC·SBS 주소는 앱이 방송사 API에서 직접 받아 옵니다 (Origin 헤더 없음, 방송사별 User-Agent/Referer).
- 시동/ACC(BOOT_COMPLETED, QUICKBOOT_POWERON, ACC_ON) 때 마지막 방송을 서비스가 바로 재생합니다. 안드로이드 10 이상에서는 화면이 안 뜰 수 있어도 소리는 나야 합니다.
- 패키지명은 `com.jaehui.cocoradio2`, 설치 파일은 coco-radio-v4.apk 입니다.

## v5 참고
- 메인 화면 위에 색 테마 선택 칸(동그라미 8개)이 있습니다. 피치 크림, 민트, 라벤더, 스카이블루, 로즈, 샴페인골드, 세이지, 나이트(야간).
- 선택은 기기에 저장되어 다음에 앱을 켜도 유지됩니다. 재생 방식(ExoPlayer)은 v4와 같습니다.
- 설치 파일은 coco-radio-v5.apk, 패키지명은 `com.jaehui.cocoradio2` 입니다.
