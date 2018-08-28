package com.os.ldfmapi

import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import javafx.scene.input.TransferMode
import kotlinx.coroutines.experimental.runBlocking
import tornadofx.*
import java.io.File
import kotlinx.coroutines.experimental.launch as klaunch

/***
 * GUI
 * @author OhkuboSGMS
 */
//　アップロード先リンクの取得
class ExploreModel(var ldList: LDList) : ViewModel() {
    val lists = bind { ldList.listsProperty }
    val parents = bind { ldList.parentsProperty }
}

class Explore : Fragment() {
    val list = FXCollections.observableArrayList<LDItem>()
    val path = SimpleStringProperty("/")
    var listview: ListView<LDItem>? = null
    val model = ExploreModel(LDList())
    override val root = vbox {
        menubar {
            menu("Login") {
                item("Back").action {
                    close()
                    find<LDLogin>().openWindow()
                }
            }
        }
        setOnDragOver { event ->
            val board = event.dragboard
            if (board.hasFiles()) {
                event.acceptTransferModes(TransferMode.MOVE)
            }
        }

        //アップロードファイル
        //同じ階層は並列,親子階層は直列
        setOnDragDropped({ event ->
            val board = event.dragboard
            if (board.hasFiles()) {
                this.isDisable = true
                val dir_id = current_dir()
                val fileCount = board.files.size
                val allFileCount = folderSize(board.files)
                val progressProperty = SimpleDoubleProperty(0.0)
                val progress = find<Progress>(mapOf(
                        Progress::achiveCnt to allFileCount, Progress::currentAchivements to progressProperty
                ))
                progress.setNewAllSize(allFileCount)
                        progress.openModal(escapeClosesWindow = false, resizable = true)

                val files = board.files
                println("all file tree size: ${allFileCount}")
                println("horizontal file count ${fileCount}")
                val prog = fun() {
                    runLater {
                        progressProperty.value += 1
                       // println("add progress ${progressProperty.value}")
                        if (progressProperty.value >= allFileCount) {
                            println("progress close")
                            progress?.close()
                            isDisable = false
                        }

                    }
                }
                runAsync {
                    runBlocking {
                        val requests = klaunch {
                            files.forEach { file ->
                                klaunch(coroutineContext) {
                                    if (file.isFile) {
                                        dir_id?.let { file.ldupload(dir_id, onUploaded = prog) }
                                    } else if (file.isDirectory)
                                        dir_id?.let {
                                            file.ldupload_recursive(dir_id, onUploaded = prog)
                                        }

                                    println("uploaded${if (file.isFile) "file" else "directory"}:${file.absolutePath}")
                                    println("lauched next file")
                                }
                            }
                        }
                        println("wait uploading...")
                        requests.join()
                        println("Finish upload")
                        update(dir_id)
                    }
                }

                event.isDropCompleted = true
            } else {
                event.isDropCompleted = false
            }
        })


        //ディレクトリのパスを表示する
        hbox {
            scrollpane(fitToWidth = false) {
                text(path)
                useMaxWidth = true
            }
        }
        //1つ上の階層に移動する
        hbox {
            imageview(resources.image("back.png")) {
                fitWidth = 15.0
                fitHeight = 15.0
            }

            text("Back") {
                paddingLeft = 20
            }
            setOnMouseClicked {
                model.ldList.parents?.let { it.elementAtOrNull(it.size - 2) }?.let { move(it.id) }
            }

        }
        hbox {
            listview = listview<LDItem>(list) {
                cellFormat {

                    lazyContextmenu {
                        //改名
                        item("Rename").action {
                            takeIf { selectionModel.selectedItems.size == 1 && selectedItem?.file == true }?.let {
                                println("Rename: ${selectionModel.selectedItems}")
                                find<Rename>(mapOf(Rename::dir_id to selectionModel.selectedItem.id, Rename::parent_id to current_dir(),
                                        Rename::explore to this@Explore, Rename::originalName to selectionModel.selectedItem.name))
                                        .openModal()

                            }
                        }
                        //削除
                        item("Remove").action {
                            println("Remove ${selectionModel.selectedItems}")
                            val progressProperty = SimpleDoubleProperty(0.0)
                            val progress = find<Progress>(mapOf(
                                    Progress::achiveCnt to 0, Progress::currentAchivements to progressProperty
                            ))
                            progress.openModal()
                            val onRemoved = fun() {
                                runLater {
                                    progressProperty.value += 1
                                 //   println("add progress ${progressProperty.value}")
                                }
                            }
                            val onAddAllSize = fun(plus:Int){
                                runLater {
                                    progress.addAllSize(plus)
                                }
                            }


                            runAsync {
                                runBlocking {
                                    println("selecteeitem size ${selectionModel.selectedItems.size}")
                                    val reuqests = List(selectionModel.selectedItems.size) {
                                        klaunch {
                                            selectionModel.selectedItems[it].let {
                                                println("deleted ${it.name}")
                                                if (it.dir) ldremove_recursive(it.id,onRemoved = onRemoved,onAdded = onAddAllSize)
                                                else ldremove(it.id,onRemoved = onRemoved)
                                            }
                                        }
                                    }
                                    reuqests.forEach { it.join() }
                                    model.ldList.parents?.let { it.elementAtOrNull(it.size - 1) }.let {
                                        println("update:${it?.id}")
                                        update(it?.id)
                                    }
                                }
                                println("delete all")
                                runLater {
                                    println("progress close")
                                    progress?.close()
                                    isDisable = false }
                            }
                        }
                        //ディレクトリ作成
                        item("MakeDirectory").action {
                            find<MakeDirectory>(mapOf(MakeDirectory::parent_id to current_dir(),
                                    MakeDirectory::explore to this@Explore)).openModal()
                        }
                    }
                    onDoubleClick {
                        if (it.dir) {
                            move(it.id)

                        }
                    }
                    graphic =
                            hbox {
                                val icon = if (it.dir) "folder.png" else "file.png"
                                val iconOb = stringBinding(icon) { "$icon" }
                                imageview(resources.image(iconOb.value)) {
                                    fitWidthProperty().set(15.0)
                                    fitHeightProperty().set(15.0)
                                    alignment = Pos.CENTER_LEFT
                                }

                                // println("Item:${it.name}")
                                text(stringBinding(it.name) { "${it.name}" }) {
                                    useMaxWidth = true
                                    alignment = Pos.CENTER_LEFT
                                }

                            }

                    selectionModel.apply {
                        selectionMode = SelectionMode.MULTIPLE
                    }

                }
            }
            runBlocking {
                model.ldList = ldlist()
                model.ldList.lists?.let {
                    runLater {
                        listview?.items?.addAll(it)
                    }
                }
            }


        }

    }

