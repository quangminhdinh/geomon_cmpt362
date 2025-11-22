package geomon.myapplication.ui.home

import android.content.Intent
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import geomon.myapplication.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import geomon.myapplication.battle.Monster
import geomon.myapplication.battle.ui.BattleActivity
import geomon.myapplication.data.Seeder
import geomon.myapplication.data.SpeciesRepository
import geomon.myapplication.data.db.AppDatabase
import kotlinx.coroutines.delay
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
            startGame()
        }

        lifecycleScope.launch {
            val db = AppDatabase.get(requireContext())
            val repo = SpeciesRepository(db.speciesDao())
            repo.clear()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    //To create a battle encounter,  take player monster object and enemy monster object put them as the
    // I want to pass over the monster object in intent.
    fun startGame() {
        lifecycleScope.launch {
            val db = AppDatabase.get(requireContext())
            val repo = SpeciesRepository(db.speciesDao())
            //repo.clear()
            delay(5)
            Seeder.run(requireContext(), repo)

            val player = Monster.initializeByName(requireContext(), "Molediver")
            val enemy  = Monster.initializeByName(requireContext(), "Electricoon")

            val intent = Intent(requireContext(), BattleActivity::class.java).apply {
                putExtra(BattleActivity.EXTRA_PLAYER_NAME, player.name)
                putExtra(BattleActivity.EXTRA_ENEMY_NAME, enemy.name)
            }

            startActivity(intent)
        }
    }


}
