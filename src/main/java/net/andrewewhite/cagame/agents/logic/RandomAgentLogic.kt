package net.andrewewhite.cagame.agents.logic

import net.andrewewhite.cagame.Agent
import net.andrewewhite.cagame.LocalEnvironment
import org.eclipse.collections.api.list.primitive.MutableByteList
import java.util.*

class RandomAgentLogic {
    companion object {
        fun live(localEnvironment: LocalEnvironment, memory: MutableByteList): Agent.Action {
            return when (Random().nextInt(3)) {
                0 -> Agent.Action.None()
                1 -> Agent.Action.Move(randomDirection())
                2 -> Agent.Action.Reproduce(randomDirection())
                else -> throw IllegalStateException()
            }
        }

        private fun randomDirection(): Agent.Action.Direction {
            return when (Random().nextInt(8)) {
                0 -> Agent.Action.Direction.NORTH
                1 -> Agent.Action.Direction.NORTH_EAST
                2 -> Agent.Action.Direction.EAST
                3 -> Agent.Action.Direction.SOUTH_EAST
                4 -> Agent.Action.Direction.SOUTH
                5 -> Agent.Action.Direction.SOUTH_WEST
                6 -> Agent.Action.Direction.WEST
                7 -> Agent.Action.Direction.NORTH_WEST
                else -> throw IllegalStateException()
            }
        }
    }
}

