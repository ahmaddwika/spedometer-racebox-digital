package com.speedometer.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.speedometer.app.SpeedometerApp
import com.speedometer.app.data.db.Trip
import com.speedometer.app.data.repository.TripRepository
import com.speedometer.app.databinding.FragmentHistoryBinding

class HistoryViewModel(private val repository: TripRepository) : ViewModel() {
    val trips = repository.trips
    fun delete(trip: Trip) = repository.deleteTrip(trip.id)
    fun clearAll() = repository.clearAll()

    companion object {
        fun factory(repository: TripRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HistoryViewModel(repository) as T
        }
    }
}

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TripAdapter

    private val viewModel: HistoryViewModel by activityViewModels {
        val app = requireActivity().application as SpeedometerApp
        HistoryViewModel.factory(app.tripRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TripAdapter(onDelete = { viewModel.delete(it) })
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.btnClear.setOnClickListener { viewModel.clearAll() }

        viewModel.trips.observe(viewLifecycleOwner) { trips ->
            adapter.submitList(trips)
            binding.tvEmpty.visibility = if (trips.isEmpty()) View.VISIBLE else View.GONE
            binding.recycler.visibility = if (trips.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
