package saarland.cispa.tanapuch.thesis

import com.natpryce.konfig.parseArgs
import org.droidmate.exploration.statemodel.features.ModelFeature
import org.droidmate.exploration.ExplorationContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.math.abs
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import edu.stanford.nlp.util.Quadruple
import edu.stanford.nlp.process.Morphology
import formsolver.CommonData
import formsolver.FormSolver
import formsolver.WordNet
import kotlinx.coroutines.experimental.*
import org.apache.jena.query.Query
import org.droidmate.exploration.statemodel.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO
import java.util.concurrent.ThreadLocalRandom

import edu.washington.cs.knowitall.morpha.MorphaStemmer

class SemanticAwareInputGenerator : ModelFeature() {
	override val context: CoroutineContext =
			newCoroutineContext(context = CoroutineName("testModelFeature"), parent = job)

	init {
		// we don't want to wait for other features (or having them wait for us), therefore create our own (child) job
		job = Job(parent = (this.job))
	}

	val saiGenEnabled = true

	private var masterMap = mutableMapOf<Widget, Widget>()
	private var masterMapWithID = mutableMapOf<String, MutableMap<Widget, Widget>>()

	private var masterMapText = mutableMapOf<Widget,String>()

	private val nullWidget = Widget() // a null widget template for dataWidget that doesn't have a match.
	private var dataWidgetCount = 0
	private var matchedPairCount = 0

	private var mapOfListOfStringToQueriesAndResults: MutableMap<MutableList<String>, MutableMap<Query, MutableList<MutableMap<String, String>>>?> = mutableMapOf()
	private var mapOfDataWidgetUIDtoValueMaster : MutableMap<UUID, String> = mutableMapOf()
	private var mapODWTV: MutableMap<UUID, String> = mutableMapOf()

	private val tagger: MaxentTagger = MaxentTagger("models/english-left3words-distsim.tagger")

	private val wNet = WordNet()

	private val lemmatize = Morphology() // this lemmatize from stanford uni can be used to morph plural into singular using tag "NNS". Test later.

	//vars for RQ2 evaluation
	private var stateIDtoFillingValuesPairList: MutableList<Pair<String, List<Pair<Widget,String>>>> = mutableListOf()
	private var valuesToFillMasterList: MutableList<Pair<Widget, String>> = mutableListOf()

	private var numOfMatchedDataWidgets: Int = 0
	val matchedOnlyMapGlobal: MutableMap<Widget,Widget> = mutableMapOf()
	val notFilledMapGlobal: MutableMap<Widget,Widget> = mutableMapOf()
	private var matchedOnlyMapUidMasterList: MutableList<UUID> = mutableListOf()

	private var uniqueNumDWL = 0
	private var uniqueNumDWV = 0
	private var totalNumDWL = 0
	private var totalNumDWV = 0

	private var numOfNonounMaster = 0

	private var bannedLabelList: MutableList<String> = mutableListOf("email", "password", "username", "login", "screen", "nonoun") //+ "nonoun"? //User's config
	private var bannedLabelMasterList: MutableList<String> = mutableListOf() //The actual occurrence of the banned labels during the apk run.

	private var listOfInt: MutableList<Quadruple<Int,Int, Int, Int>> = mutableListOf()
	//var listOfDuplicate: MutableList<MutableList<Boolean>> = mutableListOf()

	//tempDel Debug
	var randListPair: MutableList<Pair<Int,Int>> = mutableListOf()
	var widgetsMapAndUniqueMapEqualTest: MutableList<Boolean> = mutableListOf()

	override suspend fun onAppExplorationFinished(context: ExplorationContext) {
		masterMap.clear()
		masterMapWithID.clear()

		masterMapText.clear()

		dataWidgetCount = 0
		matchedPairCount = 0

		mapOfListOfStringToQueriesAndResults.clear()
		mapOfDataWidgetUIDtoValueMaster.clear()
		mapODWTV.clear()

		//vars for RQ2 evaluation
		stateIDtoFillingValuesPairList.clear()
		valuesToFillMasterList.clear()

		numOfMatchedDataWidgets = 0
		matchedOnlyMapGlobal.clear()
		matchedOnlyMapUidMasterList.clear()

		uniqueNumDWL = 0
		uniqueNumDWV = 0
		totalNumDWL = 0
		totalNumDWV = 0

		numOfNonounMaster = 0

		bannedLabelList.clear()
		bannedLabelMasterList.clear()

		listOfInt.clear()
		//var listOfDuplicate: MutableList<MutableList<Boolean>> = mutableListOf()
		notFilledMapGlobal.clear()

		//tempDel Debug
		randListPair.clear()
	}

	/*****************************************************************
	 * onNewAction() is the main body function. It runs before going into the Selector.
	 *****************************************************************/

	override suspend fun onNewAction(traceId: UUID, deferredAction: Deferred<ActionData>, prevState: StateData, newState: StateData, context: ExplorationContext) {
		//val lastAction = deferredAction.await()
		//val newState =newState
		deferredAction.await()

		var aaa = context.apk.packageName

		this.numOfMatchedDataWidgets = 0
		this.matchedOnlyMapGlobal.clear()
		val stateID = newState.stateId                  // State ID


		if (this.saiGenEnabled) {
			val mapOfDataWidgetToLabelWidget = mapWidgets(newState)  //This statement is the key to call in the mapping

			masterMapText.clear()
			masterMap.keys.forEach { dataWidget ->
				masterMapText[dataWidget] = dataWidget.text
			}

			var i = 0
			println("----MASTER MAP -------\n")
			masterMap.forEach {
				println("${i.toString().padStart(3)}. Data: ${it.key}")
				println("     LABL: ${it.value}")
				i++
			}

			/*println("----MASTER MAP with StateID -------\n")
		masterMapWithID.forEach {
			println("${it.key} -------- ${it.value}")
		}*/

			/**Maps DataWidgets to queried values. Each DataWidget already has a matched label*/
			mapOfDataWidgetUIDtoValueMaster.putAll(getMapOfDataWidgetUIDtoValue(mapOfDataWidgetToLabelWidget))
			//TODO: Check and test if we can just have mapOfDataWIdgetUidToValueGlobal as the global variable that reset every iteration instead of the Master one where entries are accumulative.
			//TODO: Continue: then we can just pass the mapp of DataWidget-To-Value without having to do all those filtering in getValues().


			/** FOR RQ2 Link evaluation */
			if (this.matchedOnlyMapGlobal.isNotEmpty()) {
				var valuesToFill: List<Pair<Widget, String>> = getValues(newState.actionableWidgets)

				valuesToFill = valuesToFill.distinct().toMutableList() //FIXME: (Debugging in progress) used to be without distinct()
				linkEvaluation(valuesToFill, newState)
			}
		}
	}

