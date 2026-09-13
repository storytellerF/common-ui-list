package com.storyteller_f.common_ui_list.db

import com.storyteller_f.common_ui_list.model.Repo
import com.storyteller_f.common_ui_list.model.RepoRemoteKey

class RepoComposite(database: RepoDatabase) :
    CommonRoomDatabase<Repo, RepoRemoteKey, RepoDatabase>(database) {

    override suspend fun clearOld() {
        database.reposDao().clearRepos()
        database.remoteKeyDao().clearRemoteKeys()
    }

    override suspend fun insertRemoteKey(remoteKeys: MutableList<RepoRemoteKey>) {
        database.remoteKeyDao().insertAll(remoteKeys)
    }

    override suspend fun getRemoteKey(id: String) = database.remoteKeyDao().remoteKeysRepoId(id)

    override suspend fun insertAllData(repos: MutableList<Repo>) {
        database.reposDao().insertAll(repos)
    }

    override suspend fun deleteItemBy(d: Repo) {
        database.reposDao().delete(d)
        database.remoteKeyDao().delete(d.remoteKeyId())
    }
}
