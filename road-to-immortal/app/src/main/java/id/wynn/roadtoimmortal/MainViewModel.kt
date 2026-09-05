package id.wynn.roadtoimmortal
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.wynn.roadtoimmortal.data.*
import id.wynn.roadtoimmortal.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class MainViewModel(application:Application):AndroidViewModel(application){
 private val app=application as ImmortalApp
 val data=app.repository.state
 val preferences=app.preferences.flow.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),UserPreferences())
 init {viewModelScope.launch {app.repository.initialize();app.repository.refresh()}}
 fun refresh(){viewModelScope.launch {app.repository.refresh(true)}}
 fun rank(p:RankPosition){viewModelScope.launch {app.preferences.setRank(Road.normalize(p,data.value.catalog?.rankRules?:RankRules()),data.value.catalog?.season?.number)}}
 fun theme(value:String){viewModelScope.launch {app.preferences.setTheme(value)}}
 fun favorite(id:Int){viewModelScope.launch {app.preferences.toggleFavorite(id)}}
}
