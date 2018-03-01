package net.andrewewhite.cagame

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.stage.Stage
import net.andrewewhite.cagame.agents.logic.RandomAgentLogic
import net.andrewewhite.cagame.agents.logic.ReplicatorAgentLogic
import org.eclipse.collections.api.list.ListIterable
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.primitive.IntLists
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples

fun main(args: Array<String>) {
    Application.launch(SimulationApplication::class.java, *args)
}

class SimulationApplication: Application() {
    private val playerColorMap: ListIterable<Color> = Lists.immutable.of(Color.WHITE, Color.RED, Color.BLUE, Color.GREEN)
    lateinit var imageBuffer: IntArray

    override fun start(primaryStage: Stage) {
        val simulation = setupSimulation()

        primaryStage.title = "Simulation Runner"
        val root = Group()
        val theScene = Scene(root, Color.BLACK)
        primaryStage.scene = theScene

        val canvas = Canvas(1024.0, 1024.0)
        root.children.add(canvas)
        imageBuffer = IntArray(size = simulation.world.width * simulation.world.height)

        object : AnimationTimer() {
            override fun handle(now: Long) {
                simulation.runIteration()
                updateImageBuffer(simulation.world)

                val image = WritableImage(simulation.world.width, simulation.world.height)

                image.pixelWriter.setPixels(
                        0,
                        0,
                        simulation.world.width,
                        simulation.world.height,
                        PixelFormat.getIntArgbInstance(),
                        imageBuffer,
                        0,
                        simulation.world.width)

                canvas.graphicsContext2D.drawImage(image, 0.0, 0.0, canvas.width, canvas.height)
            }
        }.start()

        primaryStage.show()
    }

    private fun setupSimulation(): Simulation {
        return Simulation().apply {
            val agentA = Agent(
                    playerId = 1,
                    hp = 10,
                    coordinate = PrimitiveTuples.pair(10, 10),
                    logic = RandomAgentLogic.Companion::live)

            this.world.addAgent(agentA)

            val agentB = Agent(
                    playerId = 2,
                    hp = 10,
                    coordinate = PrimitiveTuples.pair(this.world.width - 50, this.world.height - 50),
                    logic = ReplicatorAgentLogic.Companion::live)

            this.world.addAgent(agentB)
        }
    }

    fun updateImageBuffer(world: World) {
        var index = 0
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                val agent = world[x, y].agent
                val color = when (agent) {
                    null -> Color.BLACK
                    else -> playerColorMap[agent.playerId - 1]
                }

                imageBuffer[index++] = color
//                        .deriveColor(
//                                0.0,
//                                0.0,
//                                if (agent == null) 1.0 else agent.hp / 10.0,
//                                1.0 )
                        .toInt()
            }
        }
    }

}

private fun Color.toInt(): Int {
    return (255 shl 24) or
            ((this.red   * 255.0).toInt() shl 16) or
            ((this.green * 255.0).toInt() shl 8) or
            ((this.blue  * 255.0).toInt())
}

class Simulation(
        val world: World = World(width = 256, height = 256)) {

    val maxHealth = 10
    val restBonus = 2
    val friendlyHealthModifier = IntLists.immutable.of(0, 0, 0, -1, -2, -3, -4, -5, -6)
    val enemyHealthModifier = IntLists.immutable.of(0, -1, -2, -3, -5, -8, -13, -21, -34)

    fun runIteration() {
        val agents = world.agents.asReversed()

        agents.forEach(::haggleAgentTurn)
        agents.forEach(::clearDynamicVariables)
        agents.forEach(::updateDynamicVariables)
        agents.forEach(::updateState)

        val partition = world.agents.partition { it.hp > 0 }
        world.agents = partition.selected
        partition.rejected.forEach { world[it.coordinate.one, it.coordinate.two].agent = null }
    }

    private fun updateState(agent: Agent) {
        val restingDelta = if (agent.enemyNeighborCount == 0) restBonus else 0
        val friendlyHealthDelta = friendlyHealthModifier[agent.friendlyNeighborCount]
        val enemyHealthDelta = enemyHealthModifier[agent.enemyNeighborCount]

        agent.hp = Math.min(maxHealth, agent.hp + restingDelta + friendlyHealthDelta + enemyHealthDelta)
    }

    private fun clearDynamicVariables(agent: Agent) {
        agent.neighbors.clear()
        agent.friendlyNeighborCount = 0
        agent.enemyNeighborCount = 0
    }

    private fun updateDynamicVariables(agent: Agent) {
        Agent.Action.Direction.values().forEach { direction ->
            val neighbor = world[agent.coordinate.one + direction.dx, agent.coordinate.two + direction.dy].agent ?: return@forEach
            when (neighbor.playerId) {
                agent.playerId -> neighbor.friendlyNeighborCount++
                else -> neighbor.enemyNeighborCount++
            }
        }
    }

    private fun haggleAgentTurn(agent: Agent) {
        val localEnvironment = world.buildLocalEnvironment(
                x = agent.coordinate.one,
                y = agent.coordinate.two)
        val action = agent.logic.invoke(localEnvironment, agent.memory)
        handleAction(agent, action)
    }

    private fun handleAction(agent: Agent, action: Agent.Action) {
        when (action) {
            is Agent.Action.None -> return
            is Agent.Action.Move -> tryMove(agent, action.direction)
            is Agent.Action.Reproduce -> tryReproduce(agent, action.direction)
        }
    }

    private fun tryMove(agent: Agent, direction: Agent.Action.Direction) {
        val newX = agent.coordinate.one + direction.dx
        val newY = agent.coordinate.two + direction.dy
        val cell = world[newX, newY]

        if (!cell.passable || cell.agent != null) { return }

        world[agent.coordinate.one, agent.coordinate.two].agent = null
        agent.coordinate = PrimitiveTuples.pair(newX, newY)
        world[newX, newY].agent = agent
    }

    private fun tryReproduce(agent: Agent, direction: Agent.Action.Direction) {
        if (agent.hp <= 1) { return }

        val newX = agent.coordinate.one + direction.dx
        val newY = agent.coordinate.two + direction.dy
        val cell = world[newX, newY]

        if (!cell.passable || cell.agent != null) { return }

        agent.hp = agent.hp / 2
        val newAgent = Agent(
                playerId = agent.playerId,
                hp = agent.hp / 2,
                coordinate = PrimitiveTuples.pair(newX, newY),
                logic = agent.logic)

        world.addAgent(newAgent)
    }

}