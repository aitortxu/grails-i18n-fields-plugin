package i18nfields

import org.junit.Test
import static org.junit.Assert.assertThat
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.not
import static org.junit.matchers.JUnitMatchers.hasItem
import org.codehaus.groovy.tools.ast.TranformTestHelper
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.junit.BeforeClass
import org.junit.Before
import org.springframework.context.i18n.LocaleContextHolder

class I18nFieldsTransformationTests {
	def chuchu
	def blabla

	@BeforeClass
	static void "Mock locale configuration"() {
		ConfigurationHolder configurationHolder = new ConfigurationHolder()
		ConfigObject config = new ConfigObject()
		configurationHolder.setConfig(config)
		ConfigurationHolder.config.i18nFields.locales = ["es", "es_MX", "en_US", "kl_KL"]
		I18nFieldsTransformation.metaClass.ConfigurationHolder = configurationHolder
	}

	@Before
	void "Create our test instances"() {
		createChuchu()
		createBlabla()
	}

	@Test
	void "Adds localized versions of a field"() {
		assertThat blabla.metaClass.properties*.name, hasItem("name_es")
		assertThat blabla.metaClass.properties*.name, hasItem("name_es_MX")
		assertThat blabla.metaClass.properties*.name, hasItem("name_en_US")
	}

	@Test
	void "Ignores invalid locales like Klingon"() {
		assertThat blabla.metaClass.properties*.name, not(hasItem("name_kl_KL"))
	}

	@Test
	void "Makes original field transient"() {
		assertThat blabla.transients, hasItem("name")
	}

	@Test
	void "Creates static transients collection if the class does not have it"() {
		// Note that Chuchu.groovy doesn't define a static transients collection
		assertThat chuchu.transients, hasItem("name")
	}

	@Test
	void "Adds a proxy getter that uses locale settings from the context"() {
		LocaleContextHolder.metaClass.static.getLocale = {-> new Locale("es")}
		setNames()
		assertThat chuchu.getName(), is("Nombre")
	}

	@Test
	void "Overloads proxy getter with a new signature that specifies the wanted locale"() {
		setNames()
		assertThat chuchu.getName(new Locale("es")), is("Nombre")
		assertThat chuchu.getName(new Locale("es", "MX")), is("Nombre wei")
		assertThat chuchu.getName(new Locale("en", "US")), is("Name")
	}

	@Test
	void "Adds a proxy setter that set the correct localized value of a field"() {
		LocaleContextHolder.metaClass.static.getLocale = {-> new Locale("es")}
		blabla.setName "Nombre"
		assertThat blabla.name_es, is("Nombre")
	}

	@Test
	void "Overloads proxy setter with a new signature that specifies the wanted locale"() {
		blabla.setName("Nombre", new Locale("es"))
		blabla.setName("Nombre wei", new Locale("es", "MX"))
		blabla.setName("Name", new Locale("en", "US"))
		assertThat blabla.name_es, is("Nombre")
		assertThat blabla.name_es_MX, is("Nombre wei")
		assertThat blabla.name_en_US, is("Name")
	}

	@Test
	void "Adds to domain classes a static map with the current locales defined by the plugin"() {
		assertThat blabla.locales.keySet(), hasItem("es")
		assertThat blabla.locales.keySet(), hasItem("en")
		assertThat blabla.locales.es, hasItem("MX")
		assertThat blabla.locales.en, hasItem("US")
	}

	@Test
	void "Proxy getters fall back to a more generic Locale if wanted does not exist"() {
		setNames()
		LocaleContextHolder.metaClass.static.getLocale = {-> new Locale("es", "AR")}
		assertThat chuchu.getName(), is("Nombre")
	}

	@Test
	void "Localized getters fall back to a more generic Locale if wanted does not exist"() {
		setNames()
		assertThat chuchu.getName(new Locale("es", "AR")), is("Nombre")
	}

	private def createBlabla() {
		blabla = createInstanceFromFile("./test/unit/i18nfields/Blabla.groovy")
	}

	private def createChuchu() {
		chuchu = createInstanceFromFile("./test/unit/i18nfields/ChuChu.groovy")
	}

	private def createInstanceFromFile(String filePath) {
		def file = new File(filePath)
		def invoker = new TranformTestHelper(new I18nFieldsTransformation(), CompilePhase.CANONICALIZATION)
		def clazz = invoker.parse(file)
		return clazz.newInstance()
	}

	private def setNames() {
		chuchu.name_es = "Nombre"
		chuchu.name_es_MX = "Nombre wei"
		chuchu.name_en_US = "Name"
	}
}