	/*****************************************************************
	 * FUNCTION dump() is used mainly to write outputs to files.
	 * It runs after DroidMate reaches the set actionLimit.
	 *****************************************************************/

	override suspend fun dump(context: ExplorationContext) {
		this.await()

		if (saiGenEnabled) {

			println("WHATEVER ENDS HERE FOR MODEL FEATURE?")

			val packageName = context.apk.packageName
			println("----This is the apk package name: $packageName")

			/** disabled for coverage testing */
			drawRectangleOnWidgets(packageName, masterMapWithID)
			drawRectangleOnDataWidgetsWithValues(packageName, stateIDtoFillingValuesPairList)


			println("--------------OUTPUTDIRPATH: ${context.cfg.droidmateOutputDirPath}")
			File("${context.cfg.droidmateOutputDirPath}\\model\\$packageName\\states\\masterMap.txt").printWriter().use { out ->
				var i = 1
				var dwCount = 0.toFloat()
				var mpCount = 0.toFloat()
				masterMap.forEach {
					out.println("__ ${i.toString().padStart(3)}. Data: ${it.key}")
					out.println("__      LABL: ${it.value}")
					out.println("\n")
					dwCount++
					if (it.value.text.toLowerCase() != "empty") {
						mpCount++
					}
					i++
				}

				out.println("\n")
				out.println("Data Widget Counts(DC): ${dwCount}")
				out.println("Matched Pair Counts(MC): ${mpCount}")
				out.println("Matching Ratio (MC/DC): ${mpCount / dwCount}")
				out.println("\n")
				out.println("tp=")
				out.println("tn=")
				out.println("fp=")
				out.println("fn=")
				out.println("\n")
				out.println("Precision (Correctly matched labels [tp/tp+fp]): (Manaully filled)")
				out.println("Recall (#of labels which were not matched [tp/tp+fn]): (Manually filled)")
			}

			File("${context.cfg.droidmateOutputDirPath}\\model\\$packageName\\states\\masterMap3.txt").printWriter().use { out ->


				masterMapWithID.toSortedMap().forEach {
					out.println("StateID: ${it.key}")
					it.value.forEach {
						out.println("__ Data: ${it.key} === Parent: ${it.key.parentId}")
						out.println("__ LABL: ${it.value} === Parent: ${it.value.parentId}")
						dataWidgetCount++
						if (it.value.text.toLowerCase() != "empty") {
							matchedPairCount++
						}
					}
					out.println("\n")
				}
				out.println("\n")
				out.println("Data Widget Counts(DC): ${dataWidgetCount}")
				out.println("Matched Pair Counts(MC): ${matchedPairCount}")
				out.println("Matching Ratio (MC/DC): ${matchedPairCount.toFloat() / dataWidgetCount.toFloat()}")
				out.println("\n")
				out.println("tp=")
				out.println("tn=")
				out.println("fp=")
				out.println("fn=")
				out.println("\n")
				out.println("Precision (Correctly matched labels [tp/tp+fp]): (Manaully filled)")
				out.println("Recall (#of labels which were not matched [tp/tp+fn]): (Manually filled)")
			}


			if (this.stateIDtoFillingValuesPairList.isNotEmpty()) {
				File("${context.cfg.droidmateOutputDirPath}\\model\\$packageName\\states\\linkFilledMap.txt").printWriter().use { out ->
					var i = 0
					var totalTimesOfFilling = 0
					var totalTimesOfMultipleFillings = 0
					this.stateIDtoFillingValuesPairList.forEach { entry ->
						// DONT USE stateIDtoFillingValuesMap.toSortedMap as it will mess up the order for evaluation that is using listOfInt[i]

						//var duplicate = false
						val intQuad = this.listOfInt[i] //Gotten from TextInsertion.kt
						val numDWL = intQuad.first
						val numDWV = intQuad.second
						val alreadyQueriedButNoValue = intQuad.third
						val numOfDumplicates = intQuad.fourth

						if (numDWV > 0) {

							out.println("StateID: ${entry.first}")
							out.println("The DataWidgets with values to fill are displayed below: ")
							entry.second.forEach {
								out.println("__ Data:  ${it.first.text} -- ${it.first}")
								out.println("__ Label: ${masterMap.filter { dataWidget -> dataWidget.key.uid == it.first.uid }.values.first().text} --  ${masterMap.filter { dataWidget -> dataWidget.key.uid == it.first.uid }.values.first()} ")
								out.println("__ Value: ${it.second}")
								totalTimesOfFilling++
							}
							out.println("\n")
							val ratio = (numDWV.toFloat() / numDWL.toFloat())
							val ratio2 = (numDWV.toFloat() / (numDWL.toFloat() - alreadyQueriedButNoValue.toFloat()))

							if (numDWV > 1) {
								out.println("**************** semantically coherent (Manually filled):")
								out.println("\n")
								totalTimesOfMultipleFillings++
							}


							out.println("#numDWL: DataWidgets matched with a label in the current state  : $numDWL")
							out.println("#numQNV: DataWidgets previously queried with no returning values: $alreadyQueriedButNoValue ")
							out.println("#numDWV: DataWidgets with value to fill in the current state    : $numDWV")
							out.println("#numDup: Duplicate DataWidgets which had values filled before   : $numOfDumplicates")
							out.println("Ratio of numDWV/numDWL           : $ratio")
							out.println("Ratio of numDWV/(numDWL - numQNV): $ratio2")
							out.println("-----------------------------------------------------\n")

						}
						i++
					}

					//this.uniqueNumDWL = this.uniqueNumDWL - this.bannedLabelMasterList.size
					out.println("-----------------------------------------------------")
					out.println("\n\n")
					out.println("Final calculations: ")
					out.println("Banned labels found during this run: ${this.bannedLabelMasterList}")

					out.println("Total times of data widgets being filled (Not Unique)        : $totalTimesOfFilling")

					out.println("Total #numDWL: Times DataWidgets matched with a label        : ${this.totalNumDWL}")
					out.println("Total #numDWV: Times DataWidgets with value to fill          : ${this.totalNumDWV}")
					out.println("Ratio of (Total #numDWV / Total #numDWL)                     : ${this.totalNumDWV.toFloat() / this.totalNumDWL.toFloat()}")
					out.println("Total #numDWL: Times DataWidgets matched with a label - banned labels   : ${this.totalNumDWL} - ${this.bannedLabelMasterList.size} = ${this.totalNumDWL - this.bannedLabelMasterList.size}")
					out.println("Total Ratio of (Total #numDWV / (Total #numDWL - numOfBannedLabels))    : ${(this.totalNumDWV.toFloat() / (this.totalNumDWL.toFloat() - bannedLabelMasterList.size.toFloat()))}")

					out.println("\n")
					out.println("Unique #numDWL: Unique DataWidgets matched with a label      : ${this.uniqueNumDWL}")
					out.println("Unique #numDWV: Unique DataWidgets with value to fill        : ${this.uniqueNumDWV}")
					out.println("Total Ratio of (Unique #numDWV / Unique #numDWL)             : ${(this.uniqueNumDWV.toFloat() / this.uniqueNumDWL.toFloat())}")
					out.println("Unique #numDWL: Unique DataWidgets matched with a label - banned labels   : ${this.uniqueNumDWL} - ${this.bannedLabelMasterList.size} = ${this.uniqueNumDWL - this.bannedLabelMasterList.size}")
					out.println("Total Ratio of (Unique #numDWV / (Unique #numDWL - numOfBannedLabels))    : ${(this.uniqueNumDWV.toFloat() / (this.uniqueNumDWL.toFloat() - bannedLabelMasterList.size.toFloat()))}")
					out.println("\n")
					out.println("tp= ")
					out.println("tn= ")
					out.println("fp= ")
					out.println("fn= ")
					out.println("\n")
					out.println("Precision (DataWidget with correctly filled values [tp/tp+fp]) (Manually filled): ")
					out.println("Recall (#of DataWidgets which were not matched     [tp/tp+fn]) (Manually filled): ")
					out.println("\n")
					out.println("Number of DataWidget which has syntactically correct input value #numSyntValid (Syntactically Correct): ")
					out.println("Ratio of (#numSyntValid/#numDWV)                                                                      : ")
					out.println("Number of DataWidget which has meaningful input value #numSemValid             (Semantically Valid   ): ")
					out.println("Ratio of (#numSemValid/#numDWV)                                                                       : ")
					out.println("\n")
					out.println("A: Times a state has more than one value to fill (Manually filled)                                            : $totalTimesOfMultipleFillings")
					out.println("B: Times the values to fill are semantically coherent when there are multiple values to fill (Manually filled):")
					out.println("Ratio of times that values to fill are semantically coherent (B/A) (Manually filled)                          : ")
					out.println("\n\n")
					out.println("For stats only----------------------------")
					out.println("Number of the occurrence of no noun in the label: ${this.numOfNonounMaster} ")

					//stateIDtoFillingValuesMap.clear()
				}
			}
			//tempDel Debug
			println(randListPair)
			println("WidgetsMap and UniqueMap Equality Test: ${widgetsMapAndUniqueMapEqualTest}")
			//println(this.mapOfListOfStringToQueriesAndResults)
			masterMap.clear()
			masterMapWithID.clear()
			masterMapText.clear()
			mapOfListOfStringToQueriesAndResults.clear()
			mapOfDataWidgetUIDtoValueMaster.clear()
			mapODWTV.clear()
			matchedOnlyMapGlobal.clear()
			notFilledMapGlobal.clear()
		}
	}

