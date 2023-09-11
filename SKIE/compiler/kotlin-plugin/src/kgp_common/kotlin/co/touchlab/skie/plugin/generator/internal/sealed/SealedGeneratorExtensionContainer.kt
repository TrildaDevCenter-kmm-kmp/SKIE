package co.touchlab.skie.plugin.generator.internal.sealed

import co.touchlab.skie.configuration.SealedInterop
import co.touchlab.skie.plugin.api.model.SwiftModelScope
import co.touchlab.skie.plugin.api.model.type.KotlinClassSwiftModel
import co.touchlab.skie.plugin.api.sir.element.SirClass
import co.touchlab.skie.plugin.api.sir.element.superClassType
import co.touchlab.skie.plugin.api.sir.element.toTypeParameterUsage
import co.touchlab.skie.plugin.api.sir.type.DeclaredSirType
import co.touchlab.skie.plugin.api.sir.type.SirType
import co.touchlab.skie.plugin.api.sir.type.TypeParameterUsageSirType
import co.touchlab.skie.plugin.api.util.toValidSwiftIdentifier
import co.touchlab.skie.plugin.generator.internal.configuration.ConfigurationContainer

internal interface SealedGeneratorExtensionContainer : ConfigurationContainer {

    val KotlinClassSwiftModel.elseCaseName: String
        get() = this.getConfiguration(SealedInterop.ElseName)

    fun KotlinClassSwiftModel.enumCaseName(preferredNamesCollide: Boolean): String =
        if (preferredNamesCollide) this.enumCaseNameBasedOnSwiftIdentifier else this.enumCaseNameBasedOnKotlinIdentifier

    val KotlinClassSwiftModel.enumCaseNamesBasedOnKotlinIdentifiersCollide: Boolean
        get() {
            val names = this.visibleSealedSubclasses.map { it.enumCaseNameBasedOnKotlinIdentifier }

            return names.size != names.distinct().size
        }

    val KotlinClassSwiftModel.enumCaseNameBasedOnKotlinIdentifier: String
        get() {
            val configuredName = this.getConfiguration(SealedInterop.Case.Name)

            return configuredName ?: this.classDescriptor.name.identifier.replaceFirstChar { it.lowercase() }.toValidSwiftIdentifier()
        }

    val KotlinClassSwiftModel.enumCaseNameBasedOnSwiftIdentifier: String
        get() {
            val configuredName = this.getConfiguration(SealedInterop.Case.Name)

            return configuredName ?: this.kotlinSirClass.fqName.toLocalUnescapedNameString().toValidSwiftIdentifier()
        }

    val KotlinClassSwiftModel.hasElseCase: Boolean
        get() = this.hasUnexposedSealedSubclasses ||
            this.exposedSealedSubclasses.size != this.visibleSealedSubclasses.size ||
            this.visibleSealedSubclasses.isEmpty()

    val KotlinClassSwiftModel.visibleSealedSubclasses: List<KotlinClassSwiftModel>
        get() = this.exposedSealedSubclasses.filter { it.getConfiguration(SealedInterop.Case.Visible) }

    fun SirClass.getSealedSubclassType(
        enum: SirClass,
        swiftModelScope: SwiftModelScope,
    ): SirType = DeclaredSirType(
        declaration = this,
        typeArguments = this.getTypeArgumentsForEnumCase(enum, swiftModelScope),
    )

    private fun SirClass.getTypeArgumentsForEnumCase(
        enum: SirClass,
        swiftModelScope: SwiftModelScope,
    ): List<SirType> {
        val superClassTypeArguments = superClassType?.typeArguments

        return typeParameters.map { typeParameter ->
            val indexOfParentTypeParameter = superClassTypeArguments?.indexOfFirst {
                it is TypeParameterUsageSirType && it.typeParameter == typeParameter
            } ?: -1

            enum.typeParameters.getOrNull(indexOfParentTypeParameter)?.toTypeParameterUsage()
                ?: swiftModelScope.sirBuiltins.Swift.AnyObject.defaultType
        }
    }
}
