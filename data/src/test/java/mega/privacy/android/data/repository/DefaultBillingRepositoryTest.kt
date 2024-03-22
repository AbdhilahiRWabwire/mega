package mega.privacy.android.data.repository

import com.google.common.truth.Truth
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.BillingGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.LocalPricingMapper
import mega.privacy.android.data.mapper.PricingMapper
import mega.privacy.android.data.mapper.toPaymentMethodType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.BillingRepository
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaCurrency
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaPricing
import nz.mega.sdk.MegaRequest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalContracts
@kotlinx.coroutines.ExperimentalCoroutinesApi
class DefaultBillingRepositoryTest {
    private lateinit var underTest: BillingRepository

    private val accountInfoWrapper = mock<AccountInfoWrapper>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val paymentMethodFlagsCache = mock<Cache<PaymentMethodFlags>>()
    private val pricingCache = mock<Cache<Pricing>>()
    private val numberOfSubscriptionCache = mock<Cache<Long>>()
    private val skusCache = mock<Cache<List<MegaSku>>>()
    private val activeSubscriptionCache = mock<Cache<MegaPurchase>>()
    private val megaSkuObject1 = MegaSku(Skus.SKU_PRO_I_MONTH, 9990000, "EUR")
    private val megaSkuObject2 = MegaSku(Skus.SKU_PRO_II_MONTH, 9990000, "EUR")
    private val skuString = Skus.SKU_PRO_I_MONTH
    private val megaSkuList = listOf(megaSkuObject2, megaSkuObject1)
    private val localPricing =
        LocalPricing(CurrencyPoint.LocalCurrencyPoint(9990000), Currency("EUR"), skuString)
    private val localPricingMapper = mock<LocalPricingMapper>()
    private val pricingMapper = mock<PricingMapper>()
    private val billingGateway = mock<BillingGateway>()
    private val paymentMethodTypeMapper = ::toPaymentMethodType

    @Before
    fun setUp() {
        underTest = DefaultBillingRepository(
            accountInfoWrapper = accountInfoWrapper,
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            paymentMethodFlagsCache = paymentMethodFlagsCache,
            pricingCache = pricingCache,
            localPricingMapper = localPricingMapper,
            pricingMapper = pricingMapper,
            numberOfSubscriptionCache = numberOfSubscriptionCache,
            billingGateway = billingGateway,
            paymentMethodTypeMapper = paymentMethodTypeMapper,
            skusCache = skusCache,
            activeSubscriptionCache = activeSubscriptionCache
        )
    }

    @Test
    fun `test that get local pricing returns successfully`() =
        runTest {
            whenever(skusCache.get()).thenReturn(megaSkuList)
            whenever(localPricingMapper(megaSkuObject1)).thenReturn(localPricing)

            val actual = underTest.getLocalPricing(skuString)

            Truth.assertThat(actual).isEqualTo(localPricing)
        }

