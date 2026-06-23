package com.nexgenflows.limpiador

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nexgenflows.limpiador.databinding.ItemAppBinding

class AppListAdapter(
    private val onUninstallClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppInfo) {
            binding.appIcon.setImageDrawable(item.icon)
            binding.appName.text = item.label
            binding.appSize.text = Formatters.size(item.sizeBytes)
            binding.appLastUsed.text = Formatters.lastUsed(binding.root.context, item.lastUsedMillis)
            binding.btnUninstall.setOnClickListener { onUninstallClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.packageName == newItem.packageName &&
                    oldItem.sizeBytes == newItem.sizeBytes &&
                    oldItem.lastUsedMillis == newItem.lastUsedMillis
        }
    }
}
