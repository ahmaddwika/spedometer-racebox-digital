package com.speedometer.app.ui.speedometer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.speedometer.app.R
import com.speedometer.app.data.db.Trip
import com.speedometer.app.databinding.FragmentSpeedometerBinding
import com.speedometer.app.gps.GpsService
import com.speedometer.app.gps.GpsStateHolder
import com.speedometer.app.util.Format
import kotlinx.coroutines.launch

class SpeedometerFragment : Fragment() {

    private var _binding: FragmentSpeedometerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeedometerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStart.setOnClickListener {
            GpsService.start(requireContext())
            binding.btnStart.isEnabled = false
            binding.btnStop.isEnabled = true
        }
        binding.btnStop.setOnClickListener {
            saveTripIfAny()
            GpsService.stop(requireContext())
            binding.btnStart.isEnabled = true
            binding.btnStop.isEnabled = false
        }
        binding.btnReset.setOnClickListener {
            GpsService.reset(requireContext())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            GpsStateHolder.state.collect { state ->
                val cal = com.speedometer.app.util.UserPrefs.speedCalibration(requireContext())
                val mph = com.speedometer.app.util.UserPrefs.useMph(requireContext())
                val show = state.smoothedKmh * cal
                val showMax = state.maxSpeedKmh * cal
                binding.tvSpeed.text = Format.speed(show, mph)
                binding.tvMaxSpeed.text = Format.speed(showMax, mph)
                binding.tvDistance.text = Format.distance(state.distanceM, kmUnit = true)
                binding.tvTime.text = Format.elapsed(state.elapsedMs)
                binding.tvAccuracy.text =
                    getString(R.string.gps_accuracy_fmt, state.accuracyM.toInt(), state.satellites)
                binding.tvStatus.text = state.message

                val running = state.running
                binding.btnStart.isEnabled = !running
                binding.btnStop.isEnabled = running
            }
        }
    }

    private fun saveTripIfAny() {
        val st = GpsStateHolder.current()
        if (st.distanceM < 10) return
        val avg = if (st.elapsedMs > 0) {
            (st.distanceM / (st.elapsedMs / 1000.0) * 3.6).toFloat()
        } else 0f
        val app = requireActivity().application as com.speedometer.app.SpeedometerApp
        app.tripRepository.saveTrip(
            Trip(
                startTime = System.currentTimeMillis() - st.elapsedMs,
                endTime = System.currentTimeMillis(),
                durationMs = st.elapsedMs,
                distanceM = st.distanceM,
                maxSpeedKmh = st.maxSpeedKmh,
                avgSpeedKmh = avg
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
