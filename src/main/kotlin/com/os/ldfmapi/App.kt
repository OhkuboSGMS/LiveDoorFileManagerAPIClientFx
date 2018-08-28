package com.os.ldfmapi

import tornadofx.*

/***
 * LiveDoor FileManagerAPI Client
 * @author OhkuboSGMS
 */
class LiveDoorFileManagerClientApp : App(LDLogin::class)

fun main(args: Array<String>) {
    launch<LiveDoorFileManagerClientApp>(args)
}
