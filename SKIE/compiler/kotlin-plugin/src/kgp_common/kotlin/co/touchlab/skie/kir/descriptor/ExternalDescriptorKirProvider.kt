package co.touchlab.skie.kir.descriptor

import co.touchlab.skie.configuration.ClassConfiguration
import co.touchlab.skie.configuration.ClassInterop
import co.touchlab.skie.configuration.FileConfiguration
import co.touchlab.skie.configuration.FileOrClassConfiguration
import co.touchlab.skie.configuration.ModuleConfiguration
import co.touchlab.skie.configuration.PackageConfiguration
import co.touchlab.skie.configuration.RootConfiguration
import co.touchlab.skie.configuration.provider.descriptor.DescriptorConfigurationProvider
import co.touchlab.skie.kir.element.KirClass
import co.touchlab.skie.kir.element.KirModule
import co.touchlab.skie.kir.element.KirProject
import co.touchlab.skie.phases.kir.CreateExposedKirTypesPhase
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

class ExternalDescriptorKirProvider(
    private val kirProject: KirProject,
    private val rootConfiguration: RootConfiguration,
    private val descriptorConfigurationProvider: DescriptorConfigurationProvider,
    descriptorKirProvider: Lazy<DescriptorKirProvider>,
) {

    val allExternalClassesAndProtocols: Collection<KirClass>
        get() = externalClassesAndProtocolsCache.values

    private val externalClassesAndProtocolsCache = mutableMapOf<ClassDescriptor, KirClass>()

    private val externalModulesByName = mutableMapOf<String, KirModule>()

    private val descriptorKirProvider by descriptorKirProvider

    private val unknownModule: KirModule = KirModule(
        name = "<unknown>",
        project = kirProject,
        configuration = ModuleConfiguration(rootConfiguration),
        origin = KirModule.Origin.UnknownExternal,
    )

    private val externalClassParentConfigurationByModule = mutableMapOf<KirModule, FileOrClassConfiguration>()

    fun getExternalClass(descriptor: ClassDescriptor): KirClass =
        getExternalClass(descriptor, addNSObjectSuperType = true)

    fun getExternalBuiltinClass(descriptor: ClassDescriptor): KirClass =
        getExternalClass(descriptor, addNSObjectSuperType = false)

    private fun getExternalClass(descriptor: ClassDescriptor, addNSObjectSuperType: Boolean): KirClass =
        externalClassesAndProtocolsCache.getOrPut(descriptor.original) {
            val (name, kind) = if (descriptor.kind.isInterface) {
                descriptor.name.asString().removeSuffix("Protocol") to KirClass.Kind.Interface
            } else {
                descriptor.name.asString() to KirClass.Kind.Class
            }

            val module = getExternalModule(descriptor)

            val kirClass = KirClass(
                kotlinFqName = descriptor.fqNameSafe.asString(),
                objCName = name,
                swiftName = name,
                parent = module,
                kind = kind,
                origin = if (descriptor.isPlatformType) KirClass.Origin.PlatformType else KirClass.Origin.ExternalCinteropType,
                superTypes = if (addNSObjectSuperType && kind != KirClass.Kind.Interface) {
                    listOf(descriptorKirProvider.kirBuiltins.NSObject.defaultType)
                } else {
                    emptyList()
                },
                isSealed = false,
                hasUnexposedSealedSubclasses = false,
                nestingLevel = CreateExposedKirTypesPhase.getNestingLevel(descriptor),
                configuration = createClassConfiguration(module),
            )

            descriptorKirProvider.registerClass(kirClass, descriptor)

            // Obj-C classes do not have type parameters in Kotlin

            kirClass
        }

    private fun getExternalModule(descriptor: ClassDescriptor): KirModule {
        val moduleName = if (descriptor.isPlatformType) {
            descriptor.cinteropFrameworkNameForPlatformType
        } else {
            descriptorConfigurationProvider.getConfiguration(descriptor)[ClassInterop.CInteropFrameworkName]
        }

        if (moduleName == null) {
            return unknownModule
        }

        return externalModulesByName.getOrPut(moduleName) {
            KirModule(
                name = moduleName,
                project = kirProject,
                configuration = ModuleConfiguration(rootConfiguration),
                origin = KirModule.Origin.KnownExternal,
            )
        }
    }

    private val ClassDescriptor.isPlatformType: Boolean
        get() = this.fqNameUnsafe.pathSegments()[0].asString() == "platform"

    private val ClassDescriptor.cinteropFrameworkNameForPlatformType: String
        get() = if (this.fqNameUnsafe.shortName().asString() != "NSObject") {
            this.fqNameUnsafe.asString().split(".")[1]
        } else {
            "Foundation"
        }

    private fun createClassConfiguration(module: KirModule): ClassConfiguration {
        val parentConfiguration = externalClassParentConfigurationByModule.getOrPut(module) {
            FileOrClassConfiguration.File(FileConfiguration(PackageConfiguration(module.configuration)))
        }

        return ClassConfiguration(parentConfiguration)
    }
}