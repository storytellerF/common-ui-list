package com.storyteller_f.composite_compiler_ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.storyteller_f.composite_defination.Composite
import java.io.BufferedOutputStream

class ProcessingProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return Processing(environment)
    }
}

class Processing(val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {
    private val logger = environment.logger
    private var count: Int = 0

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        count++

        val composites =
            resolver.getSymbolsWithAnnotation(Composite::class.java.canonicalName)
        logger.warn("composite round $count count ${composites.count()}")
        composites.forEach {
            it as KSClassDeclaration
            val clazzList = (it.annotations.first { ksAnnotation ->
                ksAnnotation.toString() == "@Database"
            }.arguments.first { argument ->
                argument.name?.asString() == "entities"
            }.value as ArrayList<*>).filterIsInstance<KSType>().mapNotNull { ksType ->
                ksType.declaration.qualifiedName?.asString()
            }.map { s ->
                "import $s"
            }
            val annotationsByType = it.getAnnotationsByType(Composite::class).first()
            val name = annotationsByType.name.ifEmpty {
                getName(it.qualifiedName?.getShortName()!!)
            }
            val packageName = it.packageName.asString()
            val daoName = it.getAllFunctions().first { declaration ->
                val shortName = declaration.qualifiedName?.getShortName()!!
                !shortName.startsWith("remoteKey") && shortName.endsWith("Dao")
            }.qualifiedName?.getShortName()!!
            val content = produceFileContent(
                name,
                packageName,
                daoName,
                clazzList,
                it.qualifiedName?.asString()!!
            )
            val dependencies =
                Dependencies(aggregating = false, *composites.mapNotNull { ksAnnotated ->
                    ksAnnotated.containingFile
                }.toList().toTypedArray())
            BufferedOutputStream(
                environment.codeGenerator.createNewFile(
                    dependencies,
                    "$packageName.composite",
                    "${name}Composite"
                )
            ).bufferedWriter().use { writer ->
                writer.write(content)
            }
        }
        return emptyList()
    }

    private fun getName(origin: String): String {
        for (i in 1 until origin.length) {
            if (origin[i].isUpperCase()) {
                return origin.substring(0, i)
            }
        }
        return origin
    }

    @Suppress("LongMethod")
    private fun produceFileContent(
        name: String,
        packageName: String,
        dataDao: CharSequence,
        clazzList: List<String>,
        databaseFullName: String
    ): String {
        val lower = name.lowercase()
        val dataParam = "${lower}s"
        val databaseType = "${name}Database"
        val remoteKeyType = "${name}RemoteKey"
        return """package $packageName.composite;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

${clazzList.joinToString("\n")}
import $databaseFullName;
import com.storyteller_f.ui_list.database.CommonRoomDatabase;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
class ${name}Composite(database: $databaseType) : CommonRoomDatabase<$name, $remoteKeyType, $databaseType>(database) {

    override suspend fun clearOld()  {
        database.$dataDao().clearRepos();
        database.remoteKeyDao().clearRemoteKeys();
    }

    override suspend fun insertRemoteKey(remoteKeys: MutableList<$remoteKeyType> ) {
        database.remoteKeyDao().insertAll(remoteKeys);
    }

    override suspend fun getRemoteKey(id: String) : $remoteKeyType? {
        return database.remoteKeyDao().remoteKeysRepoId(id);
    }

    override suspend fun insertAllData($dataParam: MutableList<$name>) {
        database.$dataDao().insertAll($dataParam);
    }

    override suspend fun deleteItemBy(d: $name) {
        database.$dataDao().delete(d);
        database.remoteKeyDao().delete(d.remoteKeyId());
    }

}"""
    }
}
