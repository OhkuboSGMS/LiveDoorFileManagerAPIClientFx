package com.os.ldfmapi

/***
 * LiveDoor FileManagerAPI Client
 * @author OhkuboSGMS
 */

import awaitObjectResponse
import awaitStringResponse
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.DataPart
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import javafx.beans.property.SimpleListProperty
import kotlinx.coroutines.experimental.runBlocking
import tornadofx.*
import java.io.File
import kotlin.coroutines.experimental.coroutineContext
import kotlinx.coroutines.experimental.launch as klaunch



//fun main(args: Array<String>) = runBlocking {
//    val ftp = "http://livedoor.blogcms.jp/blog/XXX/file/ftp/quota"
//    val livedoor = "https://livedoor.blogcms.jp/blog/XXX/file_manager"
//    val password = "YYY"
//    FuelManager.instance.basePath = livedoor
//    FuelManager.instance.baseHeaders = mapOf("X-LDBlog-Token" to password)
//    return@runBlocking
//}

class LiveDoorFileManagerClient(val name: String, val password: String) {
    init {
        FuelManager.instance.apply {
            basePath = "https://livedoor.blogcms.jp/blog/${name}/file_manager"
            baseHeaders = mapOf("X-LDBlog-Token" to password)
        }

    }

    fun login(debug: Boolean = false): Boolean {
        println("login")
        val (req, res, result) = Fuel.post("list").responseObject<LDList>(LDList.Deserializer())
        if (debug) println(req)
        val response = result.takeIf { it is Result.Success }?.get()
        val error = Gson().fromJson(String(res.data), ErrorResponse::class.java)
        return result is Result.Success && error.errors == null

    }

    fun dispose() = {
        println("dispose client")
        FuelManager.instance.apply {
            basePath = ""
            baseHeaders = mapOf()
        }
    }

}

suspend fun File.ldupload_recursive(dir_id: Int, name: String = this.name, onUploaded: () -> Unit) {
    println("upload recursive $name @$dir_id")
    if (!this.exists()) return
    if (this.isDirectory) {
        // klaunch(coroutineContext) {
        // runBlocking {
        ldcreate_dir(dir_id, name)
        onUploaded()
        val request = klaunch {
            val children = this@ldupload_recursive.listFiles()
            children?.let {
                val id = ldlist(dir_id).lists?.filter { it.name == name && it.dir }?.firstOrNull()?.id
                id?.let {
                    children.forEach { klaunch(coroutineContext) { it.ldupload_recursive(id, onUploaded = onUploaded) } }
                }
            }
            //   }

            //  request.join()
            //  return@runBlocking
        }
    } else {
        ldupload(dir_id, name, onUploaded = onUploaded)
    }
}

//dir_idは必須（rootでも)
suspend fun File.ldupload(dir_id: Int, name: String = this.name, debug: Boolean = false, onUploaded: () -> Unit) {
    if (this.exists() == false || name.isEmpty()) return

    val params = mutableListOf("name" to name)
    params.takeIf { dir_id != -1 }?.add("dir_id" to dir_id.toString())

    val (req, res, result) = Fuel.upload("upload", parameters = params).dataParts { request, url ->
        listOf(DataPart(this, "upload_data"))
    }.awaitStringResponse()
    onUploaded()
    if (debug) {
        println(req)
        println(result.get())
    }
}

/**
 * ディレクトリは名前の変更不可-> 上げ直しが必要
 */
suspend fun ldrename(id: Int, rename: String, debug: Boolean = false) {
    println("rename  f:$id to ${rename}")
    val (_, _, result) = Fuel.post("rename", listOf("id" to id, "name" to rename)).awaitStringResponse()
    if (debug) println(result.get())
}

/***
 * ディレクトリが既に存在する場合、エラーを返す
 */
suspend fun ldcreate_dir(dir_id: Int = -1, name: String, debug: Boolean = false) {
    println("create directory　@$dir_id name:$name")
    val params = mutableListOf("name" to name)
    dir_id.takeIf { it != -1 }?.let { params.add("dir_id" to it.toString()) }
    println(params)
    val (req, _, result) = Fuel.post("create_dir", params).awaitStringResponse()
    if (debug) {
        println(req)
        println(result)
    }
}

/**
 * 1.ディレクトリ内のリスト取得
 * 2.そのリストにディレクトリがあればそれをさらに深掘り(1に戻る)
 * 3.リスト内のファイルを消去する
 */
suspend fun ldremove_recursive(dir_id: Int,onRemoved: () -> Unit,onAdded:(Int)->Unit) {
    println("remove under the ${dir_id}")
    val children = ldlist(dir_id, false)
    children.lists?.let { onAdded(it.size) }
    // val req =klaunch {
    children.lists?.forEach {
        println(it.name)
        if (it.dir) {
            ldremove_recursive(it.id,onRemoved,onAdded)
        } else {
            ldremove(it.id, false,onRemoved)
        }
    }
    ldremove(dir_id,onRemoved = onRemoved)
    println("end remove recurisive")

}

/**
 * ディレクトリ内にファイルがある場合は消せない->ldremove_recursiveを使用
 */
suspend fun ldremove(id: Int, debug: Boolean = false,onRemoved: () -> Unit) {
    println("remove ${id}")
    val (req, _, result) = Fuel.post("remove", listOf("id" to id)).awaitStringResponse()
    onRemoved()
    if (debug) {
        println(req)
        println(result.get())
        println(Gson().fromJson(result.get(), ErrorResponse::class.java).toString())
    }
}

suspend fun ldlist(dir_id: Int? = -1, debug: Boolean = false): LDList {
    println("list")
    val param = if (dir_id != null && dir_id != -1) listOf("dir_id" to dir_id) else listOf()
    val (req, res, result) = Fuel.post("list", param).awaitObjectResponse<LDList>(LDList.Deserializer())
    if (debug) println(req)

    val response = result.takeIf { it is Result.Success }?.get()
    val error = Gson().fromJson(String(res.data), ErrorResponse::class.java)
    if (debug) {
        response?.lists?.forEachIndexed { index, ldItem ->
            println("i:${index} :: ${ldItem}")
        }
        response?.parents?.forEachIndexed { index, ldItem ->
            println("parents[${index}]:${ldItem}")
        }
    }
    return response ?: LDList()

}


data class LDList(val lists: Array<LDItem>? = null, val parents: Array<LDItem>? = null) {
    val listsProperty = SimpleListProperty<LDItem>(this, "lists", lists?.toList()?.observable())
    val parentsProperty = SimpleListProperty<LDItem>(this, "Parents", parents?.toList()?.observable())

    class Deserializer : ResponseDeserializable<LDList> {
        override fun deserialize(content: String): LDList? {
            return Gson().fromJson(content, LDList::class.java)
        }

    }
}

data class LDItem(val name: String, val is_dir: Int = 0, val id: Int = 0, val is_file: Int = 0, val is_root: Int = 0) {
    val dir: Boolean get() = if (is_dir == 0) false else true
    val file: Boolean get() = if (is_file == 0) false else true
    val root: Boolean get() = if (is_root == 0) false else true

    class Deserializer : ResponseDeserializable<LDItem> {
        override fun deserialize(content: String): LDItem? {
            return Gson().fromJson(content, LDItem::class.java)
        }

    }
}

data class ErrorResponse(val errors: Array<ErrorMessage>, val status: Boolean)
data class ErrorMessage(val msg: String)