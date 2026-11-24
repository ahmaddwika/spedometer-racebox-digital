package com.speedometer.app.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.speedometer.app.databinding.FragmentSettingsBinding
import com.speedometer.app.util.UserPrefs

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val useMph = prefs.getBoolean(KEY_MPH, false)
        binding.swMph.isChecked = useMph
        binding.swMph.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_MPH, checked).apply()
        }

        val cal = UserPrefs.speedCalibration(requireContext())
        binding.sliderCalibration.value = cal * 100f
        binding.tvCalibration.text =
            getString(com.speedometer.app.R.string.settings_calibration_value,
                (cal * 100).toInt())
        binding.sliderCalibration.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val v = (value * 100).toInt() / 100f
                binding.tvCalibration.text =
                    getString(com.speedometer.app.R.string.settings_calibration_value,
                        (v * 100).toInt())
                UserPrefs.setCalibration(requireContext(), v)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val PREFS = "settings"
        const val KEY_MPH = "use_mph"
        const val KEY_CALIBRATION = "calibration"
    }
}
