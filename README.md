# LiveDoor FileManager API Client

ライブドアブログのファイル管理のクライアント



ライブドアブログのファイル管理は2018年2月くらいまではffftpなどが
使用できていましたが、[FileManager API](http://help.blogpark.jp/archives/52491176.html)という仕様変更によってこれらが
使用できなくなりました.

このため、クライアントアプリを作成しました

公式のweb版のクライアントと比較して

* 複数同時アップロード
* 複数同時削除

ができます

仕様としてはひたすらhttpリクエストを叩き続ける感じです

# 使用方法

releaseに各プラットフォームにおける実行ファイルを置いていますのでダウンロードして実行してください。またはソースからビルドもできます。

# 開発環境
* IDE:Jetbrains IDEA
* 言語:kotlin(1.2.31) ,java(1.8)

ライブラリ

* tornadofx(UI作成)
* fuel(http通信
* gson(jsonパーサ)

アプリアイコン作成,使用

* [Launcher Icon Generator](https://github.com/romannurik/AndroidAssetStudio)
* [flat icon](https://www.flaticon.com/)

# ログイン

![login](https://imgur.com/download/gZCrju7)

ログインではAPIが使用可能かのチェックをします
ログイン画面では↓の情報を入力してください

# BlogName,Passwordの場所
ログイン時に入力するBlogName,Passwordは

LiveDoorブログの //ブログ設定->API Key-> File Manager API
にあります。

BlogNameは  ルートエンドポイントの　Nameの部分

https://livedoor.blogcms.jp/blog/***Name*** /file_manager

passwordは File Manager用パスワードにあります。

![filemanager token](https://imgur.com/download/1bJ2pni)


# できること

* ディレクトリ閲覧
* アップロード(複数ディレクトリ、ファイル可)
* ファイル名変更　ディレクトリは名前変更不可
* ディレクトリ作成
* ファイル、フォルダ削除


アップロードはD&Ddで、ファイル名変更、ディレクトリ作成、削除は右クリックでできます。

複数削除は複数選択でできます。

アップロードできる１ファイルの最大サイズは10MBは公式の通りです。

# Licence

MIT

# 免責事項
このアプリケーションはLiveDoor FileManager API 非公式クライアントです。このアプリケーションによって生じた利用者に生じた損害について、作者は責任を負いかねますので御了承ください。