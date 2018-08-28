package com.os.ldfmapi

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Label
import javafx.scene.paint.Color
import tornadofx.*

/***
 * LiveDoor FileManagerAPI Client
 * @author OhkuboSGMS
 */
class LDLogin : View() {
    private val name = SimpleStringProperty(this, "my blog name", config.string("blog_name"))
    private val password = SimpleStringProperty(this, "password", config.string("password"))
    var notification: Label? = null
    override val root = form {
        fieldset {
            vbox {
                hbox {
                    field("Blog Name") {
                        textfield(name)
                    }
                }
                hbox {
                    field("Password") {
                        textfield(password)
                    }
                }
                hbox {
                    button("Login") {
                        action {
                            val client = LiveDoorFileManagerClient(name.value, password.value)
                            runAsync { client.login() } ui { success ->

                                if (success) {
                                    notification?.apply {
                                        text = "Login Success"
                                        paddingLeft = 10
                                        style {
                                            textFill = Color.LIGHTGREEN
                                        }
                                        show()
                                    }


                                    with(config) {
                                        set("blog_name" to name.value)
                                        set("password" to password.value)
                                        save()
                                    }
                                    close()
                                    find<Explore>().openWindow()

                                } else {
                                    notification?.apply {
                                        paddingLeft = 10
                                        text = "Login Failed"
                                        style {
                                            textFill = Color.RED
                                        }
                                        show()

                                    }
                                    //find<Notify>().openModal(StageStyle.UTILITY)
                                }


                            }
                        }
                        shortcut("Enter")
                    }

                    notification = label("notification") {
                        hide()
                    }
                }
            }
        }
    }

    init {
        setStageIcon(resources.image("icon.png"))
        title = "login"

    }
}

class Notify : View() {
    override val root = vbox {
        label("Fail Login name or pass is incorrect")
        button("OK") {
            action { close() }
            useMaxWidth = true
        }
    }
}