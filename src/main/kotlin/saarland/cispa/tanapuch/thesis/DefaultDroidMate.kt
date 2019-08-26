package saarland.cispa.tanapuch.thesis

import org.droidmate.ExplorationAPI
import org.droidmate.command.ExploreCommand
import org.droidmate.configuration.ConfigurationBuilder

class DefaultDroidmate{
    companion object {
        @JvmStatic
        fun main(args: Array<String>){

            val cfg = ConfigurationBuilder().build(args)
            val myStrategies = ExploreCommand.getDefaultStrategies(cfg).toMutableList()
            // Selector -> Choose strategy
            val mySelectors = ExploreCommand.getDefaultSelectors(cfg).toMutableList()

            ExplorationAPI.explore(args, strategies = myStrategies, selectors = mySelectors)
        }
    }
}