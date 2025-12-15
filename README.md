# JvmBaekjoon

> IntelliJ IDEA 플러그인으로 백준(BOJ) 문제를 IDE에서 직접 풀고 테스트할 수 있습니다.

[![Version](https://img.shields.io/badge/version-0.1.0--beta-blue.svg)](https://github.com/yourusername/JvmBaekjoon/releases)
[![IntelliJ](https://img.shields.io/badge/IntelliJ-2025.3-purple.svg)](https://www.jetbrains.com/idea/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

## 📋 소개

JvmBaekjoon은 백준 온라인 저지(BOJ) 문제를 IntelliJ IDEA에서 편리하게 풀 수 있도록 도와주는 플러그인입니다.

## ✨ 주요 기능

### 🔍 문제 검색
- **문제 번호** 또는 **문제 제목**으로 검색 가능
- 실시간 검색 결과 표시
- 검색한 문제 정보 자동 캐싱

### 📝 문제 정보
- 문제 제목, 설명, 제한 조건 표시
- **이미지 자동 다운로드 및 표시** (문제 본문의 이미지 지원)
- **LaTeX 수식 자동 변환** (수학 공식을 읽기 쉽게 표시)
- 입출력 예제 자동 파싱
- **예제 설명 표시** (예제에 추가 설명이 있는 경우)
- **제한 섹션 표시** (별도 제한 조건이 있는 문제 지원)
- 시간/메모리 제한 확인

### ▶️ 테스트 실행
- **모든 테스트 실행**: 전체 예제를 한 번에 실행 (⌘⇧J / Ctrl+Shift+J)
- **개별 테스트 실행**: 원하는 예제만 선택해서 실행
- 10초 시간 제한 적용
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

## 🚀 설치 방법

### 방법 1: GitHub Release에서 다운로드 (권장)

1. [Releases 페이지](https://github.com/wooongyee/JvmBaekjoon/releases)에서 최신 버전의 `.zip` 파일 다운로드
2. IntelliJ IDEA 실행
3. `File` → `Settings` (macOS: `Preferences`) → `Plugins`
4. 톱니바퀴 아이콘 ⚙️ → `Install Plugin from Disk...`
5. 다운로드한 `.zip` 파일 선택
6. IntelliJ 재시작

### 방법 2: 소스코드에서 빌드

```bash
git clone https://github.com/yourusername/JvmBaekjoon.git
cd JvmBaekjoon
./gradlew buildPlugin
```

빌드된 플러그인은 `build/distributions/` 디렉토리에 생성됩니다.

## 📖 사용 방법

### 1. Tool Window 열기

- **View** → **Tool Windows** → **JvmBaekjoon**
- 또는 우측 사이드바에서 **JvmBaekjoon** 탭 클릭

### 2. 문제 검색

- 문제 번호 또는 제목 입력 후 검색
- 예시: `"1000"` 또는 `"A+B"`
- 검색 결과 목록에서 문제 선택

### 3. 코드 작성 및 테스트

```kotlin
// 예시: Kotlin으로 1000번 문제 풀기
fun main() = with(System.`in`.bufferedReader()) {
    val (a, b) = readLine().split(" ").map { it.toInt() }
    println(a + b)
}
```

- 파일에서 우클릭 → **"Run JvmBaekjoon Test"** 선택
- 또는 에디터에서 **⌘⇧J** (Mac) / **Ctrl+Shift+J** (Windows/Linux)

### 4. 결과 확인

- Run Tool Window에서 각 테스트케이스의 결과 확인
- ✅ 정답: 예상 출력과 일치
- ❌ 오답: 실제 출력 표시
- ⏱️ 시간 초과: 10초 이상 실행

### 5. 백준 제출

- Tool Window 하단의 **Submit** 버튼 클릭
- 백준 제출 페이지로 이동하면 코드가 자동으로 복사됨
- 붙여넣기(Ctrl+V / ⌘V) 후 제출하면 완료

## ⚙️ 설정

### 컴파일러 경로 설정 (특정 버전 사용 시)

특정 Kotlin 또는 Java 버전을 사용하려면 컴파일러 경로를 직접 설정하세요.

`File` → `Settings` → `Tools` → `JvmBaekjoon`

#### JDK Path 설정
```bash
# 예시 (macOS)
/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# 확인 방법
echo $JAVA_HOME
```

#### Kotlin Compiler Path 설정
```bash
# 확인 방법
which kotlinc

# 일반적인 경로
/usr/local/bin/kotlinc         # Homebrew (Intel Mac)
/opt/homebrew/bin/kotlinc      # Homebrew (M1/M2 Mac)
```

> 💡 **Tip**: 이 설정을 하면 IntelliJ의 기본 컴파일러 대신 직접 컴파일러를 호출하여 더 안정적으로 동작합니다.

## 🔧 지원 환경

- **IntelliJ IDEA**: 2025.3 (테스트 완료) / 2023.1 이상 (예상 호환)
  - Community Edition ✅
  - Ultimate Edition ✅
- **언어**: Kotlin, Java
- **OS**: macOS, Windows, Linux

## 🎨 추가 기능

### 이미지 및 수식 지원
- **이미지 자동 다운로드**: 문제 본문의 이미지를 로컬에 캐싱하여 표시
- **LaTeX 수식 변환**: 수학 공식을 유니코드 기호로 변환하여 가독성 향상
- **예제 설명**: 예제에 추가 설명이 있으면 자동으로 표시
- **제한 섹션**: 별도 제한 조건이 있는 문제도 올바르게 표시

## 🐛 알려진 제한사항

- **실수(float/double) 출력 비교**: 현재는 문자열 정확 일치만 판정 (실수 오차 허용 기능은 추후 추가 예정)
- **IntelliJ 버전**: 2025.3에서 테스트 완료 (다른 버전에서 문제 발생 시 이슈 등록 부탁드립니다)
- **복잡한 LaTeX 수식**: 일부 복잡한 수식은 완벽하게 변환되지 않을 수 있습니다

## 📝 라이선스

MIT License - 자세한 내용은 [LICENSE](LICENSE) 파일 참조

## 🤝 기여하기

이슈 제보와 Pull Request를 환영합니다!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📧 문의

문제가 발생하거나 제안사항이 있으시면 [Issues](https://github.com/yourusername/JvmBaekjoon/issues)에 등록해주세요.

---
