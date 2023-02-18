/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.exceptions.reporting

import org.codehaus.groovy.control.MultipleCompilationErrorsException

/**
 * @since 2.2
 * @author Graeme Rocher
 */
class DefaultStackTracePrinter implements StackTracePrinter {

    String prettyPrint(Throwable t) {
        if (t == null) {
            return ''
        }
        if (!t.stackTrace) {
            return 'No stack trace available'
        }
        StringWriter sw = new StringWriter()
        PrintWriter sb = new PrintWriter(sw)
        int mln = Math.max(4, t.stackTrace.lineNumber.max())
        int lineNumWidth = mln.toString().size()
        int methodNameBaseWidth = t.stackTrace.methodName*.size().max() + 1

        String lh = 'Line'.padLeft(lineNumWidth + 4)
        String header = "$lh | Method"
        printHeader(sb, header)

        boolean first = true
        Throwable e = t
        while (e != null) {
            if (e.getClass().name.contains('NestedServletException')) {
                e = e.cause
                continue
            }

            StackTraceElement[] stackTrace = e.stackTrace
            int last = stackTrace.size()
            String prevFn
            String prevLn
            boolean evenRow = false
            if (!first) {
                printCausedByMessage(sb, e)
            }
            if (e instanceof MultipleCompilationErrorsException) {
                break
            }
            if (last > 0) {
                stackTrace[0..-1].eachWithIndex { StackTraceElement te, int idx ->
                    String fileName = getFileName(te)
                    String lineNumber
                    if (e instanceof SourceCodeAware) {
                        if (e.lineNumber && e.lineNumber > -1) {
                            lineNumber = e.lineNumber.toString().padLeft(lineNumWidth)
                        }
                        else {
                            lineNumber = te.lineNumber.toString().padLeft(lineNumWidth)
                        }
                        if (e.fileName) {
                            fileName = e.fileName
                            fileName = makeRelativeIfPossible(fileName)
                        }
                    }
                    else {
                        lineNumber = te.lineNumber.toString().padLeft(lineNumWidth)
                    }

                    if (prevLn == lineNumber && idx != last) {
                        return
                    } // no point duplicating lines
                    if ((idx == 0) || fileName) {
                        prevLn = lineNumber
                        if (prevFn && (prevFn == fileName)) {
                            fileName = "    ''"
                        }
                        else {
                            prevFn = fileName
                        }

                        fileName = fileName ?: te.className

                        String padChar = (evenRow || idx == 0) ? ' ' : ' .'
                        evenRow = !evenRow

                        String methodName = te.methodName
                        if (methodName.size() < methodNameBaseWidth) {
                            methodName = methodName.padRight(methodNameBaseWidth - 1, padChar)
                        }

                        if (idx == 0) {
                            printFailureLocation(sb, lineNumber, methodName, fileName)
                        }
                        else if (idx < last - 1) {
                            printStackLine(sb, lineNumber, methodName, fileName)
                        }
                        else {
                            printLastEntry(sb, lineNumber, methodName, fileName)
                        }
                    }
                }
            }

            first = false
            if (shouldSkipNextCause(e)) {
                break
            }
            e = e.cause
        }

        sw.toString()
    }

    static String makeRelativeIfPossible(String fileName) {
        String base = System.getProperty('base.dir')
        if (base) {
            fileName = fileName - base
        }
        fileName
    }

    protected boolean shouldSkipNextCause(Throwable e) {
        e.cause == null || e == e.cause
    }

    protected void printCausedByMessage(PrintWriter sb, Throwable e) {
        sb.println()
        sb.println "Caused by ${e.class.simpleName}: ${e.message}"
    }

    protected void printHeader(PrintWriter sb, String header) {
        sb.println header
    }

    protected void printLastEntry(PrintWriter sb, String lineNumber, String methodName, String fileName) {
        sb.println "^   $lineNumber | $methodName in $fileName"
    }

    protected void printStackLine(PrintWriter sb, String lineNumber, String methodName, String fileName) {
        sb.println "|   $lineNumber | $methodName in $fileName"
    }

    protected void printFailureLocation(PrintWriter sb, String lineNumber, String methodName, String fileName) {
        sb.println "->> $lineNumber | $methodName in $fileName"
        sb << '- ' * 36
        sb.println()
    }

    protected String getFileName(StackTraceElement te) {
        te.className
    }

}