	/*****************************************************************
	 * FUNCTIONS FOR DataWidget-to-LabelWidget MATCHING.
	 *****************************************************************/

	private fun mapWidgets(state: StateData): MutableMap<Widget, Widget>{
		// Paper 1 implementation
		// Clone the list to prevent concurrent access error
		val uiWidgets = state.widgets.toList()
		val wMap: Map<Widget, Widget> = emptyMap()
		val widgetsMap: MutableMap<Widget, Widget> = wMap.toMutableMap()


		//TODO:DONE Implement the dataWidget or its text to be the original if it already exists in the masterMap before processing.
		uiWidgets.forEach{ dataWidget ->

			var dataWidget = dataWidget //name has to be shadowed. DO NOT DELETE.
			// the data widget must have a text field && its uid must not already exist in masterMap
			if (dataWidget.visible && dataWidget.className.toLowerCase()
							.contains("edittext") && dataWidget.canBeActedUpon //FIXME: Debug In Progress (added dataWidget.canBeActedUpon ws
					&& !uidExistsInMasterMap(dataWidget.uid, masterMap, false)) {

				// This part copies the instance of the DataWidget if it already exists in the masterMap from the masterMap. (Mainly for it's original text value)
				/* TODO: Check if needs to uncomment.
				if (uidExistsInMasterMap(dataWidget.uid, masterMap, true)) {
					val existingDataWidget: Widget? = masterMap.filter { alreadyMatched -> alreadyMatched.key.uid == dataWidget.uid }.entries.first().key
					if (existingDataWidget != null) {
						dataWidget = existingDataWidget
					}
				}
				*/
				// This part addresses Android peculiarity
				//getNouns() here is to check that the text present contains noun to be used as label, otherwise the label is better obtained via another label widget.
				if (/*it.contentDesc != "" ||*/ dataWidget.text != "" && getNouns(dataWidget.text).isNotEmpty()){ //has to be an attribute of it describing the widget
					widgetsMap[dataWidget] = dataWidget
				}
				// This part uses guessDescription as intended by Mariani's paper mapping a label widget to a textfield.
				else {
					//widgetsWithText filters out noisy widgets that doesn't have either text or **content description**
					val widgetsWithText =
							state.widgets.filter { it.text.trim().isNotEmpty() || it.contentDesc.trim().isNotEmpty() }
					var bestWidget = guessDescription(dataWidget, widgetsWithText)
					if (bestWidget != null ){
						widgetsMap[dataWidget] = bestWidget
					}
					else {
						bestWidget = this.nullWidget
						widgetsMap[dataWidget] = bestWidget
					}
				}
			}
		}
		/**This part checks the Master Map if there exists already a key widget with a uid then do nothing,
		else it adds the widget to the master map. The purpose is to add the widget as a key as it first occurs,
		when the text value is still the correct hint and not after it has been changed to something else.*/
		val uniqueWidgetMap = widgetsMap
				.filter { !uidExistsInMasterMap(it.key.uid, masterMap, true) }.toMutableMap()

		if(widgetsMap.isNotEmpty() && uniqueWidgetMap.isNotEmpty()) {//if(widgetsMap.isNotEmpty() && masterMap.isNotEmpty() && uniqueWidgetMap.isNotEmpty()) {

			masterMap.putAll(uniqueWidgetMap)
			val stateIDString = "${state.stateId.first}_${state.stateId.second}"
			masterMapWithID[stateIDString] = uniqueWidgetMap

		}

		//tempDel Debug
		widgetsMapAndUniqueMapEqualTest.add((widgetsMap == uniqueWidgetMap))

		/*else if(widgetsMap.isNotEmpty() && uniqueWidgetMap.isNotEmpty()){
			masterMap = uniqueWidgetMap
			val stateIDString = "${state.stateId.first}_${state.stateId.second}"
			masterMapWithID[stateIDString] = uniqueWidgetMap

		}*/
		return widgetsMap
	}

	private fun uidExistsInMasterMap(uid: UUID, masterMap: MutableMap<Widget, Widget>, use: Boolean): Boolean{
		if (use == false){
			return false
		}
		if (masterMap.isEmpty()){
			return false
		}
		else {
			val uidExists = masterMap.filter { it.key.uid == uid }
			if(uidExists.isNotEmpty()){
				return true
			}
		}
		return false
	}

