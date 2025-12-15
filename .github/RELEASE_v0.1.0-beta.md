# JvmBaekjoon v0.1.0 Beta - Initial Release

백준 온라인 저지(BOJ) 문제를 IntelliJ IDEA에서 직접 풀고 테스트할 수 있는 플러그인의 첫 번째 베타 릴리즈입니다! 🎉

## ✨ 주요 기능

### 🔍 문제 검색
- **문제 번호** 또는 **문제 제목**으로 검색 가능
- 실시간 검색 결과 표시
- 검색한 문제 정보 자동 캐싱

### 📝 문제 정보
- 문제 제목, 설명, 제한 조건 표시
- 입출력 예제 자동 파싱
- 시간/메모리 제한 확인

### ▶️ 테스트 실행
- **모든 테스트 실행**: 전체 예제를 한 번에 실행 (⌘⇧J / Ctrl+Shift+J)
- **개별 테스트 실행**: 원하는 예제만 선택해서 실행
- 2초 시간 제한 적용
- 실행 시간 밀리초 단위 측정

### ✅ 결과 판정
- 예상 출력과 실제 출력 자동 비교
- 정답/오답/시간초과 판정
- trailing whitespace 자동 무시 (공백 차이로 오답 처리되지 않음)
- Run Tool Window에 상세 결과 출력

### 📤 백준 제출
- IDE에서 바로 백준에 코드 제출
- 제출 화면으로 이동 시 **코드 자동 복사** → 붙여넣기만 하면 완료
- 로그인 정보 저장 가능

### 🛠️ 컴파일 설정
- Kotlin 및 Java 지원
- 패키지 구조 완벽 지원
- **특정 Kotlin/Java 버전 사용**을 원하면 설정에서 컴파일러 경로 직접 지정

## 📥 설치 방법

### 1단계: 플러그인 다운로드
아래 Assets에서 `JvmBaekjoon-0.1.0.zip` 파일을 다운로드하세요.

### 2단계: IntelliJ에 설치
1. IntelliJ IDEA 실행
2. **File** → **Settings** (macOS: **Preferences**)
3. 왼쪽 메뉴에서 **Plugins** 선택
4. 톱니바퀴 아이콘 ⚙️ 클릭 → **Install Plugin from Disk...**
5. 다운로드한 zip 파일 선택
6. **OK** 클릭
7. IntelliJ IDEA 재시작

### 3단계: Tool Window 열기
- **View** → **Tool Windows** → **JvmBaekjoon**
- 또는 우측 사이드바에서 **JvmBaekjoon** 탭 클릭

## 🚀 빠른 시작

### 1. 문제 검색
Tool Window에서 검색창에 문제 번호 또는 제목 입력
```
예시: "1000" 또는 "A+B"
```

### 2. 코드 작성
```kotlin
fun main() = with(System.`in`.bufferedReader()) {
    val (a, b) = readLine().split(" ").map { it.toInt() }
    println(a + b)
}
```

### 3. 테스트 실행
- 파일 우클릭 → **"Run JvmBaekjoon Test"**
- 또는 에디터에서 **⌘⇧J** (Mac) / **Ctrl+Shift+J** (Windows/Linux)

### 4. 결과 확인
Run Tool Window에서 실시간 결과 확인
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
예제 1
✅ 정답 (15ms)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3

📊 결과 요약
🎉 All Passed! (2 / 2)
총 실행 시간: 30ms
```

### 5. 백준 제출
Tool Window 하단 **Submit** 버튼 클릭 → 코드 자동 복사됨 → 백준 페이지에서 붙여넣기

## ⚙️ 컴파일러 설정 (특정 버전 사용 시)

특정 Kotlin 또는 Java 버전을 사용하려면 컴파일러 경로를 직접 설정하세요.

### 설정 위치
**File** → **Settings** → **Tools** → **JvmBaekjoon**

### JDK Path 설정
```bash
# 예시 (macOS)
/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# 확인 방법
echo $JAVA_HOME
```

### Kotlin Compiler Path 설정
```bash
# 확인 방법
which kotlinc

# 일반적인 경로
/usr/local/bin/kotlinc         # Homebrew (Intel Mac)
/opt/homebrew/bin/kotlinc      # Homebrew (M1/M2 Mac)
```

## 🔧 지원 환경

### IntelliJ IDEA
- **테스트 완료**: 2025.3
- **예상 호환**: 2023.1 이상
- Community Edition ✅
- Ultimate Edition ✅

### 언어
- Kotlin ✅
- Java ✅

### 운영체제
- macOS ✅
- Windows ✅
- Linux ✅

## ⚠️ 베타 버전 안내

### 알려진 제한사항

1. **실수(float/double) 출력 비교**
   - 현재는 문자열 정확 일치만 판정
   - 실수 오차 허용 기능은 추후 추가 예정

2. **IntelliJ 버전 호환성**
   - 2025.3에서 테스트 완료
   - 다른 버전에서 문제 발생 시 이슈 등록 부탁드립니다

### 피드백 제출

문제 발견 시 [Issues](../../issues)에 다음 정보와 함께 등록해주세요:
- IntelliJ IDEA 버전
- OS 및 버전
- 재현 단계
- 스크린샷 (선택사항)

## 📝 변경 사항 (v0.1.0-beta)

### Added
- BOJ 문제 검색 (문제 번호 및 제목 지원)
- 문제 정보 파싱 및 표시
- 테스트케이스 자동 실행 (전체/개별)
- Trailing whitespace 무시 비교
- 실행 시간 측정 (밀리초 단위)
- BOJ 제출 화면 이동 (코드 자동 복사)
- Tool Window UI
- 단축키 지원 (⌘⇧J / Ctrl+Shift+J)
- 설정 페이지 (컴파일러 경로 설정)
- 패키지 구조 지원
- 문제 정보 캐싱

---

**Full Changelog**: Initial Release

**Made with ❤️ for competitive programmers**
