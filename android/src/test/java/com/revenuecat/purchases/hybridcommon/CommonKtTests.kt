package com.revenuecat.purchases.hybridcommon


import android.app.Activity
import android.app.Application
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.BillingFeature
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.hybridcommon.mappers.map
import com.revenuecat.purchases.interfaces.Callback
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

internal class CommonKtTests {

    private val mockApplicationContext = mockk<Application>(relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockPurchases = mockk<Purchases>()
    private val mockActivity = mockk<Activity>(relaxed = true)

    @BeforeEach
    fun setup() {
        mockkObject(Purchases)
        every {
            Purchases.configure(
                    context = any(),
                    apiKey = any(),
                    appUserID = any(),
                    observerMode = any(),
                    service = any()
            )
        } returns mockPurchases
        every { mockContext.applicationContext } returns mockApplicationContext
        every { Purchases.sharedInstance } returns mockPurchases
    }


    @Test
    fun `Calling setProxyURLString, sets the proxyURL correctly from a valid URL`() {
        assertEquals(Purchases.proxyURL, null)

        val urlString = "https://revenuecat.com"
        setProxyURLString(urlString)

        assertEquals(Purchases.proxyURL.toString(), urlString)
    }

    @Test
    fun `Calling setProxyURLString, sets the proxyURL to null from a null string`() {
        Purchases.proxyURL = URL("https://revenuecat.com")

        setProxyURLString(null)

        assertEquals(Purchases.proxyURL, null)
    }

    @Test
    fun `Calling setProxyURLString, raises exception if url string can't be parsed into a URL`() {
        assertFailsWith<java.net.MalformedURLException> {
            setProxyURLString("this is not a url")
        }
    }

    @RepeatedTest(5)
    fun `canMakePayments result successfully passed back`() {
        configure(
            context = mockContext,
            apiKey = "api_key",
            appUserID = "appUserID",
            observerMode = true,
            platformInfo = PlatformInfo("flavor", "version")
        )

        val receivedCanMakePayments = Random.nextBoolean()

        var capturedCallback = slot<Callback<Boolean>>()
        every {
            Purchases.canMakePayments(mockContext, listOf(), capture(capturedCallback))
        } answers {
            capturedCallback.captured.also {
                it.onReceived(receivedCanMakePayments)
            }
        }

        val onResult = mockk<OnResultAny<Boolean>>()
        val returnedResult = slot<Boolean>()
        every { onResult.onReceived(capture(returnedResult)) } just runs

        canMakePayments(mockContext,
            listOf(),
            onResult)

        assertEquals(receivedCanMakePayments, returnedResult.captured)
    }

    @Test
    fun `calling canMakePayments with empty list correctly passes through to Purchases`() {
        configure(
                context = mockContext,
                apiKey = "api_key",
                appUserID = "appUserID",
                observerMode = true,
                platformInfo = PlatformInfo("flavor", "version")
        )

        every {
            Purchases.canMakePayments(mockContext, any(), any())
        } just Runs

        val onResult = mockk<OnResultAny<Boolean>>()
        every { onResult.onReceived(any()) } just Runs

        canMakePayments(mockContext,
                listOf(),
                onResult)

        verify(exactly = 1) {
            Purchases.canMakePayments(
                    mockContext,
                    listOf(),
                    any())
        }
    }

    @Test
    fun `canMakePayments correctly maps all integer values to BillingFeature enum type`() {
        configure(
                context = mockContext,
                apiKey = "api_key",
                appUserID = "appUserID",
                observerMode = true,
                platformInfo = PlatformInfo("flavor", "version")
        )

        every { Purchases.canMakePayments(mockContext, any(), any()) } just runs

        val onResult = mockk<OnResultAny<Boolean>>()
        every { onResult.onReceived(any()) } just runs

        val billingFeatureValues = BillingFeature.values()

        billingFeatureValues.forEachIndexed { index, billingFeature ->
            canMakePayments(mockContext,
                    listOf(index),
                    onResult)

            verify(exactly = 1) {
                Purchases.canMakePayments(
                        mockContext,
                        listOf(billingFeature),
                        any())
            }
        }
    }

    @Test
    fun `calling canMakePayments with invalid integer results in error`() {
        configure(
                context = mockContext,
                apiKey = "api_key",
                appUserID = "appUserID",
                observerMode = true,
                platformInfo = PlatformInfo("flavor", "version")
        )

        every { Purchases.canMakePayments(mockContext, any(), any()) } just runs

        val onResultAny = mockk<OnResultAny<Boolean>>()
        every { onResultAny.onError(any())} just runs

        canMakePayments(mockContext,
                listOf(8),
                onResultAny)

        verify(exactly = 1) {
            onResultAny.onError(any())
        }
    }

    @Test
    fun `calling logIn correctly passes call to Purchases`() {
        val appUserID = "appUserID"

        configure(
            context = mockContext,
            apiKey = "api_key",
            appUserID = "appUserID",
            observerMode = true,
            platformInfo = PlatformInfo("flavor", "version")
        )

        every { mockPurchases.logIn(appUserID, any()) } just runs

        logIn(appUserID = appUserID, onResult = object : OnResult {
            override fun onReceived(map: Map<String?, *>?) {}
            override fun onError(errorContainer: ErrorContainer) {}
        })

        verify(exactly = 1) { mockPurchases.logIn(appUserID, any()) }
    }

    @Test
    fun `calling logIn correctly calls onReceived`() {
        val appUserID = "appUserID"

        configure(
            context = mockContext,
            apiKey = "api_key",
            appUserID = "appUserID",
            observerMode = true,
            platformInfo = PlatformInfo("flavor", "version")
        )

        val mockInfo = mockk<PurchaserInfo>(relaxed = true)
        val mockCreated = Random.nextBoolean()

        val logInCallback = slot<LogInCallback>()
        every {
            mockPurchases.logIn(
                newAppUserID = appUserID,
                capture(logInCallback)
            )
        } just runs

        val onResult = mockk<OnResult>(relaxed = true)

        logIn(appUserID = appUserID, onResult = onResult)
        logInCallback.captured.onReceived(mockInfo, mockCreated)

        val mockInfoMap = mockInfo.map()

        verify(exactly = 1) {
            onResult.onReceived(mapOf(
                "created" to mockCreated,
                "purchaserInfo" to mockInfoMap
            ))
        }
    }

    @Test
    fun `calling logIn with error calls onError`() {
        val appUserID = "appUserID"

        configure(
            context = mockContext,
            apiKey = "api_key",
            appUserID = "appUserID",
            observerMode = true,
            platformInfo = PlatformInfo("flavor", "version")
        )

        val mockError = mockk<PurchasesError>(relaxed = true)

        val logInCallback = slot<LogInCallback>()
        every {
            mockPurchases.logIn(
                newAppUserID = appUserID,
                capture(logInCallback)
            )
        } just runs

        val onResult = mockk<OnResult>()
        every { onResult.onReceived(any()) } just runs
        every { onResult.onError(any()) } just runs

        logIn(appUserID = appUserID, onResult = onResult)
        logInCallback.captured.onError(mockError)

        val mockErrorMap = mockError.map()
        verify(exactly = 1) {
            onResult.onError(mockErrorMap)
        }
    }

    @Test
    fun `calling logOut correctly passes call to Purchases`() {
        configure(
            context = mockContext,
            apiKey = "api_key",
            appUserID = "appUserID",
            observerMode = true,
            platformInfo = PlatformInfo("flavor", "version")
        )

        every { mockPurchases.logOut(any()) } just runs

        logOut(onResult = object : OnResult {
            override fun onReceived(map: Map<String?, *>?) {}
            override fun onError(errorContainer: ErrorContainer) {}
        })

        verify(exactly = 1) { mockPurchases.logOut(any()) }
    }

    @Test
    fun `calling logOut correctly calls onReceived`() {
        configure(
            context = mockContext,
            apiKey = "api_key",
            appUserID = "appUserID",
            observerMode = true,
            platformInfo = PlatformInfo("flavor", "version")
        )

        val mockInfo = mockk<PurchaserInfo>(relaxed = true)
        val receivePurchaserInfoListener = slot<ReceivePurchaserInfoListener>()

        every { mockPurchases.logOut(capture(receivePurchaserInfoListener)) } just runs
        val onResult = mockk<OnResult>()
        every { onResult.onReceived(any()) } just runs
        every { onResult.onError(any()) } just runs

        logOut(onResult)

        receivePurchaserInfoListener.captured.onReceived(mockInfo)

        val mockInfoMap = mockInfo.map()
        verify(exactly = 1) { onResult.onReceived(mockInfoMap) }
    }

    @Test
    fun `calling logOut with error calls onError`() {
        configure(
            context = mockContext,
            apiKey = "api_key",
            appUserID = "appUserID",
            observerMode = true,
            platformInfo = PlatformInfo("flavor", "version")
        )

        val mockError = mockk<PurchasesError>(relaxed = true)
        val receivePurchaserInfoListener = slot<ReceivePurchaserInfoListener>()

        every { mockPurchases.logOut(capture(receivePurchaserInfoListener)) } just runs
        val onResult = mockk<OnResult>()
        every { onResult.onReceived(any()) } just runs
        every { onResult.onError(any()) } just runs

        logOut(onResult)

        receivePurchaserInfoListener.captured.onError(mockError)

        val mockErrorMap = mockError.map()
        verify(exactly = 1) { onResult.onError(mockErrorMap) }
    }

    @Test
    fun `getPaymentDiscount returns an error`() {
        val error = getPaymentDiscount()
        assertEquals(PurchasesErrorCode.UnsupportedError.code, error.code)
        assertTrue(error.message.isNotEmpty())
    }

    @Test
    fun `purchaseProduct passes correct productIdentifier after a successful purchase`() {
        configure(
            context = mockContext,
            apiKey = "api_key",
            appUserID = "appUserID",
            observerMode = true,
            platformInfo = PlatformInfo("flavor", "version")
        )
        val expectedProductIdentifier = "product"
        var receivedResponse: MutableMap<String, *>? = null

        val capturedGetSkusResponseListener = slot<GetSkusResponseListener>()
        val mockSkuDetails = mockSubscriptionProduct(expectedProductIdentifier)
        val mockPurchase = mockk<Purchase>()
        every {
            mockPurchase.skus
        } returns ArrayList(listOf(expectedProductIdentifier, "other"))

        every {
            mockPurchases.getSubscriptionSkus(listOf(expectedProductIdentifier), capture(capturedGetSkusResponseListener))
        } answers {
            capturedGetSkusResponseListener.captured.onReceived(listOf(mockSkuDetails))
        }

        val capturedMakePurchaseListener = slot<MakePurchaseListener>()
        every {
            mockPurchases.purchaseProduct(mockActivity, mockSkuDetails, capture(capturedMakePurchaseListener))
        } answers {
            capturedMakePurchaseListener.captured.onCompleted(mockPurchase, mockk(relaxed = true))
        }

        purchaseProduct(
            mockActivity,
            productIdentifier = expectedProductIdentifier,
            oldSku = null,
            prorationMode = null,
            type = "subs",
            onResult = object : OnResult {
                override fun onReceived(map: MutableMap<String, *>?) {
                    receivedResponse = map
                }

                override fun onError(errorContainer: ErrorContainer?) {
                    fail("Should be success")
                }
            }
        )

        assertNotNull(receivedResponse)
        assertEquals(expectedProductIdentifier, receivedResponse?.get("productIdentifier"))
    }

    @Test
    fun `purchasePackage passes correct productIdentifier after a successful purchase`() {
        configure(
            context = mockContext,
            apiKey = "api_key",
            appUserID = "appUserID",
            observerMode = true,
            platformInfo = PlatformInfo("flavor", "version")
        )
        val expectedProductIdentifier = "product"
        var receivedResponse: MutableMap<String, *>? = null

        val capturedReceiveOfferingsListener = slot<ReceiveOfferingsListener>()
        val mockSkuDetails = mockSubscriptionProduct(expectedProductIdentifier)
        val mockPurchase = mockk<Purchase>()
        every {
            mockPurchase.skus
        } returns ArrayList(listOf(expectedProductIdentifier, "other"))

        val (offeringIdentifier, packageToPurchase, offerings) = getOfferings(mockSkuDetails)

        every {
            mockPurchases.getOfferings(capture(capturedReceiveOfferingsListener))
        } answers {
            capturedReceiveOfferingsListener.captured.onReceived(offerings)
        }

        val capturedMakePurchaseListener = slot<MakePurchaseListener>()

        every {
            mockPurchases.purchasePackage(mockActivity, packageToPurchase, capture(capturedMakePurchaseListener))
        } answers {
            capturedMakePurchaseListener.captured.onCompleted(mockPurchase, mockk(relaxed = true))
        }

        purchasePackage(
            mockActivity,
            packageIdentifier = "packageIdentifier",
            oldSku = null,
            prorationMode = null,
            onResult = object : OnResult {
                override fun onReceived(map: MutableMap<String, *>?) {
                    receivedResponse = map
                }

                override fun onError(errorContainer: ErrorContainer?) {
                    fail("Should be success")
                }
            },
            offeringIdentifier = offeringIdentifier
        )

        assertNotNull(receivedResponse)
        assertEquals(expectedProductIdentifier, receivedResponse?.get("productIdentifier"))
    }

    private fun getOfferings(mockSkuDetails: SkuDetails): Triple<String, Package, Offerings> {
        val offeringIdentifier = "offering"
        val packageToPurchase = Package(
            identifier = "packageIdentifier",
            packageType = PackageType.ANNUAL,
            product = mockSkuDetails,
            offering = offeringIdentifier
        )
        val offering = Offering(
            identifier = offeringIdentifier,
            serverDescription = "",
            availablePackages = listOf(packageToPurchase)
        )
        val offerings = Offerings(current = offering, all = mapOf(offeringIdentifier to offering))
        return Triple(offeringIdentifier, packageToPurchase, offerings)
    }

    private fun mockSubscriptionProduct(expectedProductIdentifier: String): SkuDetails {
        val mockSkuDetails = mockk<SkuDetails>()
        every {
            mockSkuDetails.sku
        } returns expectedProductIdentifier
        every {
            mockSkuDetails.type
        } returns BillingClient.SkuType.SUBS
        return mockSkuDetails
    }

}
