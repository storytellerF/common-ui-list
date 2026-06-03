package sample.composite;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import sample.Repo
import sample.RepoRemoteKey
import sample.RepoDatabase;
import com.storyteller_f.ui_list.database.CommonRoomDatabase;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
class RepoComposite(database: RepoDatabase) : CommonRoomDatabase<Repo, RepoRemoteKey, RepoDatabase>(database) {

    override suspend fun clearOld()  {
        database.reposDao().clearRepos();
        database.remoteKeyDao().clearRemoteKeys();
    }

    override suspend fun insertRemoteKey(remoteKeys: MutableList<RepoRemoteKey> ) {
        database.remoteKeyDao().insertAll(remoteKeys);
    }

    override suspend fun getRemoteKey(id: String) : RepoRemoteKey? {
        return database.remoteKeyDao().remoteKeysRepoId(id);
    }

    override suspend fun insertAllData(repos: MutableList<Repo>) {
        database.reposDao().insertAll(repos);
    }

    override suspend fun deleteItemBy(d: Repo) {
        database.reposDao().delete(d);
        database.remoteKeyDao().delete(d.remoteKeyId());
    }

}