    @Test
    fun `test that getPaymentMethods returns success when MegaApi returns ERROR_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_GET_PAYMENT_METHODS)
            }

            whenever(megaApiGateway.getPaymentMethods(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.getPaymentMethod(true)
        }

    @Test
    fun `test that getPricing returns success when we clear cache and MegaApi returns ERROR_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val expectedMegaPricing = mock<MegaPricing>()
            val expectedMegaCurrency = mock<MegaCurrency>()
            val expectedPricing = Pricing(emptyList())

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_GET_PRICING)
                on { pricing }.thenReturn(expectedMegaPricing)
                on { currency }.thenReturn(expectedMegaCurrency)
            }

            whenever(megaApiGateway.getPricing(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            whenever(pricingMapper(expectedMegaPricing, expectedMegaCurrency)).thenReturn(
                expectedPricing
            )

            underTest.getPricing(true)
            verify(pricingCache, times(1)).set(expectedPricing)
        }

    @Test
    fun `when clear cache false and cache return null then getPricing api call`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val expectedMegaPricing = mock<MegaPricing>()
            val expectedMegaCurrency = mock<MegaCurrency>()
            val expectedPricing = Pricing(emptyList())

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_GET_PRICING)
                on { pricing }.thenReturn(expectedMegaPricing)
                on { currency }.thenReturn(expectedMegaCurrency)
            }

            whenever(megaApiGateway.getPricing(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
            whenever(pricingCache.get()).thenReturn(null)
            whenever(pricingMapper(expectedMegaPricing, expectedMegaCurrency)).thenReturn(
                expectedPricing
            )

            underTest.getPricing(false)
            verify(pricingCache, times(1)).set(expectedPricing)
        }

    @Test
    fun `when clear cache false and cache return value then no getPricing api call`() =
        runTest {
            whenever(pricingCache.get()).thenReturn(Pricing(emptyList()))

            underTest.getPricing(false)
            verifyNoMoreInteractions(megaApiGateway)
        }

    @Test(expected = MegaException::class)
    fun `test that getPaymentMethods finishes with general MegaException when MegaChatApi returns errors other than ERROR_ACCESS or ERROR_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_NOENT)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_GET_PAYMENT_METHODS)
            }

            whenever(megaApiGateway.getPaymentMethods(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.getPaymentMethod(true)
        }

    @Test
    fun `when clear cache false and numberOfSubscription cache return value then no getCreditCardQuerySubscriptions api call`() =
        runTest {
            whenever(numberOfSubscriptionCache.get()).thenReturn(1L)

            underTest.getNumberOfSubscription(false)
            verifyNoMoreInteractions(megaApiGateway)
        }

    @Test
    fun `when clear cache false and numberOfSubscription cache return null then getCreditCardQuerySubscriptions api call`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val expectedNumberOfSubscription = 10L

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS)
                on { number }.thenReturn(expectedNumberOfSubscription)
            }

            whenever(megaApiGateway.creditCardQuerySubscriptions(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
            whenever(numberOfSubscriptionCache.get()).thenReturn(null)

            underTest.getNumberOfSubscription(false)
            verify(numberOfSubscriptionCache, times(1)).set(expectedNumberOfSubscription)
        }

    @Test
    fun `test when getCreditCardQuerySubscriptions with clearCache as true then ignore cache and call API`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val previousNumberOfSubscription = 9L
            val expectNumberOfSubscription = 10L

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS)
                on { number }.thenReturn(expectNumberOfSubscription)
            }

            whenever(megaApiGateway.creditCardQuerySubscriptions(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            whenever(numberOfSubscriptionCache.get()).thenReturn(previousNumberOfSubscription)

            underTest.getNumberOfSubscription(true)
            verify(numberOfSubscriptionCache, times(1)).set(expectNumberOfSubscription)
        }

    @Test
    fun `test when queryPurchase billingGateway queryPurchase invoke`() = runTest {
        underTest.queryPurchase()
        verify(billingGateway, times(1)).queryPurchase()
    }

    @Test
    fun `test when querySkus billingGateway querySkus invoke`() = runTest {
        underTest.querySkus()
        verify(billingGateway, times(1)).querySkus()
    }

    @Test
    fun `test that getCurrentPaymentMethod returns PaymentMethod correctly`() {
        runTest {
            val paymentMethod = PaymentMethod.GOOGLE_WALLET

            whenever(accountInfoWrapper.subscriptionMethodId).thenReturn(3)

            val actual = underTest.getCurrentPaymentMethod()
            Truth.assertThat(actual).isEqualTo(paymentMethod)
        }
    }

    @Test
    fun `test that isBillingAvailable return true when availableSkus list is not empty`() {
        runTest {
            whenever(skusCache.get()).thenReturn(megaSkuList)

            val actual = underTest.isBillingAvailable()

            Truth.assertThat(actual).isEqualTo(true)
        }
    }

    @Test
    fun `test that isBillingAvailable return false when availableSkus list is empty`() {
        runTest {
            whenever(skusCache.get()).thenReturn(emptyList())

            val actual = underTest.isBillingAvailable()

            Truth.assertThat(actual).isEqualTo(false)
        }
    }

    @Test
    fun `test that getActiveSubscription return null when activeSubscriptionCache null`() {
        runTest {
            whenever(activeSubscriptionCache.get()).thenReturn(null)
            assertNull(underTest.getActiveSubscription())
        }
    }

    @Test
    fun `test that getActiveSubscription return non null when activeSubscriptionCache non null`() {
        runTest {
            whenever(activeSubscriptionCache.get()).thenReturn(MegaPurchase(""))
            assertNotNull(underTest.getActiveSubscription())
        }
    }

    @Test
    fun `test that clearCache invoke then following call`() {
        underTest.clearCache()
        verify(activeSubscriptionCache, times(1)).clear()
        verify(numberOfSubscriptionCache, times(1)).clear()
        assertNull(activeSubscriptionCache.get())
        assertNull(numberOfSubscriptionCache.get())
    }

    @Test
    fun `test that cancelSubscriptions returns true when creditCardQuerySubscriptions is successful`() {
        runTest {
            val feedback = "feedback"
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS)
            }

            whenever(megaApiGateway.creditCardQuerySubscriptions(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            val actual = underTest.cancelSubscriptions(feedback)
            Truth.assertThat(actual).isTrue()
        }
    }
}