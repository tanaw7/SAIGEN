package saarland.cispa.tanapuch.thesis

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class ImagesDifference{

}

fun main(args: Array<String>) {


    val folder1 = "C:\\OtherGames\\tmpDel\\false_pstore_com_mirkoo_apps_mislibros_apk\\vis\\img\\states"
    val folder2 = "C:\\OtherGames\\tmpDel\\true_pstore_com_mirkoo_apps_mislibros_apk\\vis\\img\\states"

    var listA = mutableListOf<String>()
    var listB = mutableListOf<String>()
    var listC = mutableListOf<String>()

    File(folder1).walk()
            .forEach { file ->
                if (file.toString().endsWith("png")) {
                    listA.add(file.name.substringBefore("_"))
                }
            }
    File(folder2).walk()
            .forEach { file ->
                if (file.toString().endsWith("png")) {
                    listB.add(file.name.substringBefore("_"))
                }
            }

    listA = listA.distinct().toMutableList()
    listB = listB.distinct().toMutableList()

    listA.forEach { fileName ->
        if(fileName !in listB){
            listC.add(fileName)
        }
    }

    listC = listC.distinct().toMutableList()

    println(listC)

    File(folder1).walk()
            .forEach{
                val a = it
                val b =it.toPath()
                val c = it.parent
                val d = it.parentFile
                val e = it.parentFile.toPath()

                if (it.name.substringBefore("_") in listC){
                    Files.copy(it.toPath(), Paths.get("C:\\OtherGames\\tmpDel\\zlistCFolder\\${it.name}"))
                }

                //Files.copy(it.toPath(), it.parentFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

}