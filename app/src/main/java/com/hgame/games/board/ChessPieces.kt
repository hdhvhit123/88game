package com.hgame.games.board

import android.graphics.Canvas
import android.graphics.Paint
import com.hgame.engine.Gfx

/** Hand-drawn vector chess pieces (no font or image assets). */
object ChessPieces {
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    /** type: 1 pawn .. 6 king. */
    fun draw(c: Canvas, type: Int, white: Boolean, cx: Float, cy: Float, size: Float) {
        val fillC = if (white) 0xFFFBF7EE.toInt() else 0xFF2B2D3A.toInt()
        val lineC = if (white) 0xFF3A3A46.toInt() else 0xFF0E0E14.toInt()
        body.color = fillC
        outline.color = lineC
        outline.strokeWidth = size * 0.035f
        val u = size / 100f
        c.save()
        c.translate(cx - 50 * u, cy - 50 * u)
        val p = Gfx.path
        fun shape(build: () -> Unit) { p.reset(); build(); c.drawPath(p, body); c.drawPath(p, outline) }
        fun circle(x: Float, y: Float, r: Float) { c.drawCircle(x * u, y * u, r * u, body); c.drawCircle(x * u, y * u, r * u, outline) }
        fun base() = shape {
            p.moveTo(22 * u, 88 * u); p.lineTo(78 * u, 88 * u); p.lineTo(74 * u, 78 * u); p.lineTo(26 * u, 78 * u); p.close()
        }
        when (type) {
            1 -> {
                shape { p.moveTo(32 * u, 78 * u); p.quadTo(36 * u, 55 * u, 44 * u, 48 * u); p.lineTo(56 * u, 48 * u); p.quadTo(64 * u, 55 * u, 68 * u, 78 * u); p.close() }
                circle(50f, 36f, 13f)
                base()
            }
            2 -> {
                shape {
                    p.moveTo(30 * u, 78 * u); p.lineTo(70 * u, 78 * u)
                    p.quadTo(72 * u, 40 * u, 52 * u, 18 * u); p.lineTo(46 * u, 12 * u); p.lineTo(42 * u, 22 * u)
                    p.quadTo(24 * u, 34 * u, 20 * u, 52 * u); p.lineTo(28 * u, 58 * u); p.lineTo(44 * u, 48 * u)
                    p.quadTo(36 * u, 62 * u, 30 * u, 78 * u); p.close()
                }
                c.drawCircle(40 * u, 32 * u, 3.5f * u, Gfx.fillPaint(if (white) lineC else 0xFFE8E2D0.toInt()))
                base()
            }
            3 -> {
                shape { p.moveTo(34 * u, 78 * u); p.quadTo(30 * u, 58 * u, 40 * u, 48 * u); p.lineTo(60 * u, 48 * u); p.quadTo(70 * u, 58 * u, 66 * u, 78 * u); p.close() }
                shape { p.moveTo(50 * u, 18 * u); p.cubicTo(70 * u, 30 * u, 66 * u, 50 * u, 50 * u, 54 * u); p.cubicTo(34 * u, 50 * u, 30 * u, 30 * u, 50 * u, 18 * u); p.close() }
                c.drawLine(44 * u, 30 * u, 56 * u, 40 * u, outline)
                circle(50f, 14f, 5f)
                base()
            }
            4 -> {
                shape {
                    p.moveTo(30 * u, 78 * u); p.lineTo(34 * u, 40 * u); p.lineTo(66 * u, 40 * u); p.lineTo(70 * u, 78 * u); p.close()
                }
                shape {
                    p.moveTo(26 * u, 40 * u); p.lineTo(26 * u, 18 * u); p.lineTo(36 * u, 18 * u); p.lineTo(36 * u, 26 * u)
                    p.lineTo(45 * u, 26 * u); p.lineTo(45 * u, 18 * u); p.lineTo(55 * u, 18 * u); p.lineTo(55 * u, 26 * u)
                    p.lineTo(64 * u, 26 * u); p.lineTo(64 * u, 18 * u); p.lineTo(74 * u, 18 * u); p.lineTo(74 * u, 40 * u); p.close()
                }
                base()
            }
            5 -> {
                shape {
                    p.moveTo(28 * u, 78 * u); p.lineTo(20 * u, 30 * u); p.lineTo(36 * u, 50 * u); p.lineTo(40 * u, 24 * u)
                    p.lineTo(50 * u, 48 * u); p.lineTo(60 * u, 24 * u); p.lineTo(64 * u, 50 * u); p.lineTo(80 * u, 30 * u)
                    p.lineTo(72 * u, 78 * u); p.close()
                }
                for ((x, y) in listOf(20f to 27f, 40f to 21f, 60f to 21f, 80f to 27f)) circle(x, y, 5f)
                base()
            }
            else -> {
                shape {
                    p.moveTo(28 * u, 78 * u); p.quadTo(16 * u, 46 * u, 34 * u, 40 * u); p.quadTo(44 * u, 38 * u, 50 * u, 48 * u)
                    p.quadTo(56 * u, 38 * u, 66 * u, 40 * u); p.quadTo(84 * u, 46 * u, 72 * u, 78 * u); p.close()
                }
                shape {
                    p.moveTo(46 * u, 44 * u); p.lineTo(46 * u, 26 * u); p.lineTo(38 * u, 26 * u); p.lineTo(38 * u, 18 * u)
                    p.lineTo(46 * u, 18 * u); p.lineTo(46 * u, 9 * u); p.lineTo(54 * u, 9 * u); p.lineTo(54 * u, 18 * u)
                    p.lineTo(62 * u, 18 * u); p.lineTo(62 * u, 26 * u); p.lineTo(54 * u, 26 * u); p.lineTo(54 * u, 44 * u); p.close()
                }
                base()
            }
        }
        c.restore()
    }
}
