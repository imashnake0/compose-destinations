package com.ramcosta.composedestinations.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.validate
import com.ramcosta.composedestinations.codegen.commons.*
import com.ramcosta.composedestinations.codegen.model.Destination
import com.ramcosta.composedestinations.codegen.model.Parameter
import com.ramcosta.composedestinations.codegen.model.Type
import com.ramcosta.composedestinations.codegen.processors.DestinationsAggregateProcessor
import com.ramcosta.composedestinations.codegen.processors.DestinationsProcessor
import com.ramcosta.composedestinations.commons.*
import com.ramcosta.composedestinations.commons.findAnnotation
import com.ramcosta.composedestinations.commons.findArgumentValue

internal class Processor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotatedFunctions: Sequence<KSFunctionDeclaration> =
            resolver.getSymbolsWithAnnotation(DESTINATION_ANNOTATION_QUALIFIED)
                .filterIsInstance<KSFunctionDeclaration>()

        if (!annotatedFunctions.iterator().hasNext()) {
            return emptyList()
        }

        val kspCodeOutputStreamMaker = KspCodeOutputStreamMaker(codeGenerator)
        val kspLogger = KspLogger(logger)

        val generatedDestinationFiles = DestinationsProcessor(
            kspCodeOutputStreamMaker,
            kspLogger
        ).process(
            annotatedFunctions.map { it.toDestination() }
        )

        DestinationsAggregateProcessor(kspCodeOutputStreamMaker, kspLogger).process(generatedDestinationFiles)

        return annotatedFunctions.filterNot { it.validate() }.toList()
    }

    private fun KSFunctionDeclaration.toDestination(): Destination {
        val composableName = simpleName.asString()
        val name = composableName + DESTINATION_SPEC_SUFFIX
        val destinationAnnotation = findAnnotation(DESTINATION_ANNOTATION)

        return Destination(
            name = name,
            qualifiedName = "$PACKAGE_NAME.$name",
            composableName = composableName,
            composableQualifiedName = qualifiedName!!.asString(),
            cleanRoute = destinationAnnotation.findArgumentValue<String>(DESTINATION_ANNOTATION_ROUTE_ARGUMENT)!!,
            parameters = parameters.map { it.toParameter() },
            isStart = destinationAnnotation.findArgumentValue<Boolean>(DESTINATION_ANNOTATION_START_ARGUMENT)!!
        )
    }

    private fun KSValueParameter.toParameter(): Parameter {
        return Parameter(
            name!!.asString(),
            type.resolve().toType(),
            getDefaultValue()
        )
    }

    private fun KSType.toType() = Type(
        declaration.simpleName.asString(),
        declaration.qualifiedName!!.asString(),
        isMarkedNullable
    )
}