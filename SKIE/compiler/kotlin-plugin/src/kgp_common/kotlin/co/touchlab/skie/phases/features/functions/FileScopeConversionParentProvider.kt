package co.touchlab.skie.phases.features.functions

import co.touchlab.skie.kir.element.KirCallableDeclaration
import co.touchlab.skie.kir.element.KirClass
import co.touchlab.skie.phases.SirPhase
import co.touchlab.skie.sir.element.SirCallableDeclaration
import co.touchlab.skie.sir.element.SirClass
import co.touchlab.skie.sir.element.SirConditionalConstraint
import co.touchlab.skie.sir.element.SirDeclarationParent
import co.touchlab.skie.sir.element.SirExtension
import co.touchlab.skie.sir.element.SirFile
import co.touchlab.skie.sir.element.SirSimpleFunction
import co.touchlab.skie.sir.element.receiverDeclaration
import co.touchlab.skie.sir.element.resolveAsSirClass
import co.touchlab.skie.sir.element.resolveAsSirClassType
import co.touchlab.skie.sir.type.DeclaredSirType
import co.touchlab.skie.sir.type.NullableSirType
import co.touchlab.skie.sir.type.OirDeclaredSirType
import co.touchlab.skie.sir.type.SirDeclaredSirType
import co.touchlab.skie.sir.type.SirType
import co.touchlab.skie.sir.type.SpecialSirType

class FileScopeConversionParentProvider(
    private val context: SirPhase.Context,
) {

    private val sirProvider = context.sirProvider
    private val oirBuiltins = context.oirBuiltins
    private val sirBuiltins = context.sirBuiltins

    private val cache: MutableMap<SirType, List<SirExtension>> = mutableMapOf()

    private val unsupportedExtensionTypes = listOf(
        sirBuiltins.Swift.AnyClass,
    )

    fun <T : SirCallableDeclaration> forEachParent(
        callableDeclaration: KirCallableDeclaration<T>,
        sirCallableDeclaration: T,
        action: SirDeclarationParent.() -> Unit,
    ) {
        getParents(callableDeclaration, sirCallableDeclaration).forEach(action)
    }

    private fun getParents(
        callableDeclaration: KirCallableDeclaration<*>,
        sirCallableDeclaration: SirCallableDeclaration,
    ): List<SirDeclarationParent> {
        val associatedSirDeclarationHasSupportedReceiver =
            callableDeclaration.originalSirDeclaration.receiverDeclaration == sirCallableDeclaration.receiverDeclaration
        if (!associatedSirDeclarationHasSupportedReceiver) {
            return emptyList()
        }

        return when (callableDeclaration.origin) {
            KirCallableDeclaration.Origin.Member -> error("Member callable are not supported. Was: $callableDeclaration")
            KirCallableDeclaration.Origin.Extension -> getExtensions(callableDeclaration, sirCallableDeclaration)
            KirCallableDeclaration.Origin.Global -> context.skieNamespaceProvider.getNamespaceFile(callableDeclaration.owner).let(::listOf)
        }
    }

    private fun getExtensions(
        callableDeclaration: KirCallableDeclaration<*>,
        sirCallableDeclaration: SirCallableDeclaration,
    ): List<SirDeclarationParent> {
        val parentType = when (sirCallableDeclaration) {
            is SirSimpleFunction -> sirCallableDeclaration.valueParameters.first().type
            else -> error("Extensions from files cannot be anything else other than functions. Was: $callableDeclaration")
        }

        return getExtensions(callableDeclaration, parentType)
    }

    private fun getExtensions(callableDeclaration: KirCallableDeclaration<*>, parentType: SirType): List<SirExtension> =
        cache.getOrPut(parentType) {
            createExtension(callableDeclaration, parentType)
        }

    private fun createExtension(callableDeclaration: KirCallableDeclaration<*>, parentType: SirType): List<SirExtension> {
        val namespace = getExtensionNamespace(callableDeclaration, parentType)

        return when (parentType) {
            is DeclaredSirType -> {
                createNonOptionalExtension(
                    file = namespace,
                    sirClass = parentType.evaluateAsExtensionClass() ?: return emptyList(),
                ).let(::listOfNotNull)
            }
            is SpecialSirType.Any -> {
                createNonOptionalExtension(
                    file = namespace,
                    sirClass = oirBuiltins.NSObject.originalSirClass,
                ).let(::listOfNotNull)
            }
            is NullableSirType -> getOptionalExtensions(callableDeclaration, parentType, namespace)
            else -> return emptyList()
        }
    }

    private fun DeclaredSirType.evaluateAsExtensionClass(): SirClass? =
        evaluate().type.resolveAsSirClass()?.takeIf { it !in unsupportedExtensionTypes }

    private fun getExtensionNamespace(
        callableDeclaration: KirCallableDeclaration<*>,
        parentType: SirType,
    ): SirFile {
        val extensionReceiverKirClass = getExtensionReceiverKirClassIfExists(parentType)

        return extensionReceiverKirClass?.let { context.skieNamespaceProvider.getNamespaceFile(it) }
            ?: context.skieNamespaceProvider.getNamespaceFile(callableDeclaration.owner)
    }

    private fun getExtensionReceiverKirClassIfExists(parentType: SirType): KirClass? =
        when (parentType) {
            is SirDeclaredSirType -> parentType.declaration.resolveAsSirClass()?.let { context.kirProvider.findClass(it) }
            is OirDeclaredSirType -> context.kirProvider.findClass(parentType.declaration)
            is NullableSirType -> getExtensionReceiverKirClassIfExists(parentType.type)
            else -> null
        }

    private fun createNonOptionalExtension(file: SirFile, sirClass: SirClass): SirExtension? =
        if (sirClass.typeParameters.isEmpty()) {
            sirProvider.getExtension(
                classDeclaration = sirClass,
                parent = file,
            )
        } else {
            // Generics is not supported yet
            null
        }

    private fun getOptionalExtensions(callableDeclaration: KirCallableDeclaration<*>, type: NullableSirType, namespace: SirFile): List<SirExtension> {
//         val nonNullType = type.type
//
//         val constraintType = when (nonNullType) {
//             is DeclaredSirType -> nonNullType.evaluate().type.resolveAsSirClassType() ?: return emptyList()
//             SpecialSirType.Any -> oirBuiltins.NSObject.originalSirClass.defaultType
//             else -> return emptyList()
//         }
//
//         return getExtensions(callableDeclaration, nonNullType) + createOptionalExtension(namespace, constraintType)

        // TODO Not supported properly yet (support is missing for structs, enums and unsupportable types)
        return emptyList()
    }

//     private fun createOptionalExtension(parent: SirFile, constraintType: SirDeclaredSirType): SirExtension =
//         SirExtension(
//             classDeclaration = sirProvider.sirBuiltins.Swift.Optional,
//             parent = parent,
//         ).apply {
//             SirConditionalConstraint(
//                 typeParameter = sirProvider.sirBuiltins.Swift.Optional.typeParameters.first(),
//                 bounds = listOf(constraintType),
//             )
//         }
}