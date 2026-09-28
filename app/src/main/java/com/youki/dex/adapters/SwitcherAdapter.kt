package com.youki.dex.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.youki.dex.R

/** A row of the ClauDEX task switcher: one open window. */
data class SwitcherEntry(val taskId: Int, val pkg: String, val name: String, val icon: Drawable)

class SwitcherAdapter(
    context: Context,
    entries: MutableList<SwitcherEntry>,
    private val onClose: (SwitcherEntry) -> Unit
) : ArrayAdapter<SwitcherEntry>(context, R.layout.claudex_switcher_entry, entries) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.claudex_switcher_entry, parent, false)
        val entry = getItem(position) ?: return view
        view.findViewById<ImageView>(R.id.switcher_entry_icon).setImageDrawable(entry.icon)
        view.findViewById<TextView>(R.id.switcher_entry_name).text = entry.name
        view.findViewById<ImageView>(R.id.switcher_entry_close).setOnClickListener { onClose(entry) }
        return view
    }
}
