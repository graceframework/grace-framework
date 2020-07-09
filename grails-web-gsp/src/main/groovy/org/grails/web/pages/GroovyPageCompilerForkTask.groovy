/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.web.pages

import org.codehaus.groovy.control.CompilerConfiguration
import org.grails.gsp.compiler.GroovyPageCompiler
import groovy.io.FileType
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.io.FileReaderSource
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.CompletionService
import groovy.transform.CompileStatic
/**
 * A Forked Compiler Task for use (typically by Gradle)
 *
 * @author David Estes
 */

@CompileStatic
public class GroovyPageCompilerForkTask {
	@Delegate CompilerConfiguration configuration = new CompilerConfiguration()

    String packageName = ""
    File sourceDir
    File destDir
    File tmpdir
    String serverpath
    String encoding
    String targetCompatibility

    GroovyPageCompilerForkTask(File sourceDir, File destDir, File tmpdir) {
        this.tmpdir = tmpdir
        this.destDir = destDir
        this.sourceDir = sourceDir
        
    }

    GroovyPageCompiler createPageCompiler() {
    	GroovyPageCompiler compiler = new GroovyPageCompiler()
    	CompilerConfiguration config = new CompilerConfiguration()
        if (classpath) {
            config.classpath = classpath.toString()
        }

        if(targetCompatibility) {
        	config.setTargetBytecode(targetCompatibility)
        }

        compiler.compilerConfig = config

        compiler.targetDir = destDir
        compiler.viewsDir = sourceDir

        if (tmpdir) {
            compiler.generatedGroovyPagesDirectory = tmpdir
        }

        if (packageName) {
            compiler.packagePrefix = packageName
        }
        if (serverpath) {
            compiler.viewPrefix=serverpath
        }
        if (encoding) {
            compiler.encoding = encoding
        }
        return compiler
    }

    void compile(List<File> sources) {
        GroovyPageCompiler compiler = createPageCompiler()
        compiler.srcFiles = sources
        compiler.compile()
    }

    static void main(String[] args) {
        run(args)
    }


    static final String fileExtension = '.gsp'

    static void run(String[] args) {
        if(args.length != 8) {
            System.err.println("Invalid arguments: [${args.join(',')}]")
            System.err.println("""
Usage: java -cp CLASSPATH GroovyPageCompilerForkTask [srcDir] [destDir] [tmpDir] [targetCompatibility] [packageName] [serverPath] [configFile] [encoding]
""")
            System.exit(1)
        }
        File srcDir = new File(args[0])
        File destinationDir = new File(args[1])
        File tmpDir = new File(args[2])
        String targetCompatibility = args[3]
        String packageName = args[4].trim()
        String serverpath = args[5]
        File configFile = new File(args[6])
        String encoding = args[7] ?: 'UTF-8'


        // configuration.readConfiguration(configFile)
        GroovyPageCompilerForkTask compiler = new GroovyPageCompilerForkTask(srcDir,destinationDir,tmpDir)
        if(packageName) {
        	compiler.packageName = packageName	
        }
        if(encoding) {
        	compiler.encoding = encoding
        }
        if(serverpath) {
        	compiler.serverpath = serverpath
        }
       	if(targetCompatibility) {
       		compiler.targetCompatibility = targetCompatibility
       	}

        List<File> allFiles = []
        srcDir.eachFileRecurse(FileType.FILES) { File f ->
            if(f.name.endsWith(fileExtension)) {
                allFiles.add(f)
            }
        }
        compiler.compile(allFiles)
    }
}