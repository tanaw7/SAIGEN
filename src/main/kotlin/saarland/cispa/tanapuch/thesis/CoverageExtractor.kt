package saarland.cispa.tanapuch.thesis

import edu.stanford.nlp.ie.pascal.ISODateInstance
import org.droidmate.configuration.ConfigProperties
import org.droidmate.exploration.statemodel.features.StatementCoverageMF
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.util.calendar.LocalGregorianCalendar
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.streams.toList


class CoverageExtractor {


}

private val log: Logger by lazy { LoggerFactory.getLogger(StatementCoverageMF::class.java) }
private val instrumentationDir = Paths.get(System.getProperty("user.dir") + "\\instrumentation-logs\\")

/** coverageFolderPath is the root folder for which its subtrees contain all the coverage files ended with "logcat__000"
 *  Change it to the root folder of your choice to calculate all coverage files within its subtrees.
 *  The result coverage would be printed to coverageResult.txt in the same folder of its coverage file.
 *  "user.dir" is just the current directory of this project.
 * */
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\100actions tests"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\500actions tests"
private val coverageFolderPath = System.getProperty("user.dir") + "\\out"
//private val coverageFolderPath = System.getProperty("user.dir") + "\\instrumentation-logs\\RQ3_coverage\\address_book_for_coverage_debug"

fun main(args: Array<String>){

    println("Hello, Coverage World!")

    var numOfCoverageFile = 0
    File(coverageFolderPath).walk().forEach {
        if (it.path.contains("logcat__000")){
            numOfCoverageFile++
        }
    }

    println("Number of coverage files: $numOfCoverageFile")
    println("")

    numOfCoverageFile = 0
    File(coverageFolderPath).walk().forEach {
        if (it.path.contains("logcat__000")) {

            val coverageFile = Files.readAllLines(it.toPath())
            var coverageFileUidOnly = mutableListOf<String>()

            coverageFile.removeAt(0)
            coverageFile.removeAt(0)
            /*
            coverageFile.forEach{
                coverageFileUidOnly.add(it.substringAfterLast("uuid="))
            }
            coverageFileUidOnly = coverageFileUidOnly.toList().distinct().toMutableList()
            */

            coverageFile.forEach{
                coverageFileUidOnly.add(it.substringAfter("reach 1: "))
            }
            coverageFileUidOnly = coverageFileUidOnly.toList().distinct().toMutableList()

            //val baseTime = Pair(coverageFileUidOnly[0].first, coverageFileUidOnly[0].second)

            val fullPath = it.path
            val apkName = fullPath.substringAfterLast("\\").substringBeforeLast("-logcat__000")


            //val instrumentedJson = getInstrumentation(apkName).keys.toList().distinct()
            val instrumentedJson = getInstrumentation(apkName).values.toList().distinct()

            var coverageCounter = 0

            coverageFileUidOnly.forEach{
                if (it in instrumentedJson){
                    coverageCounter++
                }
            }

            println("#: ${numOfCoverageFile+1}")
            println("fileName: $apkName")
            println("FileLocation: $it")
            println("FileParent  : ${it.parent}")
            println("")
            println("Coverage counter       : $coverageCounter")
            println("Instrumented state size: ${instrumentedJson.size}")
            println("Ratio                  : ${coverageCounter.toFloat()/instrumentedJson.size.toFloat()}")
            println("----------------------------------------------------------------------------")
            println("")

            File(it.parent + "\\coverageResult.txt").printWriter().use { out ->
                out.println("#: ${numOfCoverageFile+1}")
                out.println("fileName: $apkName")
                out.println("FileLocation: $it")
                out.println("FileParent  : ${it.parent}")
                out.println("")
                out.println("Coverage counter       : $coverageCounter")
                out.println("Instrumented state size: ${instrumentedJson.size}")
                out.println("Ratio                  : ${coverageCounter.toFloat()/instrumentedJson.size.toFloat()}")
                out.println("----------------------------------------------------------------------------")
                out.println("End")
            }

            numOfCoverageFile++
        }
    }

    println("Number of processed coverage files: $numOfCoverageFile")

    println("")


}


private fun getInstrumentation(apkName: String): Map<String, String> {
    return if (!Files.exists(instrumentationDir)) {
        log.warn("Provided statementCoverageDir does not exist: ${ConfigProperties.ModelProperties.Features.statementCoverageDir}. DroidMate will monitor coverage will not be able to calculate coverage.")
        emptyMap()
    }
    else {
        val instrumentationFile = getInstrumentationFile(apkName)

        if (instrumentationFile != null)
            readInstrumentationFile(instrumentationFile)
        else
            emptyMap()
    }
}

private fun getInstrumentationFile(apkName: String): Path? {
    return Files.list(instrumentationDir)
            .toList()
            .firstOrNull{ it.fileName.toString().contains(apkName)
                    && it.fileName.toString().endsWith(".apk.json")}
}

@Throws(IOException::class)
private fun readInstrumentationFile(instrumentationFile: Path): Map<String, String> {
    val jsonData = String(Files.readAllBytes(instrumentationFile))
    val jObj = JSONObject(jsonData)

    val jArr = JSONArray(jObj.getJSONArray("allMethods").toString())

    val l = "9946a686-9ef6-494f-b893-ac8b78efb667".length
    val statements : MutableMap<String, String> = mutableMapOf()
    (0 until jArr.length()).forEach { idx ->
        val method = jArr[idx]

        if (!method.toString().contains("CoverageHelper")) {
            val parts = method.toString().split("uuid=".toRegex(), 2).toTypedArray()
            val uuid = parts.last()

            assert(uuid.length == l) { "Invalid UUID $uuid $method" }

            statements[uuid] = method.toString()
        }
    }

    return statements
}