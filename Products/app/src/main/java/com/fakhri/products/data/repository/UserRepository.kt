package com.fakhri.products.data.repository

import android.util.Log
import com.fakhri.products.data.local.db.user.UsersEntity
import com.fakhri.products.data.local.user.UserLocalDataSource
import com.fakhri.products.data.network.response.user.Users
import com.fakhri.products.data.network.response.user.toEntity
import com.fakhri.products.data.network.user.UserRemoteDataSource
import com.fakhri.products.data.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.fakhri.products.domain.IUserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : IUserRepository {
    override fun getUser(id: Int): Flow<Resource<UsersEntity>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(Resource.Success(getData(id)))
            } catch (e: Exception) {
                emit(Resource.Error(e.localizedMessage?: "Unknown Error"))
            }
        }.flowOn(dispatcher)
    }

    override suspend fun addUser(users: UsersEntity): Flow<Resource<UsersEntity>> {
        return flow {
            emit(Resource.Loading())
            try {
                localDataSource.addUser(users)
                emit(Resource.Success(users))
            }catch (e: Exception){
                Log.e("ChangeUser",e.message.toString())
                emit(Resource.Error(e.localizedMessage?: "Unknown Error"))
            }
        }.flowOn(dispatcher)
    }

    override suspend fun resetUser(id: Int): Flow<Resource<Unit>> {
        return flow {
            emit(Resource.Loading())
            try {
                localDataSource.deleteUser(id)
                emit(Resource.Success(Unit))
            }catch (e: Exception){
                Log.e("ResetUser",e.message.toString())
                emit(Resource.Error(e.localizedMessage?: "Unknown Error"))
            }
        }.flowOn(dispatcher)
    }

    override fun getUserFromDB(id: Int): Flow<Resource<UsersEntity>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(Resource.Success(localDataSource.getUser(id)))
            } catch (e: Exception) {
                Log.i("DBProducts", e.message.toString())
                emit(Resource.Error(e.localizedMessage?: "Unknown Error"))
            }
        }.flowOn(dispatcher)
    }

    private suspend fun getData(id: Int): UsersEntity {
        var user: UsersEntity? = null
        try {
            user = localDataSource.getUser(id)
        } catch (e: Exception) {
            Log.i("Products", e.message.toString())
        }
        if (user != null) {
            return user
        } else {
            user = getDataFromAPI(id).toEntity()
            localDataSource.addUser(user)
        }
        return user
    }

    private suspend fun getDataFromAPI(id: Int): Users {
        var users: Users? = null
        try {
            val response = remoteDataSource.getUser(id)
            users = response.body()
        } catch (e: Exception) {
            Log.i("Products", e.message.toString())
        }
        return users!!
    }

}