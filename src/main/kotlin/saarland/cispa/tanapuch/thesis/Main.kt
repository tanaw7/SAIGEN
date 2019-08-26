package saarland.cispa.tanapuch.thesis

import org.droidmate.ExplorationAPI
import org.droidmate.command.ExploreCommand
import org.droidmate.configuration.ConfigurationBuilder
import org.droidmate.exploration.SelectorFunction
import org.droidmate.exploration.StrategySelector
import java.nio.file.Files
import java.nio.file.Paths


class Main{
	companion object {
		@JvmStatic
		fun main(args: Array<String>){

			var i=0
			while (i<1){
				val args: Array<String> = arrayOf("--Selectors-randomSeed=$i", "--Selectors-actionLimit=10", "--Output-outputDir=./out/saiGen_disabled_runWithSeed$i")
				//val args: Array<String> = arrayOf("--Selectors-randomSeed=$i", "--Selectors-actionLimit=500", "--Output-outputDir=./out/saiGen_enabled_runWithSeed$i")
				val cfg = ConfigurationBuilder().build(args)

				/** *
				 * To get the coverage, run with args: --ExecutionMode-explore=false --ExecutionMode-instrument=true
				 */

				//val cfg = ConfigurationBuilder().build(args)

				val myStrategies = ExploreCommand.getDefaultStrategies(cfg).toMutableList()
				myStrategies.add(TextInsertion())
				myStrategies.add(MeaningfulInputOrder(cfg))

				// Selector -> Choose strategy

				// Remove random
				val mySelectors = ExploreCommand.getDefaultSelectors(cfg).dropLast(1).toMutableList()
				// Add custom selector

				val saigenSelector: SelectorFunction = { eContext, pool, _ ->
					val saigen: SemanticAwareInputGenerator = eContext.getOrCreateWatcher()
					saigen.await()
					val actWidgets = eContext.getCurrentState().actionableWidgets

					//if (!link.alreadyFilled() && link.getValues(eContext.getCurrentState().actionableWidgets).isNotEmpty()) {
					if (saigen.saiGenEnabled && saigen.notFilledMapGlobal.isNotEmpty() && saigen.getValues(actWidgets).isNotEmpty()) {
						//println(pool)
						pool.getFirstInstanceOf(TextInsertion::class.java)
					}
					else if (saigen.saiGenEnabled && actWidgets.filter { it.className.toLowerCase().contains("edittext") }.isNotEmpty()) {//actWidgets.any { it.isEdit }) { //FIXME: MeaningfulInputOrder along with the keyboard always on still mess up the input and captured datawidget original text. Fix and test.
						//println(pool)                     //FIXME: OBSERVATION: BUG. It's the keyboard the fksUP. When there is no more fields to fill, and it tries to click on a button but there's the keyboard overlaying on it. Instead of clicking that button, it clicks the keyboard.
						pool.getFirstInstanceOf(MeaningfulInputOrder::class.java)
					}
					else {
						null
					}
				}
				// Add mySelector priority = size+1 since the priority number must be unique
				mySelectors.add(StrategySelector(mySelectors.size+1, "saarland.cispa.tanapuch.thesis.getMySelector", saigenSelector))
				// Add random priority = size+1 since the priority number must be unique
				mySelectors.add(StrategySelector(mySelectors.size+1, "randomWidget", StrategySelector.randomWidget))

				ExplorationAPI.explore(args, strategies = myStrategies, selectors = mySelectors)
				//ExplorationAPI.instrument()
				i++
			}
		}
	}
}