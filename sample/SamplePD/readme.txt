改良ControllerBridge対応SamplePDコントローラコンポーネント

■概要
改良ControllerBridgeに対応したコンポーネントです。改良前と比較してSamplePDの内容は変わりませんが、設定ファイルに変更があります。

■ファイル
Makefile          ... メークファイル
SamplePD.cpp      ... コンポーネントソース
SamplePD.h        ... コンポーネントヘッダ
SamplePDComp.cpp  ... コンポーネント起動部
etc/              ... パターンファイルディレクトリ
etc/PDgain.dat    ... 制御定数
etc/angle.dat     ... 角度パターン
etc/vel.dat       ... 速度パターン
rtc.conf          ... RTコンポーネント設定ファイル
bridge.conf       ... コントローラブリッジ設定ファイル
SamplePD_RTC.xml  ... プロジェクトファイル
SamplePD.sh       ... 実行スクリプト
readme.txt        ... このファイル

■インストール
1. 解凍
ファイルアーカイブを解凍してください。

  $ tar zxvf SamplePD.tar.gz

2. Makefileを編集する
Makefile 22行めにある以下を、OpenHRPのトップディレクトリを指すように編集してください。

TOP = ../../../


3. コンパイル
Makeを実行してください。

  $ make

以上です。

■起動方法
まず、ControllerBridgeのrtc.confを次のように設定してください。

manager.modules.load_path: (SamplePDを展開したディレクトリ)
manager.modules.abs_path_allowed: yes

その上で、以下を実行してください。

コマンドラインから設定する場合:

$(OPENHRPHOME)/Controller/Server/ControllerBridge/ControllerBridge \
  --array-mapping-in angle:JOINT_VALUE \
  --array-mapping-out torque:JOINT_TORQUE \
  --port-connection angle:angle \
  --port-connection torque:torque \
  --module SamplePD.so:MyModuleInit \
  --component SamplePD0.rtc 

以上でSamplePD0.rtcの名付けられたコンポーネントに関節速度と値をマッピングします。SamplePD.shはこのオプションを実行します。

設定ファイルから設定する場合:

$(OPENHRPHOME)/Controller/Server/ControllerBridge/ControllerBridge \
  -p bridge.conf

以上でカレントディレクトリにあるbridge.confを読み込み起動します。

■シミュレーション
同梱のSamplePD_RTC.xmlをGrxUIにロードし、シミュレーションを実行してください。
適切にコンポーネント同士を接続し、シミュレーションを行います。
