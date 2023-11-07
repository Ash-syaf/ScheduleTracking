package com.example.scheduletracking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BusStopAdapter (var mList: List<BusStopData>,
                      private val onBusStopClickListener: (BusStopData) -> Unit
) : RecyclerView.Adapter<BusStopAdapter.BusStopViewHolder>(){
    inner class BusStopViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val logo : ImageView = itemView.findViewById(R.id.logoIv)
        val titleTv : TextView = itemView.findViewById(R.id.titleTv)
        var latitude: Double = 0.0
        var longitude: Double = 0.0

        fun bind(busStopData: BusStopData) {
            logo.setImageResource(busStopData.logo)
            titleTv.text = busStopData.title
            latitude = busStopData.latitude
            longitude = busStopData.longitude
            itemView.setOnClickListener {
                onBusStopClickListener(busStopData)
            }
        }
    }

    fun setFilteredList(mList: List<BusStopData>){
        this.mList = mList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusStopViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.each_item , parent , false)
        return BusStopViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusStopViewHolder, position: Int) {
        holder.bind(mList[position])
    }

    override fun getItemCount(): Int {
        return mList.size
    }

}