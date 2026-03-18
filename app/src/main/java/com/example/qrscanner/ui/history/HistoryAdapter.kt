package com.example.qrscanner.ui.history

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.qrscanner.R
import com.example.qrscanner.model.HistoryItem
import com.example.qrscanner.model.ScanResultType
import com.example.qrscanner.utils.AccentApplier
import java.text.SimpleDateFormat
import java.util.*

// List items are either a date header or a scan entry
sealed class HistoryListItem {
    data class Header(val dateLabel: String, val groupKey: String) : HistoryListItem()
    data class Entry(val item: HistoryItem) : HistoryListItem()
}

class HistoryAdapter(
    private val context: Context,
    private val onItemClick: (HistoryItem) -> Unit,
    private val onFavoriteToggle: (HistoryItem) -> Unit,
    private val onDelete: (HistoryItem) -> Unit,
    private val onShare: (HistoryItem) -> Unit,
    private val onCopy: (HistoryItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<HistoryListItem>()

    companion object {
        const val VIEW_HEADER = 0
        const val VIEW_ENTRY  = 1
    }

    fun submitList(historyItems: List<HistoryItem>) {
        items.clear()
        // Group by date
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val grouped = historyItems.groupBy { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }
        grouped.entries.sortedByDescending { it.key }.forEach { (key, group) ->
            val dateLabel = dateFormat.format(Date(group.first().timestamp))
            items.add(HistoryListItem.Header(dateLabel, key))
            group.sortedByDescending { it.timestamp }.forEach {
                items.add(HistoryListItem.Entry(it))
            }
        }
        notifyDataSetChanged()
    }

    fun getEntryAt(position: Int): HistoryItem? =
        (items.getOrNull(position) as? HistoryListItem.Entry)?.item

    fun removeAt(position: Int) {
        if (position < 0 || position >= items.size) return
        items.removeAt(position)
        notifyItemRemoved(position)
        // Remove orphan header (header with no following entries)
        if (position < items.size && items[position] is HistoryListItem.Header) {
            if (position == items.size - 1 ||
                items.getOrNull(position + 1) is HistoryListItem.Header) {
                items.removeAt(position)
                notifyItemRemoved(position)
            }
        } else if (position > 0 && items[position - 1] is HistoryListItem.Header) {
            if (position >= items.size || items[position] is HistoryListItem.Header) {
                items.removeAt(position - 1)
                notifyItemRemoved(position - 1)
            }
        }
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is HistoryListItem.Header) VIEW_HEADER else VIEW_ENTRY

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return if (viewType == VIEW_HEADER) {
            val v = inflater.inflate(R.layout.item_history_header, parent, false)
            HeaderVH(v)
        } else {
            val v = inflater.inflate(R.layout.item_history_entry, parent, false)
            EntryVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryListItem.Header -> (holder as HeaderVH).bind(item)
            is HistoryListItem.Entry  -> (holder as EntryVH).bind(item.item)
        }
    }

    // ── ViewHolders ────────────────────────────────────────────────────────

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        fun bind(header: HistoryListItem.Header) {
            tvDate.text = header.dateLabel
        }
    }

    inner class EntryVH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView   = view.findViewById(R.id.ivHistoryIcon)
        private val tvTitle: TextView   = view.findViewById(R.id.tvHistoryTitle)
        private val tvMeta: TextView    = view.findViewById(R.id.tvHistoryMeta)
        private val tvContent: TextView = view.findViewById(R.id.tvHistoryContent)
        private val btnStar: ImageButton = view.findViewById(R.id.btnHistoryStar)
        private val btnMenu: ImageButton = view.findViewById(R.id.btnHistoryMenu)

        fun bind(item: HistoryItem) {
            val timeFormat = SimpleDateFormat("M/d/yy h:mm:ss a", Locale.getDefault())
            val barcodeName = getBarcodeTypeName(item)

            ivIcon.setImageResource(getTypeIcon(item.type))
            AccentApplier.tintImage(ivIcon, context)

            tvTitle.text = item.displayTitle
            tvMeta.text  = "${timeFormat.format(Date(item.timestamp))}, $barcodeName"

            // Content preview — WiFi gets structured display
            tvContent.text = when (item.type) {
                ScanResultType.WIFI -> buildString {
                    item.wifiSsid?.let { append("Network Name: $it\n") }
                    item.wifiEncryption?.let { append("Type: $it\n") }
                    item.wifiPassword?.let { append("Password: $it") }
                }.trimEnd()
                else -> item.rawValue
            }

            // Star
            btnStar.setImageResource(
                if (item.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            if (item.isFavorite) AccentApplier.tintImage(btnStar, context)
            else btnStar.imageTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#99FFFFFF"))

            btnStar.setOnClickListener { onFavoriteToggle(item) }

            // 3-dot menu
            btnMenu.setOnClickListener { v ->
                val popup = PopupMenu(context, v)
                popup.menuInflater.inflate(R.menu.menu_history_item, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_share  -> onShare(item)
                        R.id.action_copy   -> onCopy(item)
                        R.id.action_delete -> onDelete(item)
                    }
                    true
                }
                popup.show()
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun getBarcodeTypeName(item: HistoryItem): String = when (item.type) {
        ScanResultType.URL      -> "QR_CODE"
        ScanResultType.WIFI     -> "QR_CODE"
        ScanResultType.EMAIL    -> "QR_CODE"
        ScanResultType.PHONE    -> "QR_CODE"
        ScanResultType.SMS      -> "QR_CODE"
        ScanResultType.GEO      -> "QR_CODE"
        ScanResultType.CONTACT  -> "QR_CODE"
        ScanResultType.CALENDAR -> "QR_CODE"
        ScanResultType.APP      -> "QR_CODE"
        ScanResultType.TEXT     -> "QR_CODE"
    }

    private fun getTypeIcon(type: ScanResultType): Int = when (type) {
        ScanResultType.URL      -> R.drawable.ic_type_url
        ScanResultType.WIFI     -> R.drawable.ic_type_wifi
        ScanResultType.EMAIL    -> R.drawable.ic_type_email
        ScanResultType.PHONE    -> R.drawable.ic_type_phone
        ScanResultType.SMS      -> R.drawable.ic_type_sms
        ScanResultType.GEO      -> R.drawable.ic_type_location
        ScanResultType.CONTACT  -> R.drawable.ic_type_contact
        ScanResultType.CALENDAR -> R.drawable.ic_type_calendar
        ScanResultType.APP      -> R.drawable.ic_type_app
        ScanResultType.TEXT     -> R.drawable.ic_type_text
    }
}

// ── Swipe-to-delete helper ────────────────────────────────────────────────

class SwipeToDeleteCallback(
    private val adapter: HistoryAdapter,
    private val onSwiped: (HistoryItem, Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val deleteColor = Color.parseColor("#E53935")
    private val background  = ColorDrawable(deleteColor)
    private val paint = Paint().apply { color = Color.WHITE; textSize = 40f; isAntiAlias = true }

    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder) = false

    override fun getSwipeDirs(recyclerView: RecyclerView,
                               viewHolder: RecyclerView.ViewHolder): Int {
        // Only allow swipe on Entry items
        return if (adapter.getItemViewType(viewHolder.adapterPosition) ==
            HistoryAdapter.VIEW_ENTRY) super.getSwipeDirs(recyclerView, viewHolder)
        else 0
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos  = viewHolder.adapterPosition
        val item = adapter.getEntryAt(pos) ?: return
        adapter.removeAt(pos)
        onSwiped(item, pos)
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView,
                              viewHolder: RecyclerView.ViewHolder,
                              dX: Float, dY: Float, actionState: Int, isActive: Boolean) {
        val itemView = viewHolder.itemView
        background.setBounds(
            itemView.right + dX.toInt(), itemView.top,
            itemView.right, itemView.bottom
        )
        background.draw(c)
        // Draw delete text
        val text = "Delete"
        val textX = itemView.right - 180f
        val textY = itemView.top + (itemView.height / 2f) + 14f
        c.drawText(text, textX, textY, paint)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isActive)
    }
}
