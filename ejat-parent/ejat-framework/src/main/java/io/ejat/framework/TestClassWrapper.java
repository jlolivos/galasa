package io.ejat.framework;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.ejat.framework.GenericMethodWrapper.Type;
import io.ejat.framework.spi.FrameworkException;
import io.ejat.framework.spi.Result;
import io.ejat.framework.spi.teststructure.TestMethod;
import io.ejat.framework.spi.teststructure.TestStructure;
/**
 * Representation of the test class
 *
 */
public class TestClassWrapper {

	private Log logger = LogFactory.getLog(TestClassWrapper.class);

	private final String testBundle;
	private final Class<?> testClass;
	private Object testClassObject;

	private Result result;

	private ArrayList<GenericMethodWrapper> beforeClassMethods = new ArrayList<>();
	private ArrayList<TestMethodWrapper>    testMethods = new ArrayList<>();
	private ArrayList<GenericMethodWrapper> afterClassMethods = new ArrayList<>();

	// Logger statics
	public static final String LOG_STARTING = "Starting";
	public static final String LOG_ENDING = "Ending";	
	public	static final String LOG_START_LINE = "\n" + StringUtils.repeat("-", 23) + " ";
	public	static final String LOG_ASTERS = StringUtils.repeat("*", 100);

	private final TestStructure testStructure;

	@SuppressWarnings("unused")
	private final TestRunner testRunner;

	/**
	 * Constructor
	 * @param testStructure 
	 * @param testRunner 
	 * @param testBundleName 
	 */
	public TestClassWrapper(TestRunner testRunner, String testBundle, Class<?> testClass, TestStructure testStructure) {
		this.testRunner = testRunner;
		this.testBundle = testBundle;
		this.testClass  = testClass;
		this.testStructure = testStructure;
	}