	private fun guessDescription(dataWidget: Widget, widgetWithText: List<Widget>): Widget?{
		// opts are options for fine-tuning the algorithm (Updated: all opts are removed. Only optLocalSearch is embedded.)

		var min: Int = Int.MAX_VALUE
		var bestWidget : Widget? = null // the best label widget for the data widget description

		widgetWithText.forEach point@{ labelCandidate ->
			if (!isCandidate(dataWidget, labelCandidate)){
				return@point //continue the loop without considering the code in the loop below.
			}
			//select the closest descriptive widget
			val dist: Int = computeDistance(labelCandidate, dataWidget).toInt()

			if (dist < min){
				min = dist
				bestWidget = labelCandidate
			}
		}

		/* If no descriptor is found, this will add the description of the container as a candidate
        to describe the data widget. */
		if (min == Int.MAX_VALUE) {
			return null // returns null when bestWidget is not found
		}
		else {
			return bestWidget
		}
	}

	private fun isCandidate(dataWidget: Widget, currentWidget: Widget): Boolean {
		//TODO: Add a check that the text attribute contains no longer than 3 or 5 words, otherwise not a label.

		if (!currentWidget.className.toLowerCase().contains("view")){
			return false
		}

		//if ((dataWidget.bounds.x < currentWidget.bounds.x) || (dataWidget.bounds.y < currentWidget.bounds.y)){

		if(currentWidget.className.toLowerCase().contains("edittext")){
			return false
		}

		if ((computePosition(dataWidget)[0] < currentWidget.bounds.x)){ //|| computePosition(dataWidget)[1] < currentWidget.bounds.y) {
			return false // Proximity Principle: skips widgets that are outside the interesting area of dataWidget
		}

		//This checks if dataWidget and currentWidget belong to the same container (Having the same parentID)
		// can use dataWidget.parentID == currentWidget.parentID ?? Positively sure.

		if (dataWidget.parentId != null && currentWidget.parentId != null) {
			if (dataWidget.parentId?.first != currentWidget.parentId?.first) {
				return false // ignore descriptive widgets in other containers
			}
		}
		else {
			return false
		}

		//To make sure that the label candidate contains at least one descriptive Noun.
		//if(getNouns(currentWidget.text).isEmpty()){
		//	return false
		//}
		return true
	}

	// function for computing the distance between two widgets' representative points
	// If the candidate widget is lower than the data widget, apply top right as rep point. (x+width, y)
	// Else apply buttom left as rep point. (x, y+height)
	private fun computeDistance(candidateWidget: Widget, dataWidget: Widget): Double{
		val dataWidgetRepPoint: MutableList<Double> =
				mutableListOf(dataWidget.bounds.x.toDouble(), dataWidget.bounds.y.toDouble()) // topleft coord [x, y]

		val candidateWidgetPos = computePosition(candidateWidget)
		//val dataWidgetPos = computePosition(dataWidget)
		var candidateWidgetRepPoint: MutableList<Double> = mutableListOf(0.toDouble(), 0.toDouble())

		if(candidateWidget.bounds.y >= dataWidget.bounds.y) {
			val candidateX = (candidateWidget.bounds.x + candidateWidget.bounds.width).toDouble() //top right
			val candidateY = candidateWidget.bounds.y.toDouble() //top right
			candidateWidgetRepPoint = mutableListOf(candidateX, candidateY)
		}
		else {
			val candidateX = candidateWidget.bounds.x.toDouble() // buttom left
			val candidateY = (candidateWidget.bounds.y + candidateWidget.bounds.height).toDouble() //buttom left
			candidateWidgetRepPoint = mutableListOf(candidateX, candidateY)
		}

		val dist: Double = abs( //distance calculation sqRoot(pow2(x1-x2) + pow2(y1-y2))
				Math.sqrt(
						(Math.pow(candidateWidgetRepPoint[0]-dataWidgetRepPoint[0], 2.toDouble())) +
								(Math.pow(candidateWidgetRepPoint[1]-dataWidgetRepPoint[1], 2.toDouble()))
				)
		)
		return dist
	}

	// A helper function for the local computeDistance function. It computes the position of a widget(Center of Widget).
	private fun computePosition(widget: Widget): MutableList<Double>{
		val wX: Double = widget.bounds.x.toDouble()
		val wY: Double = widget.bounds.y.toDouble()
		val wHeight: Double = widget.bounds.height.toDouble()
		val wWidth: Double = widget.bounds.width.toDouble()
		val pos = mutableListOf( (wX + (wWidth/2)), (wY + (wHeight/2)))
		return pos //returns a list of x as first member and y as the second member [x,y]
	}

	/*****************************************************************
	 * END FUNCTIONS FOR DataWidget-to-LabelWidget MATCHING.
	 *****************************************************************/

	/*****************************************************************
	 * FUNCTIONS FOR LINK.
	 *****************************************************************/

