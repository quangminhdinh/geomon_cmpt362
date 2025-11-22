package geomon.myapplication.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import geomon.myapplication.data.SpeciesRepository

class SpeciesViewModel(private val repo: SpeciesRepository) : ViewModel() {

    fun speciesLiveData(id: String) = repo.speciesById(id).asLiveData()

    val allSpeciesLiveData = repo.allSpecies().asLiveData()
}
