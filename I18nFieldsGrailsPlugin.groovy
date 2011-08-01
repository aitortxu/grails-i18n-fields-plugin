import org.codehaus.groovy.grails.commons.*
import org.apache.log4j.Logger

import i18nfields.*

class I18nFieldsGrailsPlugin {
	static final def log = Logger.getLogger(I18nFieldsGrailsPlugin)

	def version = "0.6"
	def grailsVersion = "1.3 > *"
	def dependsOn = [:]
	def pluginExcludes = [
			"grails-app",
			"web-app"
	]

	def config = ConfigurationHolder.config

	def author = "Jorge Uriarte"
	def authorEmail = "jorge.uriarte@omelas.net"
	def title = "I18n Fields"
	def description = "This plugin provides an easy way of declarativily localize database fields of your content tables."
	def documentation = "http://grails.org/plugin/i18n-fields"

	def doWithDynamicMethods = { context ->
		['controller', 'service', 'tagLib', 'codec', 'bootstrap'].each {
			application."${it}Classes".each { theClass ->
				// TODO: Use log4j
				println "[i18nFieldsPlugin] Adding 'withLocale' method to ${theClass.name} ${it} Class"
				theClass.metaClass.withLocale = I18nFieldsHelper.withLocale
			}
		}
	}
}