	private fun getMapOfDataWidgetUIDtoValue(widgetsMap: MutableMap<Widget,Widget>): MutableMap<UUID,String>{

		/** DONE:SAVE THE QUERIES and its values PERSISTENTLY so it doesn't have to query on dbpedia everytime. Should be much faster. */

		val matchedOnlyMap = widgetsMap.filterValues { it.text.toLowerCase() != "empty" }.toMutableMap()

		this.notFilledMapGlobal.clear()

		/**
		 * notFilledMapGlobal is cleared on every OnNewAction().
		 * notFilledMapGlobal is a filtered version of matchedOnlyMap, where only DataWidgets To LabelWidget pair
		 * would remain if it the DataWidget's text and UID are the same of its counterpart in the masterMap.
		 * It uses masterMapText which has the DataWidget as key and its original text as value.
		 */
		matchedOnlyMap.forEach{ matchedOnlyEntry ->
			val matchedDW = matchedOnlyEntry.key
			val matchedLW = matchedOnlyEntry.value
			masterMapText.forEach { DWtoTextEntry ->
				if (matchedDW.uid == DWtoTextEntry.key.uid){
					if (matchedDW.text == DWtoTextEntry.key.text){
						this.notFilledMapGlobal[matchedDW] = matchedLW
					}
				}
			}
		}

		/**DONE: use the notFilledMap to extract the listOfStringForQuery to be used as input, instead of the now matchedOnlyMap (Think of the consequences, check code). */
		/** Purpose is to increase values to be filled because already filled fields won't be considered. Thus reducing the need to query many string which often returns no result
		 *  Clarification: Often a list of string for query X can return fewer results than a list of string for query Y,
		 *  where Y is a subset of X. [This shouldn't be how it works, but Link by Mariani works this way.
		 *  Our implementation in this snippet solves this problem.]*/

		this.numOfMatchedDataWidgets = matchedOnlyMap.size
		this.matchedOnlyMapGlobal.putAll(matchedOnlyMap)
		// This part transform the labels' text from myMap into queries using Link.
		if (widgetsMap.isNotEmpty() && this.notFilledMapGlobal.isNotEmpty()) { //if (myMap.isNotEmpty() && matchedOnlyMap.isNotEmpty()) {
			// This is a map of uid and its text label of all matched LabelWidgets in this UI.
			var mapOfUIDtoLabelList: MutableMap<UUID,MutableList<String>> = mutableMapOf()
			this.notFilledMapGlobal.forEach {
				// changed from matchedOnlyMap.forEach {
				//TODO: Here in real cases we will use combinatorial analysis instead of first noun. remove the mutableListOf and [0]. (Do later).


				val labelList = getNouns(it.value.text)
				var text = ""
				if(labelList.isNotEmpty()){
					text = labelList[0]
				}
				else {
					text = "nonoun"
				}
				/** Label UID to Label String List */
				mapOfUIDtoLabelList[it.value.uid] = mutableListOf(removeNonAlphabet(text.toLowerCase()))
			}

			var listOfLabelsForQuery = mutableListOf<String>()

			//listOfMapOfUIDtoLabel.forEach{ uidMap ->
			mapOfUIDtoLabelList.entries.forEach{ mapEntry ->
				val uid = mapEntry.key
				val labelList = mapEntry.value

				println("Label UID: $uid")
				println("Label String ------:")
				labelList.forEach{ aWordOfLabel ->
					println(aWordOfLabel)
				}
				println("--------------------")

				val firstNoun = labelList[0]
				//TODO: Here in real cases we will use combinatorial analysis instead of first noun. (Do later)
				listOfLabelsForQuery.add(firstNoun)
			}
			//}

			val mapOfQueryToResults: MutableMap<Query, MutableList<MutableMap<String, String>>>? = mutableMapOf()
			val mapOfKeywordToValue = mutableMapOf<String, String>()

			if(this.mapOfListOfStringToQueriesAndResults.filter{it.key == listOfLabelsForQuery}.isEmpty()) {
				mapOfQueryToResults?.clear()
				/** This is the line that calls Link */
				getQueryAndResults(listOfLabelsForQuery)?.let { mapOfQueryToResults?.putAll(it) }

				/** (*****AND THUS WHY THIS SECTION's CODE IS SO MESSY******).
				 * There's a problem with map copy. It seems a local instance of map doesn't get deleted once it reaches the end
				 * of an iteration. Reusing it in the next iteration would cause additive accumulation of its keys and values.
				 * A way to solve this is to use .clear(). But then another problem occurs if the map is assigned to a new map
				 * variable (e.g. map2 = map1), in this case the map2 is not a newly created instance but it works as if
				 * it has the same reference to memory as that of map1. Thus performing map1.clear() would also clear out
				 * the content of map2.
				 *
				 * A way to remedy the problem of the same reference is to create a temporary map instance that then can be
				 * put into the real map variable as desired, using realMap.putAll(tempMap). It seems this way the realMap
				 * will have a different reference to memory than the tempMap. And thus performing tempMap.clear() would
				 * not affect realMap.
				 * (*****AND THUS WHY THIS SECTION's CODE IS SO MESSY******).
				 */

				val kk: MutableMap<MutableList<String>, MutableMap<Query, MutableList<MutableMap<String, String>>>?>  = mutableMapOf()
				kk.clear()
				kk.put(listOfLabelsForQuery, mapOfQueryToResults)//mapOQTR)
				this.mapOfListOfStringToQueriesAndResults.putAll(kk) //(listOfStringForQuery, mapOQTR)// [listOfStringForQuery] = mapOQTR

				if (mapOfQueryToResults != null) {
					mapOfQueryToResults.forEach{ queryAndItsResults ->
						val query = queryAndItsResults.key
						val results = queryAndItsResults.value
						val resultSize = results.size
						val randInt = (0..(resultSize-1)).random() //(0..(resultSize-1)).shuffled().last()
						val randResult = results[randInt]

						randResult.forEach{ mapEntry ->
							mapOfKeywordToValue.put(mapEntry.key, mapEntry.value)
						}
					}
				println("")
				}
			}
			else{
				this.mapOfListOfStringToQueriesAndResults[listOfLabelsForQuery]?.forEach{ queryAndItsResults ->
					val query = queryAndItsResults.key
					val results = queryAndItsResults.value
					val resultSize = results.size
					val randInt = (0..(resultSize-1)).random() //(0..(resultSize-1)).shuffled().last()
					val randResult = results[randInt]

					randResult.forEach{ mapEntry ->
						mapOfKeywordToValue.put(mapEntry.key, mapEntry.value)
					}
				}
			}

			val mapOfSanitizedValues = resultValueSanitizer(mapOfKeywordToValue)
			val mapOfLabelToValue = reverseSynonyms(mapOfSanitizedValues, listOfLabelsForQuery)
			val mapOfDataWidgetUidToValue = mapDataWidgetUIDtoItsValue(notFilledMapGlobal
					,mapOfUIDtoLabelList,mapOfLabelToValue)

			/**DONE Get a randomized value among all the retrieved values. Prepare it to fill back.
			//DONE Create a method to sanitize and format obtained values to be ready as input to DataWidget.
			//DONE Get synonyms of the keys of obtained values, so we can match it back to the original in the
			       listOfStringForQuery (use WordNet.kt getSynonyms())
			//DONE Find a way to link the result back to UID to LabelWidget to DataWidget */

			this.mapODWTV.clear()
			this.mapODWTV.putAll(mapOfDataWidgetUidToValue)

			println("")

			//All these map.clear() is to empty the contents of these maps. Somehow they remain in the next iteration
			//without using the .clear() function.
			matchedOnlyMap.clear()
			mapOfUIDtoLabelList.clear()
			mapOfKeywordToValue.clear()
			mapOfSanitizedValues.clear()
			mapOfLabelToValue.clear()
			mapOfDataWidgetUidToValue.clear()
			//if (mapOfQueriesToResults != null) {
			//	mapOfQueriesToResults.clear()
			//}


			return this.mapODWTV
		}
		else {
			return mutableMapOf()
		}
	}

