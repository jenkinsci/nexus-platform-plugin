/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.ci.quality

import jnr.posix.util.MethodName
import org.gmetrics.metric.abc.AbcMetric
import org.gmetrics.metric.crap.CrapMetric

ruleset {

  description '''
        A Sample Groovy RuleSet containing all CodeNarc Rules, grouped by category.
        You can use this as a template for your own custom RuleSet.
        Just delete the rules that you don't want to include.
        '''

  // rulesets/basic.xml
  AssertWithinFinallyBlock
  AssignmentInConditional
  BigDecimalInstantiation
  BitwiseOperatorInConditional
  BooleanGetBoolean
  BrokenNullCheck
  BrokenOddnessCheck
  ClassForName
  ComparisonOfTwoConstants
  ComparisonWithSelf
  ConstantAssertExpression
  ConstantIfExpression
  ConstantTernaryExpression
  DeadCode
  DoubleNegative
  DuplicateCaseStatement
  DuplicateMapKey
  DuplicateSetValue
  EmptyCatchBlock
  EmptyClass
  EmptyElseBlock
  EmptyFinallyBlock
  EmptyForStatement
  EmptyIfStatement
  EmptyInstanceInitializer
  EmptyMethod
  EmptyStaticInitializer
  EmptySwitchStatement
  EmptySynchronizedStatement
  EmptyTryBlock
  EmptyWhileStatement
  EqualsAndHashCode
  EqualsOverloaded
  ExplicitGarbageCollection
  ForLoopShouldBeWhileLoop
  HardCodedWindowsFileSeparator
  HardCodedWindowsRootDirectory
  IntegerGetInteger
  MultipleUnaryOperators
  RandomDoubleCoercedToZero
  RemoveAllOnSelf
  ReturnFromFinallyBlock
  ThrowExceptionFromFinallyBlock

  // rulesets/braces.xml
  ElseBlockBraces
  ForStatementBraces
  IfStatementBraces
  WhileStatementBraces

  // rulesets/concurrency.xml
  BusyWait
  DoubleCheckedLocking
  InconsistentPropertyLocking
  InconsistentPropertySynchronization
  NestedSynchronization
  StaticCalendarField
  StaticConnection
  StaticDateFormatField
  StaticMatcherField
  StaticSimpleDateFormatField
  SynchronizedMethod
  SynchronizedOnBoxedPrimitive
  SynchronizedOnGetClass
  SynchronizedOnReentrantLock
  SynchronizedOnString
  SynchronizedOnThis
  SynchronizedReadObjectMethod
  SystemRunFinalizersOnExit
  ThisReferenceEscapesConstructor
  ThreadGroup
  ThreadLocalNotStaticFinal
  ThreadYield
  UseOfNotifyMethod
  VolatileArrayField
  VolatileLongOrDoubleField
  WaitOutsideOfWhileLoop

  // rulesets/convention.xml
  ConfusingTernary
  CouldBeElvis
  HashtableIsObsolete
  IfStatementCouldBeTernary
  InvertedIfElse
  LongLiteralWithLowerCaseL
  // NoDef
  NoTabCharacter
  ParameterReassignment
  TernaryCouldBeElvis
  // TrailingComma Developer can choose to use trailing comma or not
  VectorIsObsolete

  // rulesets/design.xml
  AbstractClassWithPublicConstructor
  AbstractClassWithoutAbstractMethod
  AssignmentToStaticFieldFromInstanceMethod
  BooleanMethodReturnsNull
  BuilderMethodWithSideEffects
  CloneableWithoutClone
  CloseWithoutCloseable
  CompareToWithoutComparable
  ConstantsOnlyInterface
  EmptyMethodInAbstractClass
  FinalClassWithProtectedMember
  ImplementationAsType
  Instanceof
  LocaleSetDefault
  NestedForLoop
  PrivateFieldCouldBeFinal
  PublicInstanceField
  ReturnsNullInsteadOfEmptyArray
  ReturnsNullInsteadOfEmptyCollection
  SimpleDateFormatMissingLocale
  StatelessSingleton
  ToStringReturnsNull

  // rulesets/dry.xml
  DuplicateListLiteral
  DuplicateMapLiteral
  DuplicateNumberLiteral
  DuplicateStringLiteral

  // rulesets/enhanced.xml
  CloneWithoutCloneable
  JUnitAssertEqualsConstantActualValue
  UnsafeImplementationAsMap

  // rulesets/exceptions.xml
  CatchArrayIndexOutOfBoundsException
  CatchError
  CatchException
  CatchIllegalMonitorStateException
  CatchIndexOutOfBoundsException
  CatchNullPointerException
  CatchRuntimeException
  CatchThrowable
  ConfusingClassNamedException
  ExceptionExtendsError
  ExceptionExtendsThrowable
  ExceptionNotThrown
  MissingNewInThrowStatement
  ReturnNullFromCatchBlock
  SwallowThreadDeath
  ThrowError
  ThrowException
  ThrowNullPointerException
  ThrowRuntimeException
  ThrowThrowable

  // rulesets/formatting.xml
  // BlankLineBeforePackage // mainly because intellij inserts a blank line by default between copyright and package
  BracesForClass { sameLine=false } // braces for classes are on the next line
  BracesForForLoop
  BracesForIfElse
  //BracesForMethod // same line and next line acceptable
  BracesForTryCatchFinally
  //ClassJavadoc
  ClosureStatementOnOpeningLineOfMultipleLineClosure
  ConsecutiveBlankLines
  FileEndsWithoutNewline
  LineLength
  MissingBlankLineAfterImports
  MissingBlankLineAfterPackage
  SpaceAfterCatch
  SpaceAfterClosingBrace
  SpaceAfterComma
  SpaceAfterFor
  SpaceAfterIf
  SpaceAfterOpeningBrace
  SpaceAfterSemicolon
  SpaceAfterSwitch
  SpaceAfterWhile
  SpaceAroundClosureArrow
  // SpaceAroundMapEntryColon // does not fit our style guide
  SpaceAroundOperator
  SpaceBeforeClosingBrace
  SpaceBeforeOpeningBrace
  TrailingWhitespace

  // rulesets/generic.xml
  IllegalClassMember
  IllegalClassReference
  IllegalPackageReference
  IllegalRegex
  IllegalString
  IllegalSubclass
  RequiredRegex
  RequiredString
  StatelessClass

  // rulesets/grails.xml
  GrailsDomainHasEquals
  GrailsDomainHasToString
  GrailsDomainReservedSqlKeywordName
  GrailsDomainWithServiceReference
  GrailsDuplicateConstraint
  GrailsDuplicateMapping
  GrailsMassAssignment
  GrailsPublicControllerMethod
  GrailsServletContextReference
  GrailsSessionReference   // DEPRECATED
  GrailsStatelessService

  // rulesets/groovyism.xml
  AssignCollectionSort
  AssignCollectionUnique
  ClosureAsLastMethodParameter
  CollectAllIsDeprecated
  ConfusingMultipleReturns
  ExplicitArrayListInstantiation
  ExplicitCallToAndMethod
  ExplicitCallToCompareToMethod
  ExplicitCallToDivMethod
  ExplicitCallToEqualsMethod
  ExplicitCallToGetAtMethod
  ExplicitCallToLeftShiftMethod
  ExplicitCallToMinusMethod
  ExplicitCallToModMethod
  ExplicitCallToMultiplyMethod
  ExplicitCallToOrMethod
  ExplicitCallToPlusMethod
  ExplicitCallToPowerMethod
  ExplicitCallToRightShiftMethod
  ExplicitCallToXorMethod
  ExplicitHashMapInstantiation
  ExplicitHashSetInstantiation
  ExplicitLinkedHashMapInstantiation
  ExplicitLinkedListInstantiation
  ExplicitStackInstantiation
  ExplicitTreeSetInstantiation
  GStringAsMapKey
  GStringExpressionWithinString
  // GetterMethodCouldBeProperty  // TODO clarify which convention we prefer. see also UnnecessaryGetter
  GroovyLangImmutable
  UseCollectMany
  UseCollectNested

  // rulesets/imports.xml
  DuplicateImport
  ImportFromSamePackage
  ImportFromSunPackages
  MisorderedStaticImports { comesBefore=false }
  NoWildcardImports
  UnnecessaryGroovyImport
  UnusedImport

  // rulesets/jdbc.xml
  DirectConnectionManagement
  JdbcConnectionReference
  JdbcResultSetReference
  JdbcStatementReference

  // rulesets/junit.xml
  ChainedTest
  CoupledTestCase
  JUnitAssertAlwaysFails
  JUnitAssertAlwaysSucceeds
  JUnitFailWithoutMessage
  JUnitLostTest
  JUnitPublicField
  JUnitPublicNonTestMethod
  JUnitPublicProperty
  JUnitSetUpCallsSuper
  JUnitStyleAssertions
  JUnitTearDownCallsSuper
  JUnitTestMethodWithoutAssert
  JUnitUnnecessarySetUp
  JUnitUnnecessaryTearDown
  JUnitUnnecessaryThrowsException
  SpockIgnoreRestUsed
  UnnecessaryFail
  UseAssertEqualsInsteadOfAssertTrue
  UseAssertFalseInsteadOfNegation
  UseAssertNullInsteadOfAssertEquals
  UseAssertSameInsteadOfAssertTrue
  UseAssertTrueInsteadOfAssertEquals
  UseAssertTrueInsteadOfNegation

  // rulesets/logging.xml
  LoggerForDifferentClass
  LoggerWithWrongModifiers
  LoggingSwallowsStacktrace
  MultipleLoggers
  PrintStackTrace
  Println
  SystemErrPrint
  SystemOutPrint

  // rulesets/naming.xml
  AbstractClassName
  ClassName
  ClassNameSameAsFilename
  ClassNameSameAsSuperclass
  ConfusingMethodName
  FactoryMethodName
  FieldName
  InterfaceName
  InterfaceNameSameAsSuperInterface
  MethodName
  ObjectOverrideMisspelledMethodName
  PackageName
  PackageNameMatchesFilePath
  ParameterName
  PropertyName
  VariableName { finalRegex = /[a-z][a-zA-Z0-9]*/ }

  // rulesets/security.xml
  // FileCreateTempFile  // disable because alternatives like OWASP's ESAPI 3.0 seems to have died
  InsecureRandom
  // JavaIoPackageAccess // we want to be able to access files
  NonFinalPublicField
  NonFinalSubclassOfSensitiveInterface
  ObjectFinalize
  PublicFinalizeMethod
  SystemExit
  UnsafeArrayDeclaration

  // rulesets/serialization.xml
  EnumCustomSerializationIgnored
  SerialPersistentFields
  SerialVersionUID
  // SerializableClassMustDefineSerialVersionUID  // up to developer to decide

  // rulesets/size.xml
  // AbcComplexity   // DEPRECATED: Use the AbcMetric rule instead. Requires the GMetrics jar
  AbcMetric { maxMethodAbcScore = 30 ;  maxClassAverageMethodAbcScore = 15 }  // Requires the GMetrics jar
  ClassSize { maxLines = 200 }
  CrapMetric   // Requires the GMetrics jar and a Cobertura coverage file
  CyclomaticComplexity   // Requires the GMetrics jar
  MethodCount { maxMethods = 15 }
  MethodSize { maxLines = 50 }
  NestedBlockDepth
  ParameterCount

  // rulesets/unnecessary.xml
  AddEmptyString
  ConsecutiveLiteralAppends
  ConsecutiveStringConcatenation
  UnnecessaryBigDecimalInstantiation
  UnnecessaryBigIntegerInstantiation
  UnnecessaryBooleanExpression
  UnnecessaryBooleanInstantiation
  UnnecessaryCallForLastElement
  UnnecessaryCallToSubstring
  UnnecessaryCast
  UnnecessaryCatchBlock
  // UnnecessaryCollectCall collect can make code more readable. up to developer to choose what is best for readability
  UnnecessaryCollectionCall
  UnnecessaryConstructor
  UnnecessaryDefInFieldDeclaration
  UnnecessaryDefInMethodDeclaration
  UnnecessaryDefInVariableDeclaration
  UnnecessaryDotClass
  UnnecessaryDoubleInstantiation
  // UnnecessaryElseStatement  // up to developer to choose what is best for readability
  UnnecessaryFinalOnPrivateMethod
  UnnecessaryFloatInstantiation
  UnnecessaryGString
  // UnnecessaryGetter  // TODO clarify which convention we prefer. see also GetterMethodCouldBeProperty
  UnnecessaryIfStatement
  UnnecessaryInstanceOfCheck
  UnnecessaryInstantiationToGetClass
  UnnecessaryIntegerInstantiation
  UnnecessaryLongInstantiation
  UnnecessaryModOne
  UnnecessaryNullCheck
  UnnecessaryNullCheckBeforeInstanceOf
  UnnecessaryObjectReferences
  UnnecessaryOverridingMethod
  UnnecessaryPackageReference
  UnnecessaryParenthesesForMethodCallWithClosure
  UnnecessaryPublicModifier
  // UnnecessaryReturnKeyword  // return is optional. up to the developer to choose what is best for readability
  UnnecessarySafeNavigationOperator
  UnnecessarySelfAssignment
  UnnecessarySemicolon
  UnnecessaryStringInstantiation
  UnnecessarySubstring
  UnnecessaryTernaryExpression
  UnnecessaryToString
  UnnecessaryTransientModifier

  // rulesets/unused.xml
  UnusedArray
  UnusedMethodParameter
  UnusedObject
  UnusedPrivateField
  UnusedPrivateMethod
  UnusedPrivateMethodParameter
  UnusedVariable


}
