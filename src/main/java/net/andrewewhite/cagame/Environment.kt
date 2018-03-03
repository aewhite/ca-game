package net.andrewewhite.cagame

import org.eclipse.collections.api.list.ListIterable
import org.eclipse.collections.api.list.MutableList
import org.eclipse.collections.api.list.primitive.MutableByteList
import org.eclipse.collections.api.tuple.primitive.IntIntPair
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.primitive.ByteLists
import org.eclipse.collections.impl.list.mutable.FastList


class World (
        val width: Int,
        val height: Int) {
    val cells: ListIterable<Cell> = FastList.newWithNValues(width * height, { Cell() })
    var agents: MutableList<Agent> = Lists.mutable.empty()
    val outOfBoundsCell = Cell(passable = false, agent = null)

    fun addAgent(agent: Agent) {
        val index = coordinatesToIndex(agent.coordinate.one, agent.coordinate.two) ?: throw IllegalArgumentException()

        cells[index].agent = agent
        agents.add(agent)
    }

    operator fun get(x: Int, y: Int): Cell {
        val index = coordinatesToIndex(x, y) ?: return outOfBoundsCell
        return cells[index]
    }

    private fun coordinatesToIndex(x: Int, y: Int): Int? =
            if (x < 0 || x >= width || y < 0 || y >= height) null
            else width * y + x

    fun buildLocalEnvironment(x: Int, y: Int): LocalEnvironment =
            LocalEnvironment(x = x, y = y, radius = 2, world = this)
}

class LocalEnvironment(
        private val x: Int,
        private val y: Int,
        val radius: Int,
        private val world: World) {

    fun viewCell(dx: Int, dy: Int): CellView {
        if (Math.abs(dx) > radius || Math.abs(dy) > radius) { return world.outOfBoundsCell }
        return world[x + dx, y + dy]
    }
}

interface CellView {
    val passable: Boolean
    val agent: AgentView?
}

data class Cell(
        override var passable: Boolean = true,
        override var agent: Agent? = null) : CellView

interface AgentView {
    val playerId: Int
    val hp: Int
}

class Agent(
        override val playerId: Int,
        override var hp: Int,
        var coordinate: IntIntPair,
        val logic: (LocalEnvironment, MutableByteList) -> Action): AgentView {

    val memory: MutableByteList = ByteLists.mutable.empty()
    val neighbors: MutableList<Agent> = Lists.mutable.empty()
    var friendlyNeighborCount: Int = 0
    var enemyNeighborCount: Int = 0

    sealed class Action {
        enum class Direction(val dx: Int, val dy: Int) {
            NORTH(0, -1),
            NORTH_EAST(1, -1),
            EAST(1, 0),
            SOUTH_EAST(1, 1),
            SOUTH(0, 1),
            SOUTH_WEST(-1, 1),
            WEST(-1, 0),
            NORTH_WEST(-1, -1);

            companion object {
                fun fromDelta(dx: Int, dy: Int): Direction {
                    return when {
                        dx ==  0 && dy == -1 -> NORTH
                        dx ==  1 && dy == -1 -> NORTH_EAST
                        dx ==  1 && dy ==  0 -> EAST
                        dx ==  1 && dy ==  1 -> SOUTH_EAST
                        dx ==  0 && dy ==  1 -> SOUTH
                        dx == -1 && dy ==  1 -> SOUTH_WEST
                        dx == -1 && dy ==  0 -> WEST
                        dx == -1 && dy == -1 -> NORTH_WEST
                        else -> throw IllegalArgumentException()
                    }
                }
            }
        }

        class None: Action()
        data class Move(val direction: Direction): Action()
        data class Reproduce(val direction: Direction): Action()
    }
}

