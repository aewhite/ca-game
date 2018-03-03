package net.andrewewhite.cagame.agents.logic

import net.andrewewhite.cagame.Agent
import net.andrewewhite.cagame.LocalEnvironment
import org.eclipse.collections.api.list.ListIterable
import org.eclipse.collections.api.list.primitive.MutableByteList
import org.eclipse.collections.api.tuple.primitive.IntIntPair
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples
import java.util.*
import kotlin.math.sign

class SimpleAgentLogic {
    companion object {

        fun live(localEnvironment: LocalEnvironment, memory: MutableByteList): Agent.Action {
            val regionStats = buildRegionStats(localEnvironment)
            attackAction(regionStats)?.run { return this }
            reproduceAction(localEnvironment, regionStats)?.run { return this }
            return Agent.Action.Move(findLeastCrowdedDirection(regionStats))
        }

        private fun attackAction(
                regionStats: SimpleAgentLogic.RegionStats): Agent.Action? {
            if (regionStats.enemies.isEmpty) { return null }
            if (regionStats.enemyNeighborCount > 0) { return Agent.Action.None() }
            return Agent.Action.Move(findEnemyDirection(regionStats))
        }

        fun reproduceAction(localEnvironment: LocalEnvironment, regionStats: RegionStats): Agent.Action? {
            val self = localEnvironment.viewCell(0, 0).agent!!

            return when {
                self.hp <= 1 -> null
                regionStats.enemyNeighborCount >= 1 -> null
                regionStats.friendlyNeighborCount >= 1 -> null
                else -> Agent.Action.Reproduce(findLeastCrowdedDirection(regionStats))
            }
        }

        private fun findLeastCrowdedDirection(regionStats: RegionStats): Agent.Action.Direction {
            val dx = regionStats.friendlies.injectInto(0, {v: Int, p -> v + p.one})
            val dy = regionStats.friendlies.injectInto(0, {v: Int, p -> v + p.two})

            if (dx == 0 && dy == 0) { return randomDirection() }
            return Agent.Action.Direction.fromDelta(-dx.sign, -dy.sign)
        }

        private fun findEnemyDirection(regionStats: RegionStats): Agent.Action.Direction {
            val dx = regionStats.enemies.injectInto(0, {v: Int, p -> v + p.one})
            val dy = regionStats.enemies.injectInto(0, {v: Int, p -> v + p.two})

            if (dx == 0 && dy == 0) { return randomDirection() }
            return Agent.Action.Direction.fromDelta(dx.sign, dy.sign)
        }

        fun buildRegionStats(localEnvironment: LocalEnvironment): RegionStats {
            val self = localEnvironment.viewCell(0, 0).agent!!
            val r = localEnvironment.radius

            val friendlies = Lists.mutable.empty<IntIntPair>()
            val enemies = Lists.mutable.empty<IntIntPair>()
            var friendlyNeighborCount = 0
            var enemyNeighborCount = 0

            for (dx in -r..r) {
                for (dy in -r..r) {
                    if (dx == 0 && dy == 0) { continue }
                    val cell = localEnvironment.viewCell(dx, dy)

                    if (cell.agent == null) { continue }

                    val delta = PrimitiveTuples.pair(dx, dy)

                    if (cell.agent?.playerId != self.playerId) {
                        if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) { enemyNeighborCount++ }
                        enemies.add(delta)
                    }

                    if (cell.agent?.playerId == self.playerId) {
                        if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) { friendlyNeighborCount++ }
                        friendlies.add(delta)
                    }
                }
            }

            return RegionStats(friendlies, enemies, friendlyNeighborCount, enemyNeighborCount)
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



    data class RegionStats(
            val friendlies: ListIterable<IntIntPair>,
            val enemies: ListIterable<IntIntPair>,
            val friendlyNeighborCount: Int,
            val enemyNeighborCount: Int)
}