package com.speedometer.app.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.speedometer.app.data.db.Trip
import com.speedometer.app.databinding.ItemTripBinding
import com.speedometer.app.util.Format
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter(
    private val onDelete: (Trip) -> Unit
) : ListAdapter<Trip, TripAdapter.TripViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TripViewHolder(
        private val binding: ItemTripBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(trip: Trip) {
            val date = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                .format(Date(trip.startTime))
            binding.tvDate.text = date
            binding.tvDuration.text = Format.elapsed(trip.durationMs)
            binding.tvDistance.text = Format.distance(trip.distanceM, kmUnit = true)
            binding.tvMax.text = String.format(
                Locale.getDefault(), "%.0f km/h max · %.0f km/h avg",
                trip.maxSpeedKmh, trip.avgSpeedKmh
            )
            binding.btnDelete.setOnClickListener { onDelete(getItem(bindingAdapterPosition)) }
        }
    }

    companion object {
        private val Diff = object : DiffUtil.ItemCallback<Trip>() {
            override fun areItemsTheSame(a: Trip, b: Trip) = a.id == b.id
            override fun areContentsTheSame(a: Trip, b: Trip) = a == b
        }
    }
}
