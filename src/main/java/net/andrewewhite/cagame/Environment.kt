package net.andrewewhite.cagame

import org.eclipse.collections.api.list.ListIterable
import org.eclipse.collections.api.list.MutableList
import org.eclipse.collections.api.tuple.primitive.IntIntPair
import org.eclipse.collections.impl.factory.Lists
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
        x: Int,
        y: Int,
        radius: Int,
        world: World) {
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
        val logic: (LocalEnvironment) -> Action): AgentView {

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
            NORTH_WEST(-1, -1)
        }

        class None: Action()
        data class Move(val direction: Direction): Action()
        data class Reproduce(val direction: Direction): Action()
    }
}