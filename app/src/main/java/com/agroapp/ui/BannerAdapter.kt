package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.Banner

class BannerAdapter(
    private val banners: List<Banner>,
    private val onBannerClick: (Banner) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivBanner: ImageView = itemView.findViewById(R.id.ivBanner)
        val tvTitle: TextView = itemView.findViewById(R.id.tvBannerTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvBannerDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val banner = banners[position]
        holder.ivBanner.setImageResource(banner.imageRes)
        holder.tvTitle.text = banner.title
        holder.tvDescription.text = banner.description
        holder.itemView.setOnClickListener { onBannerClick(banner) }
    }

    override fun getItemCount() = banners.size
}