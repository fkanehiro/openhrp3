Eclipse版grxUIの使い方

1.準備
1.1.必須プラグイン http://java3d-eclipse.sourceforge.net/から取得します
　core.java3declipse.org_1.1.0.zip
　java3declipse-1.5.1.zip
その他OpenRTM-aistの動作条件を整えてください。

1.2. grxUIプラグインのインストール
・ソースからビルドしない場合はzipファイルを解凍してできたpluginsディレクトリ内の
　com.generalrobotix.ui.grxui.0.9.0 をeclipseのpluginsディレクトリにディレクトリごと
　コピーしてください。

・ソースからビルドする必要がある場合はeclipseを起動し、「プラグイン開発」のパースペクティブ
　を開き、パッケージ・エクスプローラーから「一般」→「既存プロジェクトをワークスペースへ」
　からインポートを行ってください。
・必要に応じてソースを編集し、ビルドしてください。その後プラグインとしてインストールして
　ください。
・Windows上などのデフォルト文字コードがUTF-8でない場合はbuild.propertiesに次の行を加えてください
javaDefaultEncoding.. = UTF-8

2.起動
2.1.必要なサービスをバッチファイルまたはシェルスクリプトにて起動します。この際ネームサービス
　を最初に起動する必要があります。
2.2.eclipseを起動し、grxUIパースペクティブを開きます。

※現状Windows上のeclipseでは既存のプロジェクトがシミュレーションできません。
