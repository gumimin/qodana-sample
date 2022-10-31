# Qodana
## Qodanaとは
> The code quality platform for your favorite CI.
> Evaluate the integrity of code you own, contract, or purchase. Enrich your CI/CD pipelines with all the smart features you love from JetBrains IDEs

### 種類
- Docker images
    - Qodana for JVM Docker image
        - IntelliJ Ultimate Edition用
    - Qodana for JVM Community Docker image
        - IntelliJ Community Edition用
    - Qodana for JVM Android Docker image
    - Qodana for PHP Docker image
    - Qodana for Python Docker image
- GitHub Actions and GitHub App
- Cloud (TeamCity Cloud)
- Plugins

### 解析結果
- JSON
- UI

### QodanaのPlayground
https://qodana.teamcity.com/login.html

TeamCity上でQodanaの解析結果がどのように表示されるか確認することができる

Qodana for JVM
The Qodana for JVM linter lets you perform static analysis of your JVM codebase. It is based on IntelliJ IDEA – that means great Java support, first-class Kotlin support for the Server-side, and, coming soon, all the other JVM languages!

Qodana for JVMはIntelliJをベースとし、JVM言語の静的解析を行うことができる
現在はJava, Kotlinがサポートされているが、いずれはすべてのJVM言語で使用できる

### YouTube
- Welcome to Qodana (https://www.youtube.com/watch?v=dgIw64OdjdU)
- Improving Your Kotlin Code Quality With Qodana (https://www.youtube.com/watch?v=_3ErSoKsoNQ)

## Getting Started

```shell
docker pull jetbrains/qodana-jvm-community
docker run --rm -it -v <source-directory>/:/data/project/ -p 8080:8080 jetbrains/qodana-jvm-community --show-report

docker run --rm -it -p 8080:8080 \
-v $(pwd)/:/data/project/ \
-v $(pwd)/:/data/results/ \
jetbrains/qodana-jvm-community --show-report

docker run --rm -it -p 8080:8080 \
-v $(pwd)/:/data/project/ \
jetbrains/qodana-jvm-community --save-report
```

- ToolBoxとQodana PluginがインストールされていればProblemとして表示されている箇所をIDE上で開くことができる
- `qodana.yaml`をプロジェクトのルートに置くことで解析項目をカスタマイズできる

## 感想
- Kotlinの静的解析をするうえでJetBrainsが提供しているという安心感
- 遅い


---
- Improving Your Kotlin Code Quality With Qodana(https://www.youtube.com/watch?v=_3ErSoKsoNQ)

Kotlinはで簡潔さ、読みやすさ、安全性を売りにしているが、これらはシンタックスやコンパイラによる効果だけではなく、ツールによってもたらされているものも大きい。
代表的なツールとしてIDEがあるが、シンタックスハイライト、自動補完、リファクタリング機能、その他インスペクションなど様々なサポートを得ることができる。
Qodanaはダッシュボード、リンター(Docker Image)、プラグインの3種がある
Dockerコマンドで解析を走らせたあとはブラウザ上で結果を確認することができる
ブラウザ上でのレポートで指摘されている箇所について、同じ場所をIDEやGitHub上で開くこともできる

Qodana for JVM Image -> IntelliJ Ultimate Edition
Qodana for JVM Community Image -> IntelliJ Community Edition

```shell
docker run --rm -it -v $(pwd)/:/data/project/ -p 8080:8080 jetbrains/qodana-jvm-community --show-report
```


## sample

```kotlin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json // Unused import directive

fun main() = runBlocking {
    launch {
        Thread.sleep(10000) // Inappropriate blocking method call
        println("second")
    }

    delay(5000)
    println("first")
}
```

このコードには2つ問題点がある
- 使用していないImportがある (Unused import directive)
- Coroutiens内でブロッキングなメソッドを呼び出している (Inappropriate blocking method call)



---
# CI連携
CI連携の代表的な方法としては以下の通り
- GitHub App (publicレポジトリのみ)
- GitHub Actions
- Docker ImageをPullして使用する処理をCIに組み込む

## GitHub Actions
### Qodana Linters
以下のようにGitHub Actionsのためのyamlファイルを作成する
```yaml
name: Qodana check on pull requests
on: [pull_request]
jobs:
  qodana:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: JetBrains/qodana-action@v3.2.1
        with:
          linter: qodana-jvm-community
          fail-threshold: 1
```
上記はプルリクをトリガーにし、1つでもproblemが発見された場合は失敗するように記述している
結果は下記の通り

以下workflow内抜粋
```text
---- Qodana - Detailed summary ----

Analysis results: 2 problems detected

Grouping problems by severity: Warning - 2
Name                                      Severity Count problems
Unused import directive                   warning  1      
Inappropriate thread-blocking method call warning  1
-----------------------------------
2021/11/28 16:07:00 IDEA exit code: 255
2021/11/28 16:07:00 Generating html report ...
Generating final reports...
Done
2021/11/28 16:07:02 Sync IDEA cache from: /data/project/.idea to: /data/cache/.idea
Statistics upload took 1.000441323s
Error: Process completed with exit code 255.
```

## Circle CI 連携
以下のようにジョブを設定すればCircle CIでQodanaを組み込むことができる
```yaml
version: 2.1
jobs:
  build:
    machine: true
    steps:
      - checkout
      - run: 'echo "$DOCKER_PASSWORD" | docker login --username $DOCKER_USERNAME --password-stdin'
      - run: 'docker pull jetbrains/qodana-jvm-community'
      - run: 'docker run --rm -it -p 8080:8080 -v $(pwd)/:/data/project/ -v $(pwd)/:/data/results/ jetbrains/qodana-jvm-community --save-report > result.txt'
      - run: 'cat result.txt'
      - run: |
          if grep 'problems detected' result.txt ;
            then exit 1
          fi
```

上記では1つでもProblemがある場合にworkflowを失敗するようにしている