    init {
        title = "LiveDoorFileManager"


    }

    private fun move(id: Int) {
        kotlinx.coroutines.experimental.launch {
            model.ldList = ldlist(id)
            model.ldList?.let {
                //set path,set item
                runLater {
                    val p = it.parents?.reduce { acc,
                                                 ldItem ->
                        LDItem(acc.name + "/" + ldItem.name)
                    }?.name
                    println("file path:$p")
                    path.value = p
                    list.setAll(it.lists?.toList())
                }
            }
        }
    }

    fun update(id: Int?, debug: Boolean = false) {
        println("update :${id}")
        klaunch {
            runBlocking {
                model.ldList = ldlist(id, debug)
                model.ldList?.let {
                    //set path,set item
                    runLater {
                        // println("Update list${it.lists?.get(0)}")
//                        listview?.items?.clear()
                        //list.clear()
                        // list.setAll(model.ldList?.lists?.toMutableList())
                        it.lists?.toList()?.let { it1 -> list?.setAll(it1) }

                    }
                }
            }
        }
    }

    private fun folderSize(files: List<File>): Int {
        return files.map { folderSize(it) }.sum()
    }

    private fun folderSize(file: File): Int {
        if (file.isDirectory) {
            return file.walkTopDown().count()
        } else if (file.isFile) {
            return 1
        }
        return 0
    }

    private fun current_dir(): Int? {
        val dir = model.ldList.parents?.let { it.elementAtOrNull(it.size - 1) }?.id
        if (dir != null) return dir
        else if (model.ldList.parents?.last() != null) return model.ldList.parents?.last()?.id

        return null
    }
}

class Progress : View() {
    val currentAchivements: SimpleDoubleProperty by param()
    val achiveCnt: Int by param()
    var vAcieveCnt :Int =achiveCnt
    var progressPP: DoubleProperty? = null
    var display: Label? = null
    override val root = vbox {
        display = label("PROGRESS")
        progressbar() {
            useMaxWidth = true
            progressPP = progressProperty()


        }
    }

    override fun onBeforeShow() {
        currentAchivements.value = 0.0
        currentAchivements.addListener { observable, oldValue, newValue ->
            println("progress changed ${currentAchivements.value} max $vAcieveCnt")
            progressPP?.value = currentAchivements.doubleValue() / vAcieveCnt.toDouble()
            display?.text = "${currentAchivements.intValue()} / ${vAcieveCnt} Complete"
        }
    }
    fun setNewAllSize(new :Int){
        vAcieveCnt =new

    }
    fun addAllSize(add:Int){
        vAcieveCnt +=add
    }

    override fun onDelete() {
        super.onDelete()
    }
}

class Rename : View() {
    val originalName: String by param()
    val parent_id: Int by param()
    val dir_id: Int by param()
    val rename = SimpleStringProperty()
    val explore: Explore by param()
    override fun onBeforeShow() {
        originalName?.let { rename.value = it }
    }

    override val root = vbox {
        form {
            fieldset {
                field("rename") {
                    textfield(rename)
                }
            }
        }
        button("Rename") {
            action {
                rename.value?.takeIf { it.isNotEmpty() }?.let {
                    klaunch {
                        runBlocking {
                            ldrename(dir_id, it, false)
                            explore.update(parent_id, false)
                            println("end list update")
                            runLater {
                                println("close")
                                close()
                            }

                        }
                    }
                }
            }
            shortcut("Enter")
        }
    }
}

class MakeDirectory : View() {
    val parent_id: Int by param()
    val rename = SimpleStringProperty()
    val explore: Explore by param()
    override val root = vbox {
        form {
            fieldset {
                field("directory") {
                    textfield(rename)
                }
            }
        }
        button("Make") {
            action {
                rename.value?.takeIf { it.isNotEmpty() }?.let {
                    klaunch {
                        runBlocking {
                            ldcreate_dir(parent_id, it)
                            explore.update(parent_id, true)
                            println("end list update")
                            runLater {
                                println("close")
                                close()
                            }
                        }
                    }
                }
            }
            shortcut("Enter")
        }
    }
}





