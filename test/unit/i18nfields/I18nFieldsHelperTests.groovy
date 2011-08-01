package i18nfields

import org.junit.Test
import org.junit.Before
import static org.junit.Assert.assertThat
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.any
import org.gmock.WithGMock

import org.springframework.context.i18n.LocaleContextHolder

@WithGMock
class I18nFieldsHelperTests {
	def mockedLocaleContextHolder

	@Before
	void setUp() {
		mockedLocaleContextHolder = mock(LocaleContextHolder)
	}

	@Test
	void "Proxies setLocale calls over to LocaleContextHolder"() {
		mockedLocaleContextHolder.static.setLocale(any(Locale)).once()
		play {
			I18nFieldsHelper.setLocale(new Locale("es", "ES"))
		}
	}

	@Test
	void "Proxies getLocale calls over to LocaleContextHolder"() {
		def locale = new Locale("es", "ES")
		mockedLocaleContextHolder.static.getLocale().returns(locale).once()
		play {
			assertThat I18nFieldsHelper.getLocale(), is(locale)
		}
	}

	@Test
	void "withLocale saves the current locale sets a new one and resets it after closure is run"() {
		// TODO: Don't reproduce implementation in test
		// Currently I'd use a partial mock to ignore calls to getLocale,
		// but as they're static calls, I don't know how to do it
		def oldLocale = new Locale("es", "ES")
		def newLocale = new Locale("kl", "KL")
		Closure mockedClosure = mock(Closure)
		ordered {
			mockedLocaleContextHolder.static.getLocale().returns(oldLocale)
			mockedLocaleContextHolder.static.setLocale(newLocale)
			mockedClosure.call()
			mockedLocaleContextHolder.static.setLocale(oldLocale)
		}
		play {
			I18nFieldsHelper.withLocale(newLocale, mockedClosure)
		}
	}

	@Test
	void "withLocale returns whatever the closure returns"() {
		def closure = { return "Chuchu blabla" }
		play {
			assertThat I18nFieldsHelper.withLocale(new Locale("es"), closure), is("Chuchu blabla")
		}
	}
}