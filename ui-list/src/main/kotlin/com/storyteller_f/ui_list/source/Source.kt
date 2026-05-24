package com.storyteller_f.ui_list.source

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.room.RoomDatabase
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.database.CommonRoomDatabase
import com.storyteller_f.ui_list.database.RemoteKey
import com.storyteller_f.ui_list.database.SimpleRemoteMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

const val STARTING_PAGE_INDEX = 1

class SimpleSourceRepository<D : Datum<RK>, RK : RemoteKey, DT : RoomDatabase>(
    service: suspend (Int, Int) -> CommonResponse<D, RK>,
    database: CommonRoomDatabase<D, RK, DT>,
    pagingSourceFactory: () -> PagingSource<Int, D>
) {
    @OptIn(ExperimentalPagingApi::class)
    val resultStream: Flow<PagingData<D>> = Pager(
        config = PagingConfig(pageSize = NETWORK_PAGE_SIZE, enablePlaceholders = false),
        remoteMediator = SimpleRemoteMediator(
            service,
            database,
        ),
        pagingSourceFactory = pagingSourceFactory
    ).flow

    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }
}

class SourceHandler<D : Datum<RK>,
    Holder : DataItemHolder,
    RK : RemoteKey,
    DT : RoomDatabase>(
    private val sourceRepository: SimpleSourceRepository<D, RK, DT>,
    private val flowFactory: (Flow<PagingData<D>>) -> Flow<PagingData<Holder>>,
) {
    fun content(scope: CoroutineScope): Flow<PagingData<Holder>> {
        return flowFactory(sourceRepository.resultStream)
            .cachedIn(scope)
    }
}

@Deprecated(
    message = "Use a business ViewModel that owns SourceHandler instead of the shared SimpleSourceViewModel.",
    level = DeprecationLevel.WARNING
)
class SimpleSourceViewModel<D : Datum<RK>,
    Holder : DataItemHolder,
    RK : RemoteKey,
    DT : RoomDatabase>(
    sourceRepository: SimpleSourceRepository<D, RK, DT>,
    flowFactory: (Flow<PagingData<D>>) -> Flow<PagingData<Holder>>,
) : ViewModel() {
    private val handler = SourceHandler(sourceRepository, flowFactory)

    val content: Flow<PagingData<Holder>> = handler.content(viewModelScope)
}

class SourceProducer<RK : RemoteKey,
    D : Datum<RK>,
    Holder : DataItemHolder,
    Database : RoomDatabase,
    Composite : CommonRoomDatabase<D, RK, Database>>(
    val composite: () -> Composite,
    val service: suspend (Int, Int) -> CommonResponse<D, RK>,
    val pagingSourceFactory: () -> PagingSource<Int, D>,
    val flowFactory: (Flow<PagingData<D>>) -> Flow<PagingData<Holder>>,
)

@Deprecated(
    message = "Define a business ViewModel and use SourceHandler inside it.",
    level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
fun <RK : RemoteKey,
    D : Datum<RK>,
    Holder : DataItemHolder,
    Database : RoomDatabase,
    Composite : CommonRoomDatabase<D, RK, Database>, ARG, T> T.source(
    arg: () -> ARG,
    sourceContentProducer: (ARG) -> SourceProducer<RK, D, Holder, Database, Composite>,
) where T : HasDefaultViewModelProviderFactory, T : ViewModelStoreOwner = vm({}) {
    val sourceContent = sourceContentProducer(arg())
    SimpleSourceViewModel(
        SimpleSourceRepository(
            sourceContent.service,
            sourceContent.composite(),
            sourceContent.pagingSourceFactory,
        ),
        sourceContent.flowFactory,
    )
}

@Deprecated(
    message = "Define a business ViewModel and use SourceHandler inside it.",
    level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
fun <RK : RemoteKey,
    D : Datum<RK>,
    Holder : DataItemHolder,
    Database : RoomDatabase,
    Composite : CommonRoomDatabase<D, RK, Database>, T> T.source(
    sourceContent: SourceProducer<RK, D, Holder, Database, Composite>,
) where T : HasDefaultViewModelProviderFactory, T : ViewModelStoreOwner = vm({}) {
    SimpleSourceViewModel(
        SimpleSourceRepository(
            sourceContent.service,
            sourceContent.composite(),
            sourceContent.pagingSourceFactory,
        ),
        sourceContent.flowFactory,
    )
}
