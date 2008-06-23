
"改良" 改良ControllerBridge について　　中岡


改良ControllerBridgeにてコード設計上の問題があったため、
構造を作り直しました.

また、あわせてオプションの与え方を以下のように変更しています。

* preference-file -> config-file

* component -> 廃止しました.

 connectionでRTCインスタンス名も指定できるようにしたので問題ありません。

* controller -> server-name

 ブリッジのCORBA側コンポーネントを生成するファクトリサーバの名前
 （旧来のOpenHRPコントローラの名前）を指定します.
 controllerだとRTCのコントローラと混乱するので変更しました。

* bridge-component -> robot-name

　ブリッジのRTC側コンポーネントの名前。
　これはユーザ側からみてロボットのI/Oを表すことになるので、こうしました。
 デフォルトだと "VirtualRobot" となります。

* without-connection -> 廃止。接続しない場合は、単にconnectionを与えなければよい。

* module -> 初期化関数名を省略できるようにしました。その場合、"コンポーネント名" + "Init" という関数を呼びます.

* mapping-in, array-mapping-in, color-camera, mono-camera, depth-camera

すべて out-port で設定。

全関節の場合は、
out-port ポート名：プロパティ

あるリンクのみの場合は
out-port ポート名：リンク名：プロパティ

カメラは
out-port ポート名：COLOR_IMAGE:カメラid
といった感じ。

* mapping-out, array-mapping-out

in-port で設定。
設定の仕方は out-port の場合と同様。



以上の設定法の詳細は、ソースの BridgeConf.cpp をみてください。
オプションの設定法はもうしばらくは変更があり得ます。
次期リリースまでには固定します.





--------------------------------------------------------------------------------

                        改良ControllerBridge


                                    株式会社エスキューブド 2008/2/20

■概要
改良されたControllerBridgeに関する説明です。改良点は別添えの報告書をご参照ください。

■ファイル
ControllerBridge.cpp        ... OpenHRPのコントローラクラスを実装しています
ControllerBridge.hpp        ... 上のヘッダファイルです
PreferenceManager.cpp       ... オプションの解析を行います
PreferenceManager.hpp       ... 上のヘッダファイルです
ControllerBridgeComp.cpp    ... 内部コンポーネントです
ControllerBridgeComp.hpp    ... 上のヘッダファイルです
RTCutil.cpp                 ... RTコンポーネントのユーティリティです
RTCutil.h                   ... 上のヘッダファイルです
ControllerBridgeServer.cpp  ... 起動処理を行います
Makefile                    ... メークファイルです
Makefile.common             ... メークするソースなどを記述しています
rtc.conf                    ... 内部コンポーネント用設定ファイルです
report.doc                  ... 報告書です
readme.txt                  ... このファイルです

■インストール
1. 解凍
  $(OPENHRPHOME)/Controller/serverにおいて、

    $ tar zxvf ControllerBridge.tar.gz

  として、解凍してください。

2. Makefileの編集
  MakefileのTOP変数をOpenHRPのトップディレクトリを指すように変更してください。

3. OpenHRPのバージョン指定
  SensorStateにdqが入っているバージョンならばControllerBridge.cppの12行め、

    //#define OPENHRP3_HAS_VELOCITY_IN_SENSORSTATE

  のコメントアウトを外してください。関節速度オプションがSensorStateから参照するようになります。

4. Make
  makeコマンドを実行してください。

    $ make

以上です。

■コンポーネントプログラミング
ここでは、RTC化されたコントローラ(以下コントローラコンポーネント)の作成方法について説明します。

コントローラコンポーネントではセンサや関節の情報をInPortから受け取り、OutPortからアクチュエータの値を出力します。また、出力する値の計算はonExecuteメソッド中で行います。

開発の流れは以下のようになります。

1. 開発するコントローラコンポーネントが入出力する情報の種類を決定する
  まず、どのような情報を入力または出力するかを考えます。情報の種類と対応するポートのデータ形式は以下の通りです。

    関節/センサ入力    ... TimedDoubleSeq (InPort)
    アクチュエータ出力 ... TimedDoubleSeq (OutPort)
    カラーカメラ       ... TimedLongSeq (InPort)
    モノクロカメラ     ... TimedOctetSeq (InPort)
    デプスカメラ       ... TimedFloatSeq (InPort)

  関節/センサ入力およびアクチュエータ出力に関しては、ひとつの関節のマッピングも全ての関節のマッピングも同じデータ形式です。
  この対応にしたがい、コントローラコンポーネントが持つポートを決定したら、rtc-templateによってスケルトンを出力します。

2. 初期設定
  コントローラコンポーネントの初期設定は2箇所で行うことができます。

  コンポーネントクラスのコンストラクタ
    コンポーネントクラスが生成される際に実行されます。

  onActivatedメソッド
    シミュレーションが実行される際に実行されます。

  実行されるタイミングが違いますので、より適切な方に初期化処理を記述してください。

3. 制御アルゴリズム
  従来のcontrolメソッドに相当します。コントローラコンポーネントの制御アルゴリズムはonExecutedメソッドに記述します。
  ここで、それぞれのInPortのupdateメソッドを呼び、データを解析した出力データをOutPortにセットした後、OutPortのwriteメソッドを呼びます。

