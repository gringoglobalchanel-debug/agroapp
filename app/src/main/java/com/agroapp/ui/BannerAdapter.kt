package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.Banner

class BannerAdapter(private val banners: List<Banner>) :
    RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivBanner: ImageView = itemView.findViewById(R.id.ivBanner)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvBannerTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvBannerDescription)

        fun bind(banner: Banner) {
            ivBanner.setImageResource(banner.imageRes)
            tvTitle.text = banner.title
            tvDescription.text = banner.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position])
    }

    override fun getItemCount() = banners.size
}