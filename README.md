
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
3. Build > Build APK(s) > app-debug.apk 생성
   위치: app/build/outputs/apk/debug/app-debug.apk
4. APK를 USB로 네비에 복사

## 네비 설치

1. 네비 설정 > 보안 > 출처 불명 앱 허용 ON
2. 파일 관리자에서 app-debug.apk 설치
3. 네비 설정 > 부팅 시 자동실행 / Autostart 앱에 "재희미용실 라디오" 추가
4. 네비 WiFi 또는 테더링으로 인터넷 연결 (방송은 인터넷 필요)

## 휴대폰은 PWA로
휴대폰에서는 APK 말고 PWA가 더 편합니다. 
index.html을 그냥 브라우저로 열고 "홈 화면에 추가" 하면 됩니다. 차 블루투스로 연결.

## 문제 해결
- 방송이 안 나와요: 네비 인터넷 확인, YTN/TBS/CBS는 바로 되는데 KBS/MBC/SBS는 토큰 때문에 브라우저보다 APK에서 더 잘 됩니다. WebView UA에 CocoRadio 추가되어 있음.
- 소리 끊김: 네비 절전 모드 해제, 백그라운드 제한 없음으로 설정.

문의: 재희미용실 원장님 라디오