	/**
	 * Process the test class looking for test methods and fields that need to be injected
	 * 
	 * @throws TestRunException
	 */
	public void parseTestClass() throws TestRunException {

		ArrayList<GenericMethodWrapper> temporaryBeforeMethods = new ArrayList<>();
		ArrayList<GenericMethodWrapper> temporaryAfterMethods = new ArrayList<>();
		ArrayList<Method> temporaryTestMethods = new ArrayList<>();


		try	{
			// Create a list of test classes and it's super classes
			LinkedList<Class<?>> classListList = new LinkedList<>();
			classListList.add(testClass);
			Class<?> superClass = testClass.getSuperclass();
			while (!superClass.isAssignableFrom(Object.class)) {
				classListList.add(superClass);
				superClass = superClass.getSuperclass();
			}

			Iterator<Class<?>> lit = classListList.descendingIterator();
			while(lit.hasNext()){
				parseMethods(lit.next(), temporaryBeforeMethods, temporaryTestMethods, temporaryAfterMethods);
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new TestRunException("Unable to process test class for methods", e);
		}


		//*** Build the wrappers for the test methods
		for(Method method : temporaryTestMethods) {
			this.testMethods.add(new TestMethodWrapper(method, this.testClass, temporaryBeforeMethods, temporaryBeforeMethods));
		}
		
		//*** Create the reporting Test Structure
		
		this.testStructure.setBundle(testBundle);
		this.testStructure.setTestName(testClass.getName());
		this.testStructure.setTestShortName(testClass.getSimpleName());
		ArrayList<TestMethod> structureMethods = new ArrayList<>();
		this.testStructure.setMethods(structureMethods);
		
		for(GenericMethodWrapper before : this.beforeClassMethods) {
			structureMethods.add(before.getStructure());
		}
		
		for(TestMethodWrapper testMethod : this.testMethods) {
			structureMethods.add(testMethod.getStructure());
		}
		
		for(GenericMethodWrapper after : this.afterClassMethods) {
			structureMethods.add(after.getStructure());
		}
		
		
		String report = this.testStructure.report(LOG_START_LINE);
		logger.trace("Test Class structure:-" + report);
	}


	/**
	 * Instantiate test class and set field values
	 *  
	 * @throws TestRunException
	 */
	public void instantiateTestClass() throws TestRunException {
		try {
			testClassObject = testClass.newInstance();
		} catch (InstantiationException | IllegalAccessException | NullPointerException e) {
			throw new TestRunException("Unable to instantiate test class", e);
		}
	}


	/**
	 * Run the test methods in declared order together with @BeforeClass, @Before, @After and @AfterClass
	 * @param managers 
	 * 
	 * @throws TestRunException 
	 */
	public void runTestMethods(@NotNull TestRunManagers managers) throws TestRunException {

		logger.info(LOG_STARTING + 
				LOG_START_LINE + LOG_ASTERS + 
				LOG_START_LINE + "*** Start of test class " + testClass.getName() +
				LOG_START_LINE + LOG_ASTERS);

		try {
			managers.startOfTestClass();
		} catch (FrameworkException e) {
			throw new TestRunException("Unable to inform managers of start of test class", e);
		}

		// Run @BeforeClass methods
		for (GenericMethodWrapper beforeClassMethod : beforeClassMethods) {
			beforeClassMethod.invoke(managers, this.testClassObject);
			if (beforeClassMethod.fullStop()) {
				this.result = Result.failed("BeforeClass method failed");
				break;
			}
		}

		if (result == null) {
			// Run test methods
			for (TestMethodWrapper testMethod : this.testMethods) {
				// Run @Test method			
				testMethod.invoke(managers, this.testClassObject);
				if (testMethod.fullStop()) {
					break;
				}
			}
			
			for (TestMethodWrapper testMethod : this.testMethods) {
				Result testMethodResult = testMethod.getResult();
				if (testMethodResult.isFailed()) {
					this.result = Result.failed("A Test failed");
					break;
				}
			}
			
			if (this.result == null) {
				this.result = Result.passed();
			}
		}

		// Run @AfterClass methods
		for (GenericMethodWrapper afterClassMethod : afterClassMethods) {
			afterClassMethod.invoke(managers, this.testClassObject);
			if (!afterClassMethod.fullStop()) {
				if (this.result == null) {
					this.result = Result.failed("AfterClass method failed");
					break;
				}
			}
		}

		try {
			Result newResult = managers.endOfTestClass(this.result, null); // TODO pass the class level exception
			if (newResult != null) {
				logger.info("Result of test run overridden to " + newResult);
				this.result = newResult;
			}
		} catch (FrameworkException e) {
			throw new TestRunException("Problem with end of test class", e);
		}

		// Test result
		logger.info(LOG_ENDING + 
					LOG_START_LINE + LOG_ASTERS + 
					LOG_START_LINE + "*** " + this.result.getName() + " - Test class " + testClass.getName() +
					LOG_START_LINE + LOG_ASTERS);
		
		this.testStructure.setStatus(this.result.getName());
		
		String report = this.testStructure.report(LOG_START_LINE);
		logger.trace("Finishing Test Class structure:-" + report);


		return;
	}


	/**
	 * Parse test class for test methods
	 * @param temporaryAfterMethods 
	 * @param temporaryTestMethods 
	 * @param temporaryBeforeMethods 
	 * 
	 * @param bcelJavaClass
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws TestRunException
	 */
	private void parseMethods(Class<?> testClassXXX, 
			List<GenericMethodWrapper> temporaryBeforeMethods, 
			List<Method> temporaryTestMethods, 
			List<GenericMethodWrapper> temporaryAfterMethods) throws NoSuchMethodException, TestRunException {  	   	
		org.apache.bcel.classfile.JavaClass bcelJavaClass;
		try {
			bcelJavaClass = org.apache.bcel.Repository.lookupClass(testClassXXX);
		} catch (ClassNotFoundException e) {
			throw new TestRunException(e);
		}
		org.apache.bcel.classfile.Method[] bcelMethods = bcelJavaClass.getMethods();
		for (org.apache.bcel.classfile.Method bcelMethod : bcelMethods) {
			if (bcelMethod.isPublic() && !bcelMethod.getName().equals("<init>")) {
				Method method = testClassXXX.getMethod(bcelMethod.getName());
				Annotation[] annotations = method.getAnnotations();
				for (Annotation annotation : annotations) {
					storeMethod(method, annotation.annotationType(), temporaryBeforeMethods, temporaryTestMethods, temporaryAfterMethods);
				}	
			}
		}
	}


	/** 
	 * Store the test methods
	 * 
	 * @param method
	 * @param annotationType
	 */
	private void storeMethod(Method method, 
			Class<? extends Annotation> annotationType,
			List<GenericMethodWrapper> temporaryBeforeMethods, 
			List<Method> temporaryTestMethods, 
			List<GenericMethodWrapper> temporaryAfterMethods) {
		if (annotationType == io.ejat.BeforeClass.class) {
			beforeClassMethods.add(new GenericMethodWrapper(method, this.testClass, Type.BeforeClass));
		}
		if (annotationType == io.ejat.AfterClass.class) {
			afterClassMethods.add(new GenericMethodWrapper(method, this.testClass, Type.AfterClass));
		}
		if (annotationType == io.ejat.Before.class) {
			temporaryBeforeMethods.add(new GenericMethodWrapper(method, this.testClass, Type.Before));
		}
		if (annotationType == io.ejat.After.class) {
			temporaryAfterMethods.add(new GenericMethodWrapper(method, this.testClass, Type.After));
		}
		if (annotationType == io.ejat.Test.class) {
			temporaryTestMethods.add(method);
		}		
	}
	
}