	/**This function maps the DataWidget that found a matched label, with its uid to its intended value from the query.
	This is the mapping without having to use bi-directional HashMap emphasizing on the last if() condition.*/
	private fun mapDataWidgetUIDtoItsValue(notFilledMap: MutableMap<Widget,Widget>,
										   mapOfUIDtoLabelList: MutableMap<UUID, MutableList<String>>,
										   mapOfLabelToValue: MutableMap<String,String>): MutableMap<UUID,String>{
		val mapOfDataWidgetsToValues = mutableMapOf<UUID,String>()
		notFilledMap.forEach point2@{ mapEntry ->
			val dataWidget = mapEntry.key
			val labelWidget = mapEntry.value

			mapOfUIDtoLabelList.forEach{ mapUidToStringList ->
				val labelUID = mapUidToStringList.key
				val labelString = mapUidToStringList.value[0]

				mapOfLabelToValue.forEach{ mapLabelTextToValue ->
					val keyWord = mapLabelTextToValue.key
					val resultValue = mapLabelTextToValue.value

					if(labelWidget.uid == labelUID && labelString == keyWord){
						mapOfDataWidgetsToValues[dataWidget.uid] = resultValue
						return@point2
					}
				}
			}
		}
		return mapOfDataWidgetsToValues
	}

	//for getQueryFromStrings function, should take in a list of string from labels (those matched with the dataWidgets)
	private fun getQueryAndResults(ListOfString: MutableList<String>)
			: MutableMap<Query, MutableList<MutableMap<String, String>>>?{
		var internalArgs = ListOfString
		val sogliaAssociazioni = 50
		val usoSoglia = false
		val euristica = 2
		val prop = Properties()
		var input: FileInputStream? = null

		FormSolver.endpoint = "http://dbpedia.org/sparql/"

		if (internalArgs.isEmpty()) {
			println("Missing inputs")
			return null
		}
		var etichette = internalArgs.toTypedArray()
		while (etichette.isNotEmpty()) {
			val risolutore = FormSolver(etichette, usoSoglia, sogliaAssociazioni, euristica)
			val etichetteTagliate = risolutore.runProcess(1)
			etichette = etichetteTagliate.toTypedArray()
		}
		var i = 0
		while (i < CommonData.outputQueries.size) {
			val query2 = CommonData.outputQueries[i]
			println("*********************************")
			println("*********************************")
			println("*********************************")
			println(query2)
			++i
		}

		val queriesAndResults: MutableMap<Query, MutableList<MutableMap<String, String>>> = mutableMapOf()
		queriesAndResults.clear()
		queriesAndResults.putAll(CommonData.queryToValuesMap)
		//wNet.getSynonyms("car")
		return queriesAndResults
		//TODO: Move the whole link project into our project instead of being a gradle dependency since I have changed many things. It needs to be so to be able to run this project out of the box.
	}

	fun getValues(widgets:List<Widget>): List<Pair<Widget,String>>{
		val filteredWidgets2: MutableList<Widget> = mutableListOf()

		//Experimentation what if we don't use the first two filters. Result -> Error Caused by: kotlin.KotlinNullPointerException: null at the return statement.
		val filteredWidgets = widgets
				.filter{ dataWidget -> mapOfDataWidgetUIDtoValueMaster.containsKey(dataWidget.uid)}
				.filter { dataWidget -> dataWidget.text != mapOfDataWidgetUIDtoValueMaster[dataWidget.uid]!! }.toMutableList()


		/** FIXME: In this function we can get data widgets from the notFilledMap instead of all actionable widgets, since the mapping using UID is not always correct.
		 *  In that case, we don't need all these filters. We could also get DataWidget-To-Value straight away from the function getMapOfDataWidgetToValue() instead of taking the DataWidgetUid.
		 */

		/** This forEach (filter) is important because it would only allowed dataWidgets which are not filled yet.
		 *  'Not filled yet' is determined by the widget having the same value(text) as in the original masterMap.
		 *  masterMapText maps the DataWidget to its text. (Since it's the masterMap, its text is original).
		 */
		filteredWidgets.forEach{ dataWidget ->
			masterMapText.forEach{ mapEntry ->
				if (mapEntry.key.uid == dataWidget.uid && mapEntry.key.text == dataWidget.text){
					filteredWidgets2.add(dataWidget)
				}
			}
		}

		return filteredWidgets2.map { dataWidget -> Pair(dataWidget, mapOfDataWidgetUIDtoValueMaster[dataWidget.uid]!!) }
		/*
		return widgets
				.filter{ dataWidget -> mapOfDataWidgetUIDtoValue.containsKey(dataWidget.uid)}
				.filter { dataWidget -> dataWidget.text != mapOfDataWidgetUIDtoValue[dataWidget.uid]!! }
				.filter { dataWidget -> dataWidget.text == masterMapText[dataWidget]} //masterMap[dataWidget]?.text ?: String }
				.map { dataWidget -> Pair(dataWidget, mapOfDataWidgetUIDtoValue[dataWidget.uid]!!) }
				*/
	}

