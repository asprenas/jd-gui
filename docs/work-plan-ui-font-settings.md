# 개발 계획: UI 및 디컴파일 영역 글꼴 설정

작성일: 2026-10-26 · 기준 브랜치: `master`

## 1. 목적

사용자가 설치한 글꼴과 화면 환경에 맞춰 JD-GUI 글꼴을 설정할 수 있도록 한다. 글꼴 이름과 크기를 다음 세 영역별로 지정하고, 적용 가능한 글꼴이 운영체제마다 다른 점을 고려해 기본값과 대체 동작을 제공한다.

1. **Java 소스**: `.class`에서 디컴파일해 보여 주는 Java 코드
2. **기타 디컴파일/텍스트**: XML, JSON, Manifest, module-info 등 Java 소스 이외의 텍스트 페이지 및 기타 디컴파일 출력
3. **UI**: 메뉴, 트리, 탭, 대화상자, 환경 설정 등 Swing UI

## 2. 현재 구조

- `ViewerPreferencesProvider`가 `Viewer > Appearance` 환경 설정을 제공한다. 현재는 `ViewerPreferences.fontSize` 한 개만 입력받는다(2~40).
- `AbstractTextPage`가 공통 `RSyntaxTextArea`를 만들고 테마를 적용한다. `preferencesChanged()`에서 공통 글꼴 크기를 적용하며, Ctrl/Meta+마우스 휠로 크기를 바꾸면 해당 크기를 환경 설정에 저장한다.
- `JavaFilePage`와 `ClassFilePage`는 공통 텍스트 페이지를 상속한다. XML 등 기타 텍스트 페이지도 같은 계층을 사용한다.
- `CustomLineNumbersPage`는 줄 번호 글꼴 크기를 텍스트 영역에 맞추지만 글꼴 종류는 별도로 설정하지 않는다.
- UI는 Swing Look and Feel의 `UIManager` 기본 글꼴을 사용한다. 설정 변경 후 전체 UI에 글꼴을 다시 적용하는 공통 경로는 확인되지 않았다.
- 설정값은 `Map<String, String>`을 통해 저장되고 `PreferencesChangeListener`로 열린 페이지에 전달된다.

## 3. 목표 및 제외 범위

### 목표

- 세 영역 각각에 글꼴 종류와 글꼴 크기를 설정할 수 있다.
- 현재 OS/JVM에서 사용할 수 있는 폰트를 선택지로 제공하고, 설정값을 사용자 환경설정에 저장한다.
- 비어 있거나 사용할 수 없는 폰트 이름이 저장되어도 안전한 기본 폰트로 동작한다.
- 환경 설정 적용 시 열린 텍스트 페이지와 UI에 설정을 반영한다.
- Ctrl/Meta+휠 크기 변경을 Java 소스와 기타 텍스트 설정에 맞게 처리한다.

### 제외 범위

- 글꼴 색상, 굵기, 테마 색상, syntax highlighting 사용자화
- 글꼴 파일 설치·다운로드 및 OS 폰트 설정 변경
- 문서 저장/내보내기 결과의 글꼴 변경 (환경 설정은 화면 표시 전용)

## 4. 제안 설계

### 설정 화면

`Viewer > Appearance`를 세 영역으로 확장한다. 각 영역에 글꼴 선택과 크기 선택을 둔다.

| 영역 | 글꼴 종류 | 글꼴 크기 |
|---|---|---|
| Java source | 사용 가능한 폰트 콤보박스 | 2~40 |
| Other text | 사용 가능한 폰트 콤보박스 | 2~40 |
| UI | 사용 가능한 폰트 콤보박스 | UI 배율과 충돌이 없도록 별도 범위 결정 |

폰트 콤보박스는 현재 Graphics Environment가 반환하는 설치 폰트에서 구성한다. Java AWT의 `GraphicsEnvironment.getAvailableFontFamilyNames()`로 현재 Java 환경에서 사용할 수 있는 family 목록을 얻을 수 있다. 이름을 직접 입력할 수 있게 할지는 구현 전에 선택한다. 이름 입력을 허용한다면 미설치 이름은 검증 오류 대신 fallback으로 처리한다.

