package com.os.ldfmapi

import tornadofx.*

/*
    1.アカウント情報入力画面
     blog name
     password

    2.ルートのファイル情報を表示
    3.左クリックででxレク取り進む
    右クリックでデリート
 */

class HelloWorld : View() {
    override val root = hbox {
        label("Hello World")
        button("wat") {
            action {
                chooseFile(filters = arrayOf()) {
                }
            }
        }

    }
}

class MyApp : App(HelloWorld::class)