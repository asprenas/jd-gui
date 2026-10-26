# JD-Core 1.3.0 업데이트 구현 계획

> 이 계획은 단계별로 실행한다. 저장소의 기준 개발 환경은 Java 21과 Gradle이다.

**목표:** JD-GUI의 JD-Core를 1.2.0에서 1.3.0으로 업데이트하고, multi-release JAR 로딩을 화면 표시와 소스 내보내기에 일관되게 적용하며, module-info와 디컴파일 회귀 동작을 검증한다.

**구조:** Maven Local, JitPack, 릴리스 빌드가 공통 `jdCoreVersion`을 사용한다. 아카이브 manifest에 `Multi-Release: true`가 있을 때만 JD-Core의 `MultiReleaseLoader`를 사용한다. 클래스 화면, module-info 화면, 소스 내보내기는 공통 loader factory를 사용한다.

**참고 자료:** JD-Core [v1.2.1 변경 기록](https://github.com/asprenas/jd-core/blob/v1.3.0/.github/release-notes/v1.2.1.md), [v1.3.0 변경 기록](https://github.com/asprenas/jd-core/blob/v1.3.0/.github/release-notes/v1.3.0.md), [`MultiReleaseLoader`](https://github.com/asprenas/jd-core/blob/v1.3.0/src/main/java/org/jd/core/v1/api/loader/MultiReleaseLoader.java).

## 제약 조건

- README와 폰트 설정 등 기존 사용자 변경사항을 보존한다.
- 일반 JAR 동작은 유지한다. multi-release 동작은 manifest에 `Multi-Release: true`가 있을 때만 사용한다.
- 기본 multi-release 대상은 현재 JVM feature version이다. versioned entry를 직접 선택하면 해당 entry의 버전을 대상으로 사용한다.
- JD-Core loader가 대상 버전부터 9까지 숫자를 내려가며 검색하므로, 직접 선택한 비정상 버전 디렉터리 번호가 255보다 크면 거부한다.
- Linux와 macOS 수동 테스트는 사용자 요청에 따라 추후 진행한다.

## 검토 항목

1. 일반 JAR과 multi-release manifest가 없는 JAR은 root class를 사용하는지 확인한다.
2. 대상 JVM보다 높은 버전 디렉터리는 건너뛰고, 지원 가능한 가장 높은 버전을 선택하는지 확인한다.
3. versioned entry를 직접 선택하면 경로를 정규화하고 그 버전을 대상으로 사용하는지 확인한다.
4. 참조된 class에도 동일한 대상 버전을 적용하는지 확인한다.
5. module annotation, `requires static`, module version 동작과 암묵적인 `java.base` 생략을 확인한다.

---

### Task 1: 의존성 버전 및 빌드 안내 갱신

**파일:** `build.gradle`, `README.md`, `.github/workflows/ci.yml`; `.github/workflows/release.yml`, `services/build.gradle` 확인.

- [x] 기본 `jdCoreVersion`과 README의 버전 안내를 1.3.0으로 갱신하고 기존 문서 변경을 보존한다.
- [x] CI 주석의 이전 JD-Core 버전을 갱신한다. 릴리스 workflow가 최신 core tag를 동적으로 선택하는지 확인한다.
- [x] 의존성 좌표와 fat JAR manifest 버전을 검증한다.

근거: `./gradlew :services:dependencyInsight --dependency jd-core --configuration runtimeClasspath -PjdCoreJitpack`에서 `com.github.asprenas:jd-core:v1.3.0` 선택을 확인했다. `./gradlew clean test build -PjdCoreJitpack`이 통과했고, 생성된 manifest에 `JD-Core-Version: 1.3.0`이 기록됐다.

### Task 2: ContainerLoader의 archive root 검색 추가

**파일:** `services/src/main/java/org/jd/gui/util/decompiler/ContainerLoader.java`, `services/src/test/java/org/jd/gui/util/decompiler/ContainerLoaderTest.java`.

- [x] 선택 entry, sibling, archive root 하위 경로, 없는 경로를 검사하고 JD-Core `MultiReleaseLoader`가 versioned class를 찾는 테스트를 추가한다.
- [x] 선택 entry와 sibling 검색을 유지하면서 archive root 경로 탐색을 구현한다.
- [x] `./gradlew :services:test --tests '*ContainerLoaderTest' -PjdCoreJitpack`을 실행한다.

### Task 3: 화면과 소스 내보내기에서 loader 선택 공유

**파일:** `ClassFilePage.java`, `ModuleInfoFilePage.java`, `ClassFileSourceSaverProvider.java`, 신규 `DecompilerLoaderFactory.java` 및 통합 테스트.

- [x] root/versioned class, dependency, manifest 변형을 포함하는 multi-release fixture를 추가한다.
- [x] loader와 정규화된 internal name을 반환하는 `DecompilerLoaderFactory.create(Container.Entry, int)`를 구현하고 클래스, module-info, 소스 내보내기에서 사용한다.
- [x] versioned class가 같은 대상 버전의 참조 dependency를 로드하는지 검증한다.
- [x] 화면, loader, source saver 대상 테스트를 실행한다.
- [x] 비정상적으로 큰 version 디렉터리 번호를 거부하고, version 99를 직접 선택하면 해당 entry를 사용하는지 확인한다.

### Task 4: JD-Core 1.2.1 회귀와 module-info 왕복 검증

**파일:** `services/src/test/java/org/jd/gui/view/component/`의 신규 `ModuleInfoFilePageTest.java`, `JDCore121RegressionTest.java`.

- [x] module annotation과 `requires static`이 있는 module descriptor를 컴파일한다. `ModuleInfoFilePage`로 디컴파일해 암묵적인 `java.base`가 출력되지 않는지 확인하고, 재컴파일 결과의 module 속성을 원본과 비교한다.
- [x] v1.2.1 변경 기록의 generic 호출 cast와 labeled block 제어 흐름 사례를 실제 `ClassFilePage`로 디컴파일한다. 결과 소스를 재컴파일하고 원본/재컴파일 class의 실행 결과를 비교한다.
- [x] `./gradlew :services:test --tests '*ModuleInfoFilePageTest' --tests '*ClassFilePageTest' --tests '*JDCore121RegressionTest' -PjdCoreJitpack`을 실행한다.

### Task 5: 통합 빌드와 릴리스 metadata 검증

- [x] JDK 21에서 JitPack 의존성 해석과 `./gradlew clean test build -PjdCoreJitpack`을 검증한다.
- [x] 릴리스 workflow와 같이 JD-Core v1.3.0을 Maven Local에 게시한다. `org.jd:jd-core:1.3.0` 해석을 확인하고 JDK 21에서 `./gradlew clean test build`를 실행한다.
- [x] CI와 release workflow 설정 및 생성된 fat JAR manifest의 JD-Core 1.3.0 표시를 확인한다.
- [x] Temurin JDK 25에서 `./gradlew clean test build -PjdCoreJitpack`을 실행한다.
- [ ] Linux와 macOS 수동 테스트는 사용자 요청에 따라 추후 진행한다.

**현재 검증 범위:** Windows에서 JDK 21과 Temurin JDK 25 전체 빌드가 통과했다. Linux/macOS 수동 검증은 추후 진행한다. CI workflow는 JDK 21/25 matrix를 유지하며, release workflow는 최신 JD-Core tag를 선택해 Maven Local에 게시한 뒤 JD-GUI를 빌드한다.
