/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.Utility
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.util.common.AuthenticationTokenTestUtil
import com.facebook.util.common.mockLocalBroadcastManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.security.PublicKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    FacebookSdk::class, AuthenticationTokenCache::class, Utility::class, OidcSecurityUtil::class)
class AuthenticationTokenManagerTest : FacebookPowerMockTestCase() {
  private lateinit var localBroadcastManager: LocalBroadcastManager
  private lateinit var authenticationTokenCache: AuthenticationTokenCache

  private fun createAuthenticationTokenManager(): AuthenticationTokenManager {
    return AuthenticationTokenManager(localBroadcastManager, authenticationTokenCache)
  }

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getApplicationId()).thenReturn(AuthenticationTokenTestUtil.APP_ID)

    // mock and bypass signature verification
    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    PowerMockito.`when`(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("key")
    PowerMockito.`when`(OidcSecurityUtil.getPublicKeyFromString(any()))
        .thenReturn(PowerMockito.mock(PublicKey::class.java))
    PowerMockito.`when`(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)

    authenticationTokenCache = mock()
    localBroadcastManager = mockLocalBroadcastManager(ApplicationProvider.getApplicationContext())
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T107014577
  @Test
  fun testDefaultsToNoCurrentAuthenticationToken() {
    val authenticationTokenManager = createAuthenticationTokenManager()
    assertThat(authenticationTokenManager.currentAuthenticationToken).isNull()
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T107020255
  @Test
  fun testCanSetCurrentAuthenticationToken() {
    val authenticationTokenManager = createAuthenticationTokenManager()
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    authenticationTokenManager.currentAuthenticationToken = authenticationToken
    assertThat(authenticationTokenManager.currentAuthenticationToken).isEqualTo(authenticationToken)
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T107034731, T106984355
  @Test
  fun testLoadReturnsFalseIfNoCachedToken() {
    val authenticationTokenManager = createAuthenticationTokenManager()
    val result = authenticationTokenManager.loadCurrentAuthenticationToken()
    assertThat(result).isFalse
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T107014726
  @Test
  fun testLoadReturnsTrueIfCachedToken() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    whenever(authenticationTokenCache.load()).thenReturn(authenticationToken)
    val authenticationTokenManager = createAuthenticationTokenManager()
    val result = authenticationTokenManager.loadCurrentAuthenticationToken()
    assertThat(result).isTrue
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T107020350
  @Test
  fun testLoadSetsCurrentTokenIfCached() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    whenever(authenticationTokenCache.load()).thenReturn(authenticationToken)
    val authenticationTokenManager = createAuthenticationTokenManager()
    authenticationTokenManager.loadCurrentAuthenticationToken()
    assertThat(authenticationTokenManager.currentAuthenticationToken).isEqualTo(authenticationToken)
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T107011064
  @Test
  fun testSaveWritesToCacheIfToken() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    val authenticationTokenManager = createAuthenticationTokenManager()
    authenticationTokenManager.currentAuthenticationToken = authenticationToken
    verify(authenticationTokenCache, times(1)).save(any())
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T107013115
  @Test
  fun testSetEmptyTokenClearsCache() {
    val authenticationTokenManager = createAuthenticationTokenManager()
    authenticationTokenManager.currentAuthenticationToken = null
    verify(authenticationTokenCache, times(1)).clear()
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T107014675
  @Test
  fun testLoadDoesNotSave() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    whenever(authenticationTokenCache.load()).thenReturn(authenticationToken)
    val authenticationTokenManager = createAuthenticationTokenManager()
    authenticationTokenManager.loadCurrentAuthenticationToken()
    verify(authenticationTokenCache, never()).save(any())
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T107020148
  @Test
  fun testChangingAuthenticationTokenSendsBroadcast() {
    val authenticationTokenManager = createAuthenticationTokenManager()
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    authenticationTokenManager.currentAuthenticationToken = authenticationToken
    val intents = arrayOfNulls<Intent>(1)
    val broadcastReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
          override fun onReceive(context: Context, intent: Intent) {
            intents[0] = intent
          }
        }
    localBroadcastManager.registerReceiver(
        broadcastReceiver,
        IntentFilter(AuthenticationTokenManager.ACTION_CURRENT_AUTHENTICATION_TOKEN_CHANGED))
    val anotherAuthenticationToken =
        AuthenticationTokenTestUtil.getAuthenticationTokenEmptyOptionalClaimsForTest()
    authenticationTokenManager.currentAuthenticationToken = anotherAuthenticationToken
    localBroadcastManager.unregisterReceiver(broadcastReceiver)
    val intent = intents[0]
    assertThat(intent).isNotNull()
    checkNotNull(intent)
    val oldAuthenticationToken: AuthenticationToken =
        checkNotNull(
            intent.getParcelableExtra(AuthenticationTokenManager.EXTRA_OLD_AUTHENTICATION_TOKEN))
    val newAuthenticationToken: AuthenticationToken =
        checkNotNull(
            intent.getParcelableExtra(AuthenticationTokenManager.EXTRA_NEW_AUTHENTICATION_TOKEN))
    assertThat(authenticationToken.token).isEqualTo(oldAuthenticationToken.token)
    assertThat(anotherAuthenticationToken.token).isEqualTo(newAuthenticationToken.token)
  }
}
