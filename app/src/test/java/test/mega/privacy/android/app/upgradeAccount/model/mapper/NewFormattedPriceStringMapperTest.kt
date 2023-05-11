package test.mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.app.upgradeAccount.model.mapper.toNewFormattedPriceString
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class NewFormattedPriceStringMapperTest {
    @Test
    fun `test that mapper returns correctly formatted price string`() {
        val expectedResult = Pair("€4.99", "EUR")
        val currencyAmount = CurrencyAmount(4.99.toFloat(), Currency("EUR"))

        assertEquals(expectedResult, toNewFormattedPriceString(currencyAmount, Locale.US))
    }
}