### 기본값 및 OS 차이

- 사용자 설정이 없으면 Java 코드와 기타 텍스트에 각각 현재 RSyntaxTextArea 테마의 글꼴/크기를 기본값으로 사용한다.
- 표준 Java API에는 Windows/Linux/macOS의 네이티브 기본 글꼴 family를 동일한 방식으로 직접 조회하는 이식성 있는 메서드가 없다. UI의 기본값은 OS 기본 Look and Feel 자체가 아니라, 앱이 Look and Feel을 설정한 뒤 `UIManager.getFont("Label.font")` 등에서 읽은 Swing 기본 글꼴을 사용한다. 이는 Look and Feel이 제공하는 기본값이며 OS의 네이티브 글꼴과 항상 같다고 보장하지 않는다.
- 코드/기타 텍스트의 기본값은 현재 테마의 글꼴을 우선한다. 테마 글꼴을 쓸 수 없을 때는 Java 논리 글꼴(`Monospaced`)을 사용한다. UI는 현재 LAF의 기본 글꼴을 우선하고, 얻지 못할 때 `Dialog` 논리 글꼴을 사용한다. 논리 글꼴은 각 Java 구현이 실제 환경에 매핑하므로 특정 OS family 설치를 가정하지 않는다.
- 설정 저장은 폰트 패밀리명과 크기 값으로 한다. 폰트 파일 경로나 OS 전용 식별자는 저장하지 않는다.

### 적용 범위

- Java 소스 설정은 `JavaFilePage`와 `.class` 디컴파일을 표시하는 `ClassFilePage`에 적용한다.
- 기타 텍스트 설정은 Java 페이지 이외의 `AbstractTextPage` 하위 페이지에 적용한다. Java와 기타 텍스트가 명확히 구분되도록 `AbstractTextPage`에 역할/설정 키 선택 지점을 둔다.
- 글꼴 종류와 크기를 `RSyntaxTextArea`에 적용하고, gutter/줄 번호와 스크롤 영역도 같은 글꼴 family 및 크기에 맞춘다.
- UI 설정은 저장 후 `UIManager` defaults와 현재 열려 있는 component tree를 갱신한다. Look and Feel의 기본 폰트를 일괄 변경하는 범위와 재시작 필요성은 구현 시 검증한다.
- 설정 변경 알림은 기존 `PreferencesChangeListener` 경로를 재사용한다. UI 전체 갱신은 적절한 상위 창/컨트롤러에서 수행한다.

## 5. 구현 단계

### 단계 1: 설정 모델 및 기본값

- 기존 `ViewerPreferences.fontSize` 사용처와 마이그레이션 동작을 정리한다.
- 영역별 키를 정의한다. 예: `ViewerPreferences.javaFontFamily`, `javaFontSize`, `otherFontFamily`, `otherFontSize`, `uiFontFamily`, `uiFontSize`.
- 키가 없는 기존 설정 파일은 현재 테마와 Look and Feel 값으로 자연스럽게 동작하도록 기본값을 제공한다.
- 사용 가능한 폰트 열거와 폰트 생성/fallback 로직을 한 곳에 둔다.

### 단계 2: 환경 설정 UI

- `ViewerPreferencesProvider`를 세 영역 설정 UI로 확장한다.
- 현재 사용 가능한 폰트 목록을 로드하고 각 영역의 값을 표시한다.
- 크기 범위와 잘못된 값 검증을 제공하고, 유효하지 않은 설정에서는 기존 Preferences 확인 동작대로 저장을 막는다.
- 기존 `fontSize` 키와의 호환 또는 제거 시점을 결정하고 이전 값을 새 Java/기타 텍스트 크기로 승계한다.

### 단계 3: 텍스트 페이지 적용

