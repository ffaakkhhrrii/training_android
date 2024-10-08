package com.fakhri.products.data.repository

import app.cash.turbine.test
import com.fakhri.products.data.local.db.product.FavoriteProductEntity
import com.fakhri.products.data.local.db.user.UsersEntity
import com.fakhri.products.data.local.user.UserLocalDataSource
import com.fakhri.products.data.network.response.user.Users
import com.fakhri.products.data.network.response.user.toEntity
import com.fakhri.products.data.network.user.UserRemoteDataSource
import com.fakhri.products.data.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import retrofit2.Response

class UserRepositoryTest{
    private fun <T> BDDMockito.BDDMyOngoingStubbing<T>.willThrowUnchecked(vararg throwables: Throwable) {
        var invocationNumber = 0
        this.willAnswer {
            val throwableIndex = invocationNumber++.coerceAtMost(throwables.size - 1)
            throw throwables[throwableIndex]
        }
    }
    private lateinit var repos: UserRepository
    private val localDataSource: UserLocalDataSource = mock(UserLocalDataSource::class.java)
    private val remoteDataSource: UserRemoteDataSource = mock(UserRemoteDataSource::class.java)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repos = UserRepository(localDataSource, remoteDataSource)
    }

    @After
    fun tearDown(){
        Dispatchers.resetMain()
    }

    @Test
    fun `when addUser should return Loading then Success`() = runTest {
        val user = UsersEntity(
            id = 1,
            firstName = "Fakhri",
            address = "Bumi",
            cardNumber = "123"
        )

        val actual = repos.addUser(user)

        actual.test {
            val loading = awaitItem()
            assert(loading is Resource.Loading)

            val success = awaitItem()
            assert(success is Resource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        verify(localDataSource).addUser(user)
    }

    @Test
    fun `when addUser should return Loading then Error` () = runTest {
        val user = UsersEntity(
            id = 1,
            firstName = "Fakhri",
            address = "Bumi",
            cardNumber = "123"
        )
        val exception = Exception("Unknown Error")
        given(localDataSource.addUser(user)).willThrowUnchecked(exception)
        val actual = repos.addUser(user)

        actual.test {
            val loading = awaitItem()
            assert(loading is Resource.Loading)

            val error = awaitItem()
            assert(error is Resource.Error)
            cancelAndIgnoreRemainingEvents()
        }

        verify(localDataSource).addUser(user)
    }

    @Test
    fun `when resetUser should return Loading then Success` ()= runTest {
        val actual = repos.resetUser(1)

        actual.test {
            val loading = awaitItem()
            assert(loading is Resource.Loading)

            val success = awaitItem()
            assert(success is Resource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        verify(localDataSource).deleteUser(1)
    }

    @Test
    fun `given resetUser should return Loading then Error`()= runTest {
        val exception = Exception("Unknown Error")
        given(localDataSource.deleteUser(1)).willThrowUnchecked(exception)
        val actual = repos.resetUser(1)

        actual.test {
            val loading = awaitItem()
            assert(loading is Resource.Loading)

            val error = awaitItem()
            assert(error is Resource.Error)
            cancelAndIgnoreRemainingEvents()
        }
        verify(localDataSource).deleteUser(1)
    }

    @Test
    fun `when getUser should return Loading then Success`() = runTest {
        val user = Users(
            id = 1,
            firstName = "Fakhri"
        )

        `when`(localDataSource.getUser(1)).thenReturn(user.toEntity())
        `when`(remoteDataSource.getUser(1)).thenReturn(Response.success(user))

        val actual = repos.getUser(1)
        actual.test {
            val loading = awaitItem()
            assert(loading is Resource.Loading)

            val success = awaitItem()
            assert(success is Resource.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given getUser should return Loading then Error`() = runTest {
        val exception = Exception("Unknown Error")
        given(localDataSource.getUser(1)).willThrowUnchecked(exception)
        given(remoteDataSource.getUser(1)).willThrowUnchecked(exception)

        val actual = repos.getUser(1)
        actual.test {
            val loading = awaitItem()
            assert(loading is Resource.Loading)

            val error = awaitItem()
            assert(error is Resource.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getUser should fetch from API and save to local data source if user is not found locally`() = runTest {
        val userId = 1
        val apiUser = Users(id = userId)
        val expectedUser = apiUser.toEntity()

        // Mock local data source to return null
        `when`(localDataSource.getUser(userId)).thenReturn(null)

        // Mock remote data source to return the API user
        `when`(remoteDataSource.getUser(userId)).thenReturn(Response.success(apiUser))

        // Execute the function and verify the flow emissions
        repos.getUser(userId).test {
            assert(awaitItem() is Resource.Loading)
            assert(awaitItem() is Resource.Success)
            awaitComplete()
        }

        // Verify that the API was called and the user was saved to the local data source
        verify(remoteDataSource).getUser(userId)
        verify(localDataSource).addUser(expectedUser)
    }

    @Test
    fun `when getUserFromDB should return Loading then Success`() = runTest {
        val user = Users(
            id = 1,
            firstName = "Fakhri"
        )

        `when`(localDataSource.getUser(1)).thenReturn(user.toEntity())

        val actual = repos.getUserFromDB(1)
        actual.test {
            val loading = awaitItem()
            assert(loading is Resource.Loading)

            val success = awaitItem()
            assert(success is Resource.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given getUserFromDB should return Loading then Error`() = runTest {
        val exception = Exception("Unknown Error")
        given(localDataSource.getUser(1)).willThrowUnchecked(exception)

        val actual = repos.getUserFromDB(1)
        actual.test {
            val loading = awaitItem()
            assert(loading is Resource.Loading)

            val error = awaitItem()
            assert(error is Resource.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}