package com.speedometer.app.ui.racebox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.speedometer.app.R
import com.speedometer.app.databinding.FragmentRaceboxBinding
import com.speedometer.app.gps.GpsService
import com.speedometer.app.gps.GpsStateHolder
import com.speedometer.app.util.Format
import kotlinx.coroutines.launch

class RaceboxFragment : Fragment() {

    private var _binding: FragmentRaceboxBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRaceboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStart.setOnClickListener {
            GpsService.start(requireContext())
        }
        binding.btnStop.setOnClickListener {
            GpsService.stop(requireContext())
        }
        binding.btnReset.setOnClickListener {
            GpsService.reset(requireContext())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            GpsStateHolder.state.collect { state ->
                val cal = com.speedometer.app.util.UserPrefs.speedCalibration(requireContext())
                val mph = com.speedometer.app.util.UserPrefs.useMph(requireContext())
                binding.tvSpeed.text = Format.speed(state.smoothedKmh * cal, mph)
                binding.tvMax.text = Format.speed(state.maxSpeedKmh * cal, mph)
                binding.tvDistance.text = Format.distance(state.distanceM, kmUnit = true)
                binding.tvTime.text = Format.elapsed(state.elapsedMs)
                binding.tvSignal.text = getString(R.string.gps_signal_fmt, state.satellites)

                val running = state.running
                binding.btnStart.isEnabled = !running
                binding.btnStop.isEnabled = running
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