- `AbstractTextPage`의 설정 적용을 Java/기타 텍스트별로 분기한다.
- 폰트 family와 크기를 합쳐 적용하고, 줄 번호 및 gutter에 반영한다.
- 새 페이지 생성 시 현재 설정을 적용하고, 이미 열린 페이지는 환경 설정 저장 직후 갱신한다.
- Ctrl/Meta+휠 변경이 현재 페이지 영역의 크기 키만 갱신하는지 정의하고 일관되게 동작시킨다. 제안 기본은 현재 페이지 범주의 크기 설정을 갱신해 같은 범주 페이지에 유지 적용하는 것이다.

### 단계 4: UI 전체 적용

- 저장된 UI 글꼴 설정으로 Swing 기본 글꼴을 구성한다.
- 현재 프레임과 자식 컴포넌트의 글꼴을 갱신하며, 버튼·아이콘·레이아웃이 깨지지 않는지 확인한다.
- 동적 적용이 Look and Feel 또는 플랫폼에서 안정적이지 않으면 UI 설정만 재시작 적용으로 제한하고 환경 설정에 이를 표시한다.

### 단계 5: 검증 및 문서화

- 기존 설정 파일과 새 설정 파일을 모두 읽는지 확인한다.
- 세 범주의 글꼴 종류/크기, 페이지 전환, 환경 설정 재개방, 재시작 후 유지, 폰트 누락 fallback을 확인한다.
- Windows, Linux, macOS에서 설치 폰트 목록과 기본값 선택을 확인한다.
- HiDPI, Look and Feel, 긴 폰트 이름, 비라틴 문자 표시를 점검한다.
- README 또는 사용자 문서에 환경 설정 사용법을 반영한다.

## 6. 주요 파일 후보

- `services/src/main/java/org/jd/gui/service/preferencespanel/ViewerPreferencesProvider.java`
- `services/src/main/java/org/jd/gui/view/component/AbstractTextPage.java`
- `services/src/main/java/org/jd/gui/view/component/JavaFilePage.java`
- `services/src/main/java/org/jd/gui/view/component/ClassFilePage.java`
- `services/src/main/java/org/jd/gui/view/component/CustomLineNumbersPage.java`
- `app/src/main/java/org/jd/gui/view/PreferencesView.java` 및 설정 변경 전달 경로 (`MainView`, `MainController`)
- 폰트 설정/해석 helper를 둘 적절한 공통 패키지

실제 변경 파일은 적용 흐름을 확인한 뒤 최소화한다.

## 7. 검증 기준

- 각 영역에서 폰트 종류와 크기를 독립적으로 설정하고 저장할 수 있다.
- Java 소스 페이지와 기타 텍스트 페이지가 각자 올바른 설정을 사용한다.
- 줄 번호의 family/크기가 본문과 설정에 맞는다.
- UI 설정은 지정된 적용 시점(즉시 또는 재시작 후)에 메뉴, 트리, 탭, 대화상자에 반영된다.
- 설치되지 않은 폰트 또는 오래된 설정값으로 앱이 실패하지 않고 fallback한다.
- 이전 환경 설정 파일을 읽어도 설정 손실이나 시작 오류가 없다.
- Windows, Linux, macOS 각각에서 OS 전용 폰트가 없어도 정상 동작한다.

## 8. 설계 시 결정할 항목

- 글꼴 선택을 설치 폰트 목록 전용으로 제한할지, 임의 family 이름 입력도 허용할지
- UI 폰트 크기 범위와 즉시 적용/재시작 적용 정책
- Ctrl/Meta+휠이 현재 영역 크기 설정을 갱신할지, 일회성 페이지 크기만 바꿀지
- 구형 `ViewerPreferences.fontSize` 값을 Java/기타 영역 모두에 복사할지, Java 영역에만 승계할지
- OS별 기본 폰트 후보 우선순위 (사용 가능한 폰트 조회에 기반하며 특정 폰트 설치를 가정하지 않음)