添付のSamplePDコンポーネントでは、
1. 入出力
  関節値の入力ポートangle
  アクチュエータトルクの出力ポートtorque
  を使用しています。

2. 初期設定
  コンストラクタ
    パターンファイルを開きます。

  onActivatedメソッド
    シーク位置を先頭にします。
    最初期データをセットします。

3. 制御アルゴリズム
  ポートangleから読み込んだ関節角度とそこから生成した関節角速度をパターンファイルの目標数値へPD制御をかけます。

というように行っています。

■ロード対応コンポーネント
改良コントローラブリッジでは、外部のモジュールとしてコントローラコンポーねんとをロードすることができます。
上記プログラミング方法でもダイナミックライブラリが生成されますが、初期化関数においてManager#createComponentを呼ばないため、問題が生じます。
そこで、コンポーネント起動部分である、*Comp.cppの関数MyModuleInitをコンポーネントのソースに移してコンパイルしてください。
この方法でコンパイルされたダイナミックライブラリにはManager#createComponentを呼ぶ初期化関数MyModuleInitが含まれるため、正常にロードすることができます。

また、コントローラブリッジが存在するディレクトリのrtc.confに以下の2行が適切に記述されていることを確認してください。
  manager.modules.load_path: (ダイナミックライブラリが存在するディレクトリ)
  manager.modules.abs_path_allowed: yes

■オプション
・例
example-option example1:example2
  例の説明

以上の説明があった場合、コマンドラインに対しては、

--example-option example1:example2

のように指定し、ファイルに対しては、

example-option = example1:example2

のように設定してください。


・ヘルプ
help
  コマンドラインのみ有効です。オプションの一覧を出力します。

・設定ファイル読み込み
preference-file ファイル名
  コマンドラインのみ有効です。設定ファイルを読み込みます。また、-pオプションでも等価です。

・コンポーネント指定
component コンポーネント名
  コントローラコンポーネントを指定します。-cオプションと等価です。

・OpenHRPネームサーバ設定
name-server ネームサーバホスト名:ポート
  OpenHRPのCORBAネームサーバを指定します。-nオプションと等価です。デフォルトではlocalhost:2809です。

・コントローラコンポーネント設定
controller コントローラ名
  OpenHRPに登録されるコントローラ名を指定します。GrxUIから見える名称です。デフォルトではControllerBridgeです。

・内部コンポーネント設定
bridge-component コンポーネント名
  内部コンポーネントを指定します。デフォルトではControllerBridgeCompです。

・接続
without-connection
  ControllerBridgeによるコントローラコンポーネントへの接続を行いません。RtcLinkを使用する際に指定してください。

・外部モジュールロード
module モジュール名:初期化関数名
  起動時に外部のモジュールをロードします。モジュールのライブラリファイル名とその初期化関数を:でわけて記述してください。

・単体入力マッピング
mapping-in ポート名:関節/センサID:プロパティ
  DynamicsSimulatorからjointIdもしくはsensorIdによって指定された関節/センサのデータを指定されたポート名のポートにマッピングします。プロパティには以下があります。

    JOINT_VALUE    ... 関節値
    JOINT_VELOCITY ... 関節速度
    FORCE          ... 力センサ出力
    RATE_GYRO      ... ジャイロセンサ出力
    ACCEL          ... 加速度センサ出力  

  例えば、ポートangleにjointId=0の関節値をマッピングするには以下のようにします。

    mapping-in angle:0:JOINT_VALUE


以下のマッピング設定において、ポート名はブリッジ内部コンポーネントのポートとなります。
・単体出力マッピング
mapping-out ポート名:関節ID:プロパティ
  関節IDの関節に指定されたプロパティの型のデータをセットします。プロパティには以下があります。

    JOINT_VALUE        ... 関節値
    JOINT_VELOCITY     ... 関節速度
    JOINT_ACCELERATION ... 関節加速度
    JOINT_TORQUE       ... 関節トルク
    EXTERNAL_FORCE     ... 力, トルク

  例えば、ポートtorqueをjointId=0の関節トルクとするには以下のように設定します。

    mapping-out torque:0:JOINT_TORQUE

・全関節入力マッピング
array-mapping-in  ポート名:プロパティ
  jointIdがついた全ての関節をポート名で指定されたポートに指定されたプロパティでマッピングします。プロパティには以下があります。

    JOINT_VALUE
    JOINT_VELOCITY

・全関節出力マッピング
array-mapping-out ポート名:プロパティ
  jointidがついた全ての関節に指定されたプロパティのデータを設定します。使用できるプロパティは以下の通りです。

    JOINT_VALUE
    JOINT_VELOCITY
    JOINT_ACCELERATION
    JOINT_TORQUE

・カラーカメラ
color-camera ポート名:カメラID
  カラーカメラの出力であるlongの配列をTimedLongSeqとしてマッピングします。

・モノクロカメラ
mono-camera ポート名:カメラID
  モノクロカメラの出力であるoctetの配列をTimedOctetSeqとしてマッピングします。

・デプスカメラ
depth-camera ポート名:カメラID
  デプスカメラの出力であるfloatの配列をTimedFloatSeqとしてマッピングします。

・ポート接続
port-connection コントローラブリッジポート名:コントローラコンポーネントポート名
  コントローラブリッジのポートとコントローラコンポーネントのポートを接続します。

