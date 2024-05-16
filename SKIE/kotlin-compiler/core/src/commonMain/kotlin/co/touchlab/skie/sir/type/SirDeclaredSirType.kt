package co.touchlab.skie.sir.type

import co.touchlab.skie.sir.SirFqName
import co.touchlab.skie.sir.element.SirClass
import co.touchlab.skie.sir.element.SirModule
import co.touchlab.skie.sir.element.SirTypeAlias
import co.touchlab.skie.sir.element.SirTypeDeclaration
import co.touchlab.skie.sir.element.SirTypeParameter
import co.touchlab.skie.sir.element.SirVisibility
import co.touchlab.skie.sir.element.minimumVisibility
import co.touchlab.skie.sir.element.module
import co.touchlab.skie.sir.element.resolveAsKirClass
import io.outfoxx.swiftpoet.DeclaredTypeName
import io.outfoxx.swiftpoet.TypeName
import io.outfoxx.swiftpoet.parameterizedBy

data class SirDeclaredSirType(
    val declaration: SirTypeDeclaration,
    val typeArguments: List<SirType> = emptyList(),
    override val pointsToInternalName: Boolean = true,
) : DeclaredSirType() {

    override val isHashable: Boolean
        get() = declaration.isHashable

    override val isReference: Boolean
        get() = declaration.isReference

    override fun asHashableType(): SirType? =
        this.takeIf { declaration.isHashable }

    override fun asReferenceType(): SirType? =
        this.takeIf { declaration.isReference }

    override fun evaluate(): EvaluatedSirType {
        val evaluatedTypeArguments = lazy { typeArguments.map { it.evaluate() } }

        val evaluatedType = lazy {
            if (declaration.module !is SirModule.Unknown) {
                copy(typeArguments = evaluatedTypeArguments.value.map { it.type })
            } else {
                getUnknownCInteropModuleType()
            }
        }

        return EvaluatedSirType.Lazy(
            typeProvider = evaluatedType,
            canonicalNameProvider = lazy {
                if (evaluatedType.value is SirDeclaredSirType) {
                    getCanonicalName(evaluatedTypeArguments.value)
                } else {
                    evaluatedType.value.evaluate().canonicalName
                }
            },
            swiftPoetTypeNameProvider = lazy {
                if (evaluatedType.value is SirDeclaredSirType) {
                    getSwiftPoetTypeName(evaluatedTypeArguments.value)
                } else {
                    evaluatedType.value.evaluate().swiftPoetTypeName
                }
            },
            lowestVisibility = lazy {
                if (evaluatedType.value is SirDeclaredSirType) {
                    getVisibilityConstraint(evaluatedTypeArguments.value)
                } else {
                    evaluatedType.value.evaluate().visibilityConstraint
                }
            },
            referencedTypeDeclarationsProvider = lazy {
                if (evaluatedType.value is SirDeclaredSirType) {
                    getReferencedTypeDeclarations(evaluatedTypeArguments.value)
                } else {
                    setOf(declaration)
                }
            },
        )
    }

    private fun getUnknownCInteropModuleType(): SkieErrorSirType.UnknownCInteropFramework {
        val name = declaration.resolveAsKirClass()?.kotlinFqName ?: declaration.fqName.toLocalString()

        return SkieErrorSirType.UnknownCInteropFramework(name)
    }

    override fun inlineTypeAliases(): SirType {
        val inlinedTypeArguments = typeArguments.map { it.inlineTypeAliases() }

        return when (declaration) {
            is SirClass -> copy(typeArguments = inlinedTypeArguments)
            is SirTypeAlias -> {
                val substitutions = declaration.typeParameters.zip(inlinedTypeArguments).toMap()

                declaration.type.substituteTypeArguments(substitutions).inlineTypeAliases()
            }
        }
    }

    fun toSwiftPoetDeclaredTypeName(): DeclaredTypeName =
        if (pointsToInternalName) declaration.internalName.toSwiftPoetName() else declaration.fqName.toSwiftPoetName()

    private fun getCanonicalName(evaluatedTypeArguments: List<EvaluatedSirType>): String {
        val typeArgumentSuffix = if (evaluatedTypeArguments.isEmpty()) {
            ""
        } else {
            "<${evaluatedTypeArguments.joinToString { it.canonicalName }}>"
        }

        return when (declaration) {
            is SirClass -> declaration.fqName.toString() + typeArgumentSuffix
            is SirTypeAlias -> {
                val substitutions = declaration.typeParameters.zip(typeArguments).toMap()

                declaration.type.substituteTypeArguments(substitutions).evaluate().canonicalName + typeArgumentSuffix
            }
        }
    }

    private fun getSwiftPoetTypeName(evaluatedTypeArguments: List<EvaluatedSirType>): TypeName {
        val baseName = toSwiftPoetDeclaredTypeName()

        return if (evaluatedTypeArguments.isEmpty()) {
            baseName
        } else {
            baseName.parameterizedBy(evaluatedTypeArguments.map { it.swiftPoetTypeName })
        }
    }

    private fun getVisibilityConstraint(evaluatedTypeArguments: List<EvaluatedSirType>): SirVisibility =
        (evaluatedTypeArguments.map { it.visibilityConstraint } + declaration.visibility).minimumVisibility()

    private fun getReferencedTypeDeclarations(evaluatedTypeArguments: List<EvaluatedSirType>): Set<SirTypeDeclaration> =
        evaluatedTypeArguments.flatMap { it.referencedTypeDeclarations }.toSet() + declaration

    private fun SirFqName.toSwiftPoetName(): DeclaredTypeName =
        parent?.toSwiftPoetName()?.nestedType(simpleName)
            ?: DeclaredTypeName.qualifiedTypeName(module.name + "." + simpleName)

    override fun withFqName(): SirDeclaredSirType =
        copy(pointsToInternalName = false)

    override fun substituteTypeParameters(substitutions: Map<SirTypeParameter, SirTypeParameter>): SirDeclaredSirType =
        copy(typeArguments = typeArguments.map { it.substituteTypeParameters(substitutions) })

    override fun substituteTypeArguments(substitutions: Map<SirTypeParameter, SirType>): SirDeclaredSirType =
        copy(typeArguments = typeArguments.map { it.substituteTypeArguments(substitutions) })
}
