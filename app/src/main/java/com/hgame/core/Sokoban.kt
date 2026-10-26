package com.hgame.core

/** Sokoban level state and an optional BFS solver (used by tests to validate levels). */
class Sokoban(level: String) {
    val rows: List<String> = level.trimEnd('\n').split("\n")
    val h = rows.size
    val w = rows.maxOf { it.length }
    val wall = BooleanArray(w * h)
    val goal = BooleanArray(w * h)
    val boxes = HashSet<Int>()
    var player = 0

    init {
        for (y in 0 until h) for (x in 0 until w) {
            val ch = rows[y].getOrElse(x) { ' ' }
            val i = y * w + x
            when (ch) {
                '#' -> wall[i] = true
                '.' -> goal[i] = true
                '$' -> boxes.add(i)
                '*' -> { boxes.add(i); goal[i] = true }
                '@' -> player = i
                '+' -> { player = i; goal[i] = true }
            }
        }
    }

    /** Tries to move the player by (dx,dy). Returns 0 no move, 1 walked, 2 pushed a box. */
    fun move(dx: Int, dy: Int): Int {
        val px = player % w; val py = player / w
        val nx = px + dx; val ny = py + dy
        if (nx !in 0 until w || ny !in 0 until h) return 0
        val n = ny * w + nx
        if (wall[n]) return 0
        if (n in boxes) {
            val bx = nx + dx; val by = ny + dy
            if (bx !in 0 until w || by !in 0 until h) return 0
            val b = by * w + bx
            if (wall[b] || b in boxes) return 0
            boxes.remove(n); boxes.add(b); player = n
            return 2
        }
        player = n
        return 1
    }

    fun solved() = boxes.all { goal[it] }

    /** Minimum number of pushes to solve, or -1 (search capped at [limit] states). */
    fun solvePushes(limit: Int = 400_000): Int {
        data class State(val boxes: List<Int>, val reach: Int)
        fun reachable(p: Int, bx: Set<Int>): BooleanArray {
            val seen = BooleanArray(w * h)
            val st = ArrayDeque<Int>(); st.add(p); seen[p] = true
            while (st.isNotEmpty()) {
                val c = st.removeLast()
                for (d in intArrayOf(1, -1, w, -w)) {
                    val n = c + d
                    if (n in 0 until w * h && !wall[n] && n !in bx && !seen[n]) { seen[n] = true; st.add(n) }
                }
            }
            return seen
        }
        fun norm(p: Int, bx: Set<Int>): Int = reachable(p, bx).indexOfFirst { it }
        val start = State(boxes.sorted(), norm(player, boxes))
        val q = ArrayDeque<Pair<State, Int>>()
        val seen = HashSet<State>()
        q.add(start to 0); seen.add(start)
        while (q.isNotEmpty()) {
            val (s, d) = q.removeFirst()
            val bx = s.boxes.toHashSet()
            if (bx.all { goal[it] }) return d
            if (seen.size > limit) return -1
            val reach = reachable(s.reach, bx)
            for (b in s.boxes) for (dir in intArrayOf(1, -1, w, -w)) {
                val from = b - dir; val to = b + dir
                if (from !in 0 until w * h || to !in 0 until w * h) continue
                if (!reach[from] || wall[to] || to in bx) continue
                if (dir == 1 || dir == -1) { if (from / w != b / w || to / w != b / w) continue }
                if (deadCorner(to) && !goal[to]) continue
                val nb = HashSet(bx); nb.remove(b); nb.add(to)
                val ns = State(nb.sorted(), norm(b, nb))
                if (seen.add(ns)) q.add(ns to d + 1)
            }
        }
        return -1
    }

    private fun deadCorner(i: Int): Boolean {
        val up = wall.getOrElse(i - w) { true }; val dn = wall.getOrElse(i + w) { true }
        val lf = wall.getOrElse(i - 1) { true }; val rt = wall.getOrElse(i + 1) { true }
        return (up || dn) && (lf || rt)
    }

    companion object {
        /** Original levels made for this app. */
        val LEVELS = listOf(
            """
            #####
            #@$.#
            #####
            """,
            """
            ######
            #    #
            # $$ #
            #@..##
            #####
            """,
            """
            #######
            #.    #
            #  $  #
            #  @$.#
            #######
            """,
            """
            #######
            #     #
            # $.$ #
            # .@. #
            # $.$ #
            #     #
            #######
            """,
            """
            ########
            #  .   #
            # #$## #
            # $@ . #
            ## $#  #
             # .   #
             #######
            """,
            """
             #####
            ##   ##
            # $ $ #
            #  .  #
            ## . ##
            #  @  #
            #######
            """,
            """
            #######
            #.  ..#
            # $$$ #
            #  @  #
            # $   #
            #.    #
            #######
            """,
            """
            ########
            #  #   #
            #  $ $ #
            ##   ..#
             # @   #
             #######
            """,
            """
            #########
            #       #
            # $ $ $ #
            # .#.#. #
            #   @   #
            #########
            """,
            """
            #######
            #     #
            # *$* #
            #  @  #
            # *.* #
            #     #
            #######
            """,
            """
              #####
            ###   #
            #  $# ##
            # #  . #
            #    # #
            ## #   #
             #@ ###
             #####
            """,
            """
            ##########
            #        #
            # $    $ #
            ## ##.##.#
             #  @   #
             ########
            """,
        ).map { it.trimIndent() }
    }
}
