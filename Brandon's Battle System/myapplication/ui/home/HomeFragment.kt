package geomon.myapplication.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import geomon.myapplication.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import geomon.myapplication.data.Seeder
import geomon.myapplication.data.SpeciesRepository
import geomon.myapplication.data.db.AppDatabase
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        homeViewModel.text.observe(viewLifecycleOwner) { binding.textHome.text = it }

        binding.btnTestBattle.setOnClickListener {
            startActivity(
                android.content.Intent(requireContext(),
                    geomon.myapplication.battle.ui.BattleActivity::class.java)

            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.get(requireContext())
            val repo = SpeciesRepository(db.speciesDao())
            Seeder.run(requireContext(), repo)
            Log.d("GeoMon", "Database transfered")
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
