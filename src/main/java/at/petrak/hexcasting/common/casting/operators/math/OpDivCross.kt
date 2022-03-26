package at.petrak.hexcasting.common.casting.operators.math

import at.petrak.hexcasting.api.spell.ConstManaOperator
import at.petrak.hexcasting.api.spell.Operator.Companion.spellListOf
import at.petrak.hexcasting.api.spell.SpellDatum
import at.petrak.hexcasting.common.casting.CastingContext
import net.minecraft.world.phys.Vec3

object OpDivCross : ConstManaOperator {
    override val argc: Int
        get() = 2

    override fun execute(args: List<SpellDatum<*>>, ctx: CastingContext): List<SpellDatum<*>> {
        val lhs = MathOpUtils.GetNumOrVec(args[0], 1)
        val rhs = MathOpUtils.GetNumOrVec(args[1], 0)

        return spellListOf(
            lhs.map({ lnum ->
                rhs.map(
                    { rnum -> lnum / rnum }, { rvec -> Vec3(lnum / rvec.x, lnum / rvec.y, lnum / rvec.z) }
                )
            }, { lvec ->
                rhs.map(
                    { rnum -> lvec.scale(1.0 / rnum) },
                    { rvec -> lvec.cross(rvec) }
                )
            })
        )
    }
}