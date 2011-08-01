package i18nfields

import java.lang.reflect.Modifier
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.ast.FieldNode
import static org.springframework.asm.Opcodes.ACC_PUBLIC
import static org.springframework.asm.Opcodes.ACC_STATIC
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.ast.Parameter

import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.expr.MapExpression

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class I18nFieldsTransformation implements ASTTransformation {
	public static final String I18N_FIELDS_DEFINITION_FIELD_NAME = "i18n_fields"
	public static final String TRANSIENTS_DEFINITION_FIELD_NAME = "transients"
	public static final String LOCALES_DEFINITION_FIELD_NAME = "locales"

	void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
		if (!isValidAstNodes(astNodes))
			return
		i18nalizeFields(astNodes[1])
	}

	private boolean isValidAstNodes(ASTNode[] astNodes) {
		return astNodes != null && astNodes[0] != null && astNodes[1] != null && astNodes[0] instanceof AnnotationNode && astNodes[0].classNode?.name == I18nFields.class.getName() && astNodes[1] instanceof ClassNode
	}

	private void i18nalizeFields(ClassNode classNode) {
		for (field in getFieldsToI18nalize(classNode)) {
			i18nalizeField(field, classNode)
		}
	}

	private Collection getFieldsToI18nalize(ClassNode classNode) {
		def i18nFieldsExpression = classNode.getField(I18N_FIELDS_DEFINITION_FIELD_NAME).getInitialValueExpression() as ListExpression
		return i18nFieldsExpression.expressions.collect { it.getText() }
	}

	private void i18nalizeField(String field, ClassNode classNode) {
		def locales = locales()
		addLocalesMap(locales, classNode)
		addI18nFields(locales, classNode, field)
		makeFieldTransient(field, classNode)
		addGettersAndSetters(field, classNode)
	}

	private def addGettersAndSetters(String field, ClassNode classNode) {
		addProxyGetterAndSetter(field, classNode)
		addLocalizedGetterAndSetter(field, classNode)
	}

	private def addLocalizedGetterAndSetter(String field, ClassNode classNode) {
		addLocalizedGetter(field, classNode)
		addLocalizedSetter(field, classNode)
	}

	private def addProxyGetterAndSetter(String field, ClassNode classNode) {
		addProxyGetter(field, classNode)
		addProxySetter(field, classNode)
	}

	private def addI18nFields(Collection locales, ClassNode classNode, String field) {
		for (locale in locales) {
			def fieldName = "${field}_${locale}"
			addI18nField(fieldName, classNode)
		}
	}

	private Collection locales() {
		return filterInvalidLocales(getConfiguredLocales())
	}

	private Collection getConfiguredLocales() {
		return pluginConfig().containsKey(LOCALES_DEFINITION_FIELD_NAME) ? pluginConfig().get(LOCALES_DEFINITION_FIELD_NAME).collect { it.trim() } : []
	}

	private Map pluginConfig() {
		ConfigurationHolder.config?.i18nFields ? ConfigurationHolder.config.i18nFields : [:]
	}

	private Collection filterInvalidLocales(Collection configuredLocales) {
		def locales = configuredLocales.findAll { isValidLocale(it) }
		def invalidLocales = configuredLocales - locales
		logInvalidLocales(invalidLocales)
		return locales
	}

	private boolean isValidLocale(String locale) {
		Collection availableLocales = Locale.getAvailableLocales().collect { it.toString() }
		return availableLocales.contains(locale)
	}

	private void logInvalidLocales(invalidLocales) {
		// TODO: Use log4j
		if (invalidLocales.size() > 0)
			println "[i18nFieldsPlugin] Ignoring ${invalidLocales} invalid locale(s)"
	}

	private void addLocalesMap(Collection locales, ClassNode classNode) {
		MapExpression localesFieldMap = new MapExpression()
		Map<String, ListExpression> mapEntryExpressions = [:]
		locales.each { localeString ->
			Locale locale = getLocale(localeString)
			if (!mapEntryExpressions.containsKey(locale.language))
				mapEntryExpressions.put(locale.language, new ListExpression())
			if ("" != locale.country)
				mapEntryExpressions.get(locale.language).addExpression(new ConstantExpression(locale.country))
		}
		mapEntryExpressions.each {
			localesFieldMap.addMapEntryExpression(new ConstantExpression(it.key), it.value)
		}
		def localesField = new FieldNode(LOCALES_DEFINITION_FIELD_NAME, ACC_PUBLIC | ACC_STATIC, new ClassNode(Object.class), classNode, localesFieldMap)
		// TODO: Use log4j
		println "[i18nFieldsPlugin] Adding locales static field to ${classNode.name}"
		classNode.addField(localesField)
	}

	private def getLocale(localeString) {
		String[] localeAsArray = localeString.split("_")
		String language = localeAsArray[0]
		String country = localeAsArray.size() > 1 ? localeAsArray[1] : null
		if (null != country)
			return new Locale(language, country)
		return new Locale(language)
	}

	private void addI18nField(String fieldName, ClassNode classNode) {
		// TODO: Use log4j
		println "[i18nFieldsPlugin] Adding '${fieldName}' field to ${classNode.name}"
		classNode.addProperty(fieldName, Modifier.PUBLIC, new ClassNode(String.class), new ConstantExpression(null), null, null)
	}

	private void makeFieldTransient(String field, ClassNode classNode) {
		FieldNode transientsField = getTransientsField(classNode)
		ListExpression transientFieldList = transientsField.getInitialExpression()
		// TODO: Use log4j
		println "[i18nFieldsPlugin] Making '${field}' field of class ${classNode.name} transient"
		transientFieldList.addExpression(new ConstantExpression(field))
	}

	private FieldNode getTransientsField(ClassNode classNode) {
		if (!fieldExists(TRANSIENTS_DEFINITION_FIELD_NAME, classNode)) {
			addTransientsField(classNode)
		}
		return classNode.getDeclaredField(TRANSIENTS_DEFINITION_FIELD_NAME)
	}

	private boolean fieldExists(String field, ClassNode classNode) {
		return null != classNode.getDeclaredField(field)
	}

	private void addTransientsField(ClassNode classNode) {
		def transients = new FieldNode(TRANSIENTS_DEFINITION_FIELD_NAME, ACC_PUBLIC | ACC_STATIC, new ClassNode(Object.class), classNode, new ListExpression())
		// TODO: Use log4j
		println "[i18nFieldsPlugin] Adding transients static field to ${classNode.name}"
		classNode.addField(transients)
	}

	private addProxyGetter(String field, ClassNode classNode) {
		def methodName = GrailsClassUtils.getGetterName(field)
		def code = """
def locale = org.springframework.context.i18n.LocaleContextHolder.getLocale()
if (${LOCALES_DEFINITION_FIELD_NAME}.containsKey(locale.language) && !${LOCALES_DEFINITION_FIELD_NAME}[locale.language].contains(locale.country))
	locale = new Locale(locale.language)
return this.\"${field}_\${locale}\"
"""
		def codeBlock = new AstBuilder().buildFromString(code).pop()
		// TODO: Use log4j
		println "[i18nFieldsPlugin] Adding '${methodName}()' proxy method to ${classNode.name}"
		classNode.addMethod(getNewMethod(methodName, [] as Parameter[], codeBlock))
	}

	private addProxySetter(String field, ClassNode classNode) {
		def methodName = GrailsClassUtils.getSetterName(field)
		def code = "this.\"${field}_\${org.springframework.context.i18n.LocaleContextHolder.getLocale()}\" = value"
		def codeBlock = new AstBuilder().buildFromString(code).pop()
		def parameters = [new Parameter(ClassHelper.make(String, false), "value")] as Parameter[]
		// TODO: Use log4j
		println "[i18nFieldsPlugin] Adding '${methodName}(String value)' proxy method to ${classNode.name}"
		classNode.addMethod(getNewMethod(methodName, parameters as Parameter[], codeBlock))
	}

	private addLocalizedGetter(String field, ClassNode classNode) {
		def methodName = GrailsClassUtils.getGetterName(field)
		def code = """
if (${LOCALES_DEFINITION_FIELD_NAME}.containsKey(locale.language) && !${LOCALES_DEFINITION_FIELD_NAME}[locale.language].contains(locale.country))
	locale = new Locale(locale.language)
return this.\"${field}_\${locale}\"
"""
		def codeBlock = new AstBuilder().buildFromString(code).pop()
		def parameters = [new Parameter(ClassHelper.make(Locale, false), "locale")] as Parameter[]
		classNode.addMethod(getNewMethod(methodName, parameters, codeBlock))
		// TODO: Use log4j
		println "[i18nFieldsPlugin] Adding '${methodName}(Locale locale)' helper method to ${classNode.name}"
	}

	private addLocalizedSetter(String field, ClassNode classNode) {
		def methodName = GrailsClassUtils.getSetterName(field)
		def code = "this.\"${field}_\${locale}\" = value"
		def codeBlock = new AstBuilder().buildFromString(code).pop()
		def parameters = [
				new Parameter(ClassHelper.make(String, false), "value"),
				new Parameter(ClassHelper.make(Locale, false), "locale")
		] as Parameter[]
		classNode.addMethod(getNewMethod(methodName, parameters, codeBlock))
		// TODO: Use log4j
		println "[i18nFieldsPlugin] Adding '${methodName}(String value, Locale locale)' helper method to ${classNode.name}"
	}

	private MethodNode getNewMethod(String methodName, Parameter[] parameters, ASTNode codeBlock) {
		return new MethodNode(methodName, ACC_PUBLIC, ClassHelper.make(String.class, false),
				parameters,
				[] as ClassNode[],
				codeBlock
		)
	}
}