	private fun linkEvaluation(valuesToFill: List<Pair<Widget,String>>, currentState: StateData ) {

		val fillingStateID = "${currentState.stateId.first}_${currentState.stateId.second}"
		val listOfUidOfDwToFill: MutableList<UUID> = mutableListOf()
		listOfUidOfDwToFill.clear()
		val matchedUidToNoValueList = mutableListOf<UUID>()
		matchedUidToNoValueList.clear()

		/** This snippet adds the UID of the DataWidgets in to the list of UID of DataWidgets to be filled
		 *  valuesToFill is from getValues(actionableWidgets).
		 *  valuesToFill is a List of Pair of DataWidget to its filling value.
		 * */
		valuesToFill.forEach{ pair ->
			listOfUidOfDwToFill.add(pair.first.uid)
		}

		/**
		 * notFilledMapGlobal is a filtered version of matchedOnlyMapGlobal, where only DataWidgets To LabelWidget pair
		 * would remain if it the DataWidget's text and UID are the same of its counterpart in the masterMap.
		 *
		 * matchedUidToNoValueList is a list of UID of the matched DataWidget but has no value to fill
		 * (queried but no value came of it).
		 */
		this.notFilledMapGlobal.keys.forEach{ notFilledDW ->
			if (notFilledDW.uid !in listOfUidOfDwToFill){
				matchedUidToNoValueList.add(notFilledDW.uid)
			}
		}

		var alreadyQueriedButNoValue = 0
		matchedUidToNoValueList.forEach{ uid ->
			if (uid in this.matchedOnlyMapUidMasterList){
				alreadyQueriedButNoValue++
			}
		}
		this.matchedOnlyMapGlobal.keys.forEach{ dataWidget ->
			this.matchedOnlyMapUidMasterList.add(dataWidget.uid)
			this.matchedOnlyMapUidMasterList = this.matchedOnlyMapUidMasterList.distinct().toMutableList()
		}

		/** This snippet considers the banned labels (email/password) which can be configure in the Model Feature class
		 *  in var bannedLabelList*/
		var numOfBannedLabels = 0
		var numNoNoun = 0
		this.notFilledMapGlobal.values.forEach{ labelWidget ->

			val labelList = getNouns(labelWidget.text)
			var word = ""
			if(labelList.isNotEmpty()){
				word = labelList[0]
			}
			else {
				word = "nonoun"
			}

			if (word == "nonoun"){
				numNoNoun++
				this.numOfNonounMaster++
			}

			val text = removeNonAlphabet(word.toLowerCase())
			if (text in this.bannedLabelList ) {
				this.bannedLabelMasterList.add(text) //TODO: So we have bannedLabelsInRun list and numOfBannedLabels in a state. Apply them.
				this.bannedLabelMasterList = this.bannedLabelMasterList.distinct().toMutableList()
				numOfBannedLabels++
			}
		}

		var subDuplicateList = mutableListOf<Boolean>()

		if (valuesToFill.isEmpty()){
			subDuplicateList = emptyList<Boolean>().toMutableList()
		}
		else {
			valuesToFill.forEach { pair ->
				if (this.valuesToFillMasterList.filter { it.first.uid == pair.first.uid }.isNotEmpty()) {
					subDuplicateList.add(true)
				} else {
					subDuplicateList.add(false)
				}
			}
		}

		val numDuplicates = subDuplicateList.filter { it }.size //size of duplicates only (where it == true)
		val numAlreadyFilled = this.matchedOnlyMapGlobal.size - notFilledMapGlobal.size //used to be xCount
		val numDWL = this.matchedOnlyMapGlobal.size - numAlreadyFilled - numOfBannedLabels
		val numDWV = valuesToFill.size


		this.totalNumDWL = this.totalNumDWL + (this.matchedOnlyMapGlobal.size - numAlreadyFilled - alreadyQueriedButNoValue) //- numOfBannedLabels)//
		this.totalNumDWV = this.totalNumDWV + valuesToFill.size

		this.uniqueNumDWL = this.uniqueNumDWL + (this.matchedOnlyMapGlobal.size - numAlreadyFilled - numDuplicates - alreadyQueriedButNoValue) //- numOfBannedLabels)//
		this.uniqueNumDWV = this.uniqueNumDWV + (valuesToFill.size - numDuplicates)

		this.stateIDtoFillingValuesPairList.add(Pair(fillingStateID, valuesToFill))
		/** listOfInt structure */
		this.listOfInt.add(Quadruple(numDWL, numDWV, alreadyQueriedButNoValue, numDuplicates))
		/** listOfDuplicate structure list of boolean values representing each value to fill whether if it's a duplicate */
		//this.listOfDuplicate.add(subDuplicateList)

		this.valuesToFillMasterList.addAll(valuesToFill)

		/**
		 * Method: we can transfer values here to the Model Feature via link.{var} then write to file in the dump()
		 * Write to file for evaluation:
		 * 1.0 DONE State ID
		 * 1.1 DONE DataWidget and its entered text.
		 * 1.2 DONE Optional: LabelWidget associated with the DataWidget.
		 * 1.3 DONE Optional: Draw a rectangle on each of the DataWidget with the text entered.
		 * 1.4 DONE numDWL: Number of DataWidget matched with a labels
		 * 1.5 DONE numDWV: Number of DataWidget with values to fill.
		 * 1.6 DONE Ratio of numDWV/DWL
		 * 1.7 DONE Total numDWL (unique)
		 * 1.8 DONE Total numDWV (unique)
		 * 1.9 DONE Final Ratio of (Total numDWV/Total numDWL)
		 * 1.7 MANUALLY Number of DataWidget which has syntactically correct input value. (Syntactically Correct)
		 * 1.8 MANUALLY Number of DataWidget which has meaningful input value. (Semantically Valid)
		 * 1.9 MANUALLY Percentage of input values which are meaningfully inter-related. (Semantically Coherent)
		 */

	}

	//This function remap the queried keyWord back to its original form in the listOfStringForQuery: MutableList<String>
	//The keyword is replaced by the original word, while the value is carried over into the new map.
	//This is important because we need the original word to map it back to the UID of the Label Widget.
	private fun reverseSynonyms(mapOfSanitizedValues: MutableMap<String,String>,
								listOfLabelsForQuery: MutableList<String>): MutableMap<String,String>{
		val mapOfReversedSynonyms = mutableMapOf<String, String>()
		mapOfSanitizedValues.forEach point1@{ mapEntry ->
			val keyWord = mapEntry.key
			val value = mapEntry.value

			if (!listOfLabelsForQuery.contains(keyWord.toLowerCase())){
				listOfLabelsForQuery.forEach{ Label ->
					var labelSynonyms = wNet.getSynonyms(Label)
					labelSynonyms.forEach{ synonym ->
						if(synonym.toLowerCase() == keyWord.toLowerCase()){
							mapOfReversedSynonyms[Label] = value
							return@point1 //@point1 returns to the next loop since a synonym is found. Saves time.
						}
					}
				}
			}
			else {
				mapOfReversedSynonyms[keyWord] = value
			}
		}
		return mapOfReversedSynonyms
	}


	/**These two sanitizer functions are used to transform machine readable into human readable string to be parsed into DataWidget.
	the resultValueSanitizer() clean up the key and merge the values, while The valueSanitizer() clean up the values.*/
	private fun resultValueSanitizer(mapOfObtainedValues: MutableMap<String,String>): MutableMap<String,String>{
		val sanitizedMap = mutableMapOf<String,String>()

		mapOfObtainedValues.forEach{ mapEntry ->
			sanitizedMap[mapEntry.key.substringAfter("_").toLowerCase()] = valueSanitizer(mapEntry.value)
		}
		return sanitizedMap
	}

	private fun valueSanitizer(value: String): String{
		val value0 = value.substringBefore("^^")
		val value1 = value0.substringAfterLast("/")
		val value2 = value1.replace("_", " ")
		val value3 = value2.substringBeforeLast("@")
		return value3
	}

	/*****************************************************************
	 * END FUNCTIONS FOR LINK.
	 *****************************************************************/

	/*****************************************************************
	 * FUNCTIONS FOR BUTTON GROUPING
	 *****************************************************************/

	fun getButtonWidgets(widgets: List<Widget>): List<Widget>{

		return widgets
				.filter { widget -> widget.className.toLowerCase().contains("button") }
	}


	/*****************************************************************
	 * END FUNCTIONS FOR BUTTON GROUPING
	 *****************************************************************/

	/*****************************************************************
	 * HELPER FUNCTIONS
	 *****************************************************************/

	private fun removeNonAlphabet(input: String): String{
		var answer = input
		val re = Regex("[^A-Za-z]")
		answer = re.replace(answer, "")
		return answer
	}

	private fun replaceNonAlphabetWithSpace(input: String): String{
		var answer = input
		val re = Regex("[^A-Za-z ]")
		answer = re.replace(answer, " ")
		return answer
	}

	//Three below are not used. Put by Nataniel. Insight later.
	private fun getValueFromQuery(query: String): String{
		// query DBPedia
		return ""
	}
	private val myHash = ConcurrentHashMap<UUID, String>() // counts how often any state was explored
	fun getValue(widget: Widget): String{
		return if (myHash.containsKey(widget.uid))
			getValueFromQuery(myHash[widget.uid]!!)
		else
			""
	}

