package i18nfields

import org.junit.Test
import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.hasItem
import org.codehaus.groovy.grails.plugins.converters.codecs.JSONCodec


class DynamicMethodsTests {
	@Test
	void "Plugin initialization adds withLocale to controller classes"() {
		assertThat TestController.metaClass.methods*.name, hasItem("withLocale")
	}

	@Test
	void "Plugin initialization adds withLocale to service classes"() {
		assertThat TestService.metaClass.methods*.name, hasItem("withLocale")
	}

	@Test
	void "Plugin initialization adds withLocale to taglib classes"() {
		assertThat TestTagLib.metaClass.methods*.name, hasItem("withLocale")
	}

	@Test
	void "Plugin intialization adds withLocale to codecs classes"() {
		assertThat JSONCodec.metaClass.methods*.name, hasItem("withLocale")
	}
}