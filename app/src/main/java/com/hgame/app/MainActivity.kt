package com.hgame.app

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.hgame.engine.Pal
import com.hgame.engine.SoundBank
import com.hgame.games.Catalog
import com.hgame.games.Category
import com.hgame.games.GameInfo
import com.hgame.ui.GameCard
import com.hgame.ui.applySystemBarPadding
import java.text.Normalizer

/** Home screen: search, category filter and the grid of games. */
class MainActivity : Activity() {
    private val d by lazy { resources.displayMetrics.density }
    private lateinit var grid: GridView
    private lateinit var chipRow: LinearLayout
    private lateinit var subtitle: TextView
    private lateinit var soundBtn: TextView
    private val adapter = Adapter()
    private var filter = "all"
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Pal.BG)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(16), dp(4))
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(TextView(this).apply {
            text = "HGames: 88 Offline Games"
            textSize = 24f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Pal.TEXT)
        })
        subtitle = TextView(this).apply {
            textSize = 13f
            setTextColor(Pal.MUTED)
        }
        titles.addView(subtitle)
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        soundBtn = TextView(this).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            background = round(Pal.SURFACE, dp(22).toFloat())
            setOnClickListener {
                Prefs.sound = !Prefs.sound
                SoundBank.enabled = Prefs.sound
                updateSoundIcon()
                if (Prefs.sound) SoundBank.play(com.hgame.engine.Sfx.CLICK)
            }
        }
        header.addView(soundBtn, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header)

        val search = EditText(this).apply {
            hint = "Tìm trò chơi…"
            setHintTextColor(Pal.MUTED)
            setTextColor(Pal.TEXT)
            textSize = 15f
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            background = round(Pal.SURFACE, dp(16).toFloat())
            setPadding(dp(16), dp(11), dp(16), dp(11))
            setCompoundDrawablePadding(dp(8))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { query = s.toString(); refresh() }
            })
            setOnEditorActionListener { v, _, _ ->
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus(); true
            }
        }
        root.addView(search, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), dp(10), dp(16), dp(6)) })

        val scroller = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        chipRow = LinearLayout(this).apply { setPadding(dp(12), dp(6), dp(12), dp(8)) }
        scroller.addView(chipRow)
        root.addView(scroller)

        grid = GridView(this).apply {
            adapter = this@MainActivity.adapter
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            horizontalSpacing = dp(10)
            verticalSpacing = dp(10)
            setPadding(dp(12), dp(4), dp(12), dp(24))
            clipToPadding = false
            selector = android.graphics.drawable.ColorDrawable(0)
            isVerticalScrollBarEnabled = false
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        applySystemBarPadding(root)
        root.viewTreeObserver.addOnGlobalLayoutListener {
            val cols = maxOf(2, (root.width / d / 175).toInt())
            if (grid.numColumns != cols) grid.numColumns = cols
        }
        buildChips()
        updateSoundIcon()
        // Allows opening a game directly (used by automated UI checks): am start ... --es game_id <id>
        intent.getStringExtra(GameActivity.EXTRA_ID)?.let { id -> Catalog.byId(id)?.let { open(it) } }
        if (intent.getBooleanExtra(SelfTest.EXTRA, false)) {
            selfTestIndex = intent.getIntExtra("from", 0)
            runNextSelfTest()
        }
    }

    private var selfTestIndex = -1

    private fun runNextSelfTest() {
        if (selfTestIndex >= Catalog.all.size) { android.util.Log.i(SelfTest.TAG, "done"); return }
        val g = Catalog.all[selfTestIndex]
        android.util.Log.i(SelfTest.TAG, "start ${g.id}")
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(this, GameActivity::class.java).putExtra(GameActivity.EXTRA_ID, g.id).putExtra(SelfTest.EXTRA, true), 7)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7 && selfTestIndex >= 0) { selfTestIndex++; grid.postDelayed({ runNextSelfTest() }, 300) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun updateSoundIcon() { soundBtn.text = if (Prefs.sound) "🔊" else "🔇" }

    private fun buildChips() {
        chipRow.removeAllViews()
        val chips = listOf("all" to "Tất cả", "fav" to "★ Yêu thích", "recent" to "Gần đây") +
            Category.values().map { it.name to it.label }
        for ((key, label) in chips) {
            val selected = key == filter
            chipRow.addView(TextView(this).apply {
                text = label
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (selected) Pal.WHITE else Pal.MUTED)
                background = round(if (selected) Pal.ACCENT else Pal.SURFACE, dp(18).toFloat())
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setOnClickListener { filter = key; buildChips(); refresh(); grid.setSelection(0) }
            }, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(4), 0, dp(4), 0) })
        }
    }

    private fun refresh() {
        val favs = Prefs.favorites()
        val q = fold(query.trim())
        var list: List<GameInfo> = when (filter) {
            "all" -> Catalog.all
            "fav" -> Catalog.all.filter { it.id in favs }
            "recent" -> Prefs.recent().mapNotNull { Catalog.byId(it) }
            else -> Catalog.all.filter { it.category.name == filter }
        }
        if (q.isNotEmpty()) list = list.filter { fold(it.title + " " + it.tagline + " " + it.category.label).contains(q) }
        adapter.items = list
        adapter.favs = favs
        adapter.notifyDataSetChanged()
        subtitle.text = when {
            list.isEmpty() && filter == "fav" -> "Chạm ☆ trên thẻ game để thêm yêu thích"
            list.isEmpty() -> "Không tìm thấy trò chơi"
            else -> "${Catalog.all.size} trò chơi · đang hiện ${list.size}"
        }
    }

    private fun fold(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").replace('đ', 'd')

    private fun open(g: GameInfo) {
        startActivity(Intent(this, GameActivity::class.java).putExtra(GameActivity.EXTRA_ID, g.id))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun dp(v: Int) = (v * d).toInt()

    private fun round(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius
    }

    private inner class Adapter : BaseAdapter() {
        var items: List<GameInfo> = emptyList()
        var favs: Set<String> = emptySet()
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val card = (convertView as? GameCard) ?: GameCard(this@MainActivity).also { c ->
                var downX = 0f; var downY = 0f
                c.setOnTouchListener { v, e ->
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) { downX = e.x; downY = e.y }
                    false.also { v.tag = floatArrayOf(downX, downY) }
                }
                c.setOnClickListener { v ->
                    val g = (v as GameCard).game ?: return@setOnClickListener
                    val pos = v.tag as? FloatArray
                    if (pos != null && v.hitStar(pos[0], pos[1])) {
                        Prefs.toggleFavorite(g.id); refresh()
                    } else open(g)
                }
            }
            val g = items[position]
            card.game = g
            card.favorite = g.id in favs
            card.best = Prefs.best(g.id)?.let { b ->
                if (g.bestMode == com.hgame.engine.BestMode.LOW) "%d:%02d".format(b / 60, b % 60) else b.toString()
            } ?: ""
            card.invalidate()
            return card
        }
    }
}