	//Tagger function from Standford NLP group
	//It takes a string and returns only the nouns to be used as the label for the descriptive widget
	//Done: 1. convert plural to singular, 2. .lowerCase() before tagging, 3. remove duplicates
	private fun getNouns(label: String): MutableList<String> {
		//Path to the tagger
		var labelLowerCase = replaceNonAlphabetWithSpace(label.toLowerCase())

		/** Here are guided tempering which can help increase effectiveness of retrieving the correct semantics
		 *  However, it should be avoided as much as possible since it's not automated, and represents human bias.
		 */

		/** Reason: On apps, 'search' is usually used as a verb but Stanford NLP tags it as a noun */
		labelLowerCase = labelLowerCase.replace("search","")
		/** Reason: WordNet doesn't have a synonym for 'where'. But Thesaurus.com has 'location' as a SYN for it. */
		labelLowerCase = labelLowerCase.replace("where", "location")
		/** Reason: Thesaurus.com has 'song' as a SYN for 'music'. */
		labelLowerCase = labelLowerCase.replace("music", "song")

		val tagged: String = tagger.tagString(labelLowerCase)

		/** Here changed into split first then tag */
		var separatel = tagged.split(" ")
		// Filtering and keeping the members that are nouns only
		//Putting each separated tagged word into a list

		val nounsList: MutableList<String> = separatel.filter {
			it.contains("NN") || it.contains("NP") }.toMutableList()
		//println("NOUNS ONLY ------- $nounsList ------------------")

		//Remove the _NN* tags part to keep the raw words only. To be further parsed into Link
		val nounsListCut: MutableList<String> = emptyList<String>().toMutableList()
		nounsList.forEach{
			nounsListCut.add(it.substringBefore("_"))
		}
		//println("NOUNS CUT ONLY------ $nounsListCut ---------------")

		var singularNouns: MutableList<String> = emptyList<String>().toMutableList()
		nounsListCut.forEach{
			/**Got illegal group reference using hypertino inflector. Changes to Morpha. */
			//var single = English.singular(it) // com.hypertino.inflector.English, converts plural to singular.
			var single = MorphaStemmer.stem(it)
			if (it == "address"){
				single = "address"
			}
			singularNouns.add(single)
		}

		return singularNouns.distinct().toMutableList() //the distinct() method removes duplicates.
		//reference for PenTreeBankConstituents e.g. NN-Noun singular , NNS-Noun Plural, NNP-Proper Noun singular,
		//NNPS-Proper Noun plural, NP-Noun Phrase
		//http://www.surdeanu.info/mihai/teaching/ista555-fall13/readings/PennTreebankConstituents.html#NNP
	}

	private fun drawRectangleOnWidgets(packageName: String,masterMapID: MutableMap<String, MutableMap<Widget, Widget>>){
		println("-------Starts drawing rectangles on the mapped label and data widgets-----")
		masterMapID.forEach{ data ->
			val stateID = data.key
			val subMasterMap = data.value
			var pathName: String = ""
			try {
				pathName = System.getProperty("user.dir") + "\\out\\droidMate\\model\\$packageName\\states\\"
				File("$pathName\\drawn").mkdirs()
			}
			catch (IOException: Exception) {

			}
			//println("DataWidget to Label matching images are being written to $pathName\\drawn")
			subMasterMap.forEach{
				try {
					val filePath = "$pathName$stateID.png"
					val img: BufferedImage = ImageIO.read(File(filePath))
					val g2d = img.createGraphics()
					val labelBounds = it.value.bounds
					val dataBounds = it.key.bounds

					g2d.color = Color.RED
					g2d.stroke = BasicStroke(10f)
					g2d.drawRect(labelBounds.x, labelBounds.y, labelBounds.width, labelBounds.height)

					g2d.color = Color.BLUE
					g2d.stroke = BasicStroke(4f)
					g2d.drawRect(dataBounds.x, dataBounds.y, dataBounds.width, dataBounds.height)

					//draw another using parentID for layout

					val writeName = "$pathName\\drawn\\${stateID}_${it.key.uid}_${it.value.uid}.png"

					if(!File(writeName).exists()) { //checks if the image file has already been written
						ImageIO.write(img, "PNG", File(writeName))
						println("image written to $pathName\\drawn\\${stateID}_${it.key.uid}_${it.value.uid}.png")
					}
					g2d.dispose()
				}
				catch (IOException: Exception){

				}
			}
		}
	}

	private fun drawRectangleOnDataWidgetsWithValues(packageName: String, stateIDtoFillingValuesPairList: MutableList<Pair<String,
			List<Pair<Widget,String>>>>){
		println("-------Starts drawing rectangles on the mapped data widgets and their input values-----")
		stateIDtoFillingValuesPairList.forEach{ data ->
			val stateID = data.first
			val listOfDataWidgetsAndValues = data.second
			var pathName: String = ""
			try {
				pathName = System.getProperty("user.dir") + "\\out\\droidMate\\model\\$packageName\\states\\"
				File("$pathName\\drawnDataWidgetsAndFillingValues").mkdirs()
			}
			catch (IOException: Exception) {

			}
			//println("DataWidget and Filled Value images are being written to $pathName\\drawnDataWidgetsAndFillingValues")
			listOfDataWidgetsAndValues.forEach{
				try {
					val filePath = "$pathName$stateID.png"
					val img: BufferedImage = ImageIO.read(File(filePath))
					val g2d = img.createGraphics()
					//val labelBounds = it.value.bounds
					val dataBounds = it.first.bounds

					//g2d.color = Color.RED
					//g2d.stroke = BasicStroke(10f)
					//g2d.drawRect(labelBounds.x, labelBounds.y, labelBounds.width, labelBounds.height)

					g2d.color = Color.BLUE
					g2d.stroke = BasicStroke(4f)
					g2d.drawRect(dataBounds.x, dataBounds.y, dataBounds.width, dataBounds.height)

					//draw another using parentID for layout

					val writeName = "$pathName\\drawnDataWidgetsAndFillingValues\\${stateID}_${it.first.uid}.png"

					if(!File(writeName).exists()) { //checks if the image file has already been written
						ImageIO.write(img, "PNG", File(writeName))
						println("DataWidget and Filled Value image written to " +
								"$pathName\\drawnDataWidgetsAndFillingValues\\${stateID}_${it.first.uid}.png")
					}
					g2d.dispose()
				}
				catch (IOException: Exception){

				}
			}
		}
	}

	//This function helps in randomizing integers. Mainly for getting a value of the list of queried values.
	private fun ClosedRange<Int>.random() =
			ThreadLocalRandom.current().nextInt((endInclusive + 1) - start) +  start

	/*****************************************************************
	 * END HELPER FUNCTIONS
	 *****************************************************************/

}