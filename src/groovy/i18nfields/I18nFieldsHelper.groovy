package i18nfields

import org.springframework.context.i18n.LocaleContextHolder

class I18nFieldsHelper {
	public static transients_model = ['fieldname'];

	static void setLocale(Locale locale) {
		LocaleContextHolder.setLocale(locale)
	}

	static Locale getLocale() {
		return LocaleContextHolder.getLocale()
	}

	static withLocale = { Locale newLocale, Closure closure ->
		def previous = i18nfields.I18nFieldsHelper.getLocale()
		i18nfields.I18nFieldsHelper.setLocale(newLocale)
		def result = closure.call()
		i18nfields.I18nFieldsHelper.setLocale(previous)
		return result
	}